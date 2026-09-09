import os
import torch
import torchaudio
import pyaudio
import wave
import threading
import time
from transformers import AutoModelForAudioClassification, AutoProcessor
import speech_recognition as sr
import whisper
import streamlit as st
from pydub import AudioSegment
import uuid
from gtts import gTTS
from io import BytesIO
from datetime import datetime

# ==== [ STT/감정 분석 영역 ] ====

MODEL_NAME = "forwarder1121/ast-finetuned-model"
processor = AutoProcessor.from_pretrained(MODEL_NAME)
model = AutoModelForAudioClassification.from_pretrained(MODEL_NAME)

class AudioRecorder:
    def __init__(self):
        self.frames = []
        self.stream = None
        self.p = None
        self._recording = False
        
    @property
    def is_recording(self):
        return self._recording
        
    @is_recording.setter
    def is_recording(self, value):
        self._recording = value
        
    def start_recording(self):
        try:
            self.frames = []
            self._recording = True
            
            CHUNK = 1024
            FORMAT = pyaudio.paInt16
            CHANNELS = 1
            RATE = 16000
            
            self.p = pyaudio.PyAudio()
            info = self.p.get_host_api_info_by_index(0)
            numdevices = info.get('deviceCount')
            input_device = None
            
            for i in range(numdevices):
                device_info = self.p.get_device_info_by_host_api_device_index(0, i)
                if device_info.get('maxInputChannels') > 0:
                    input_device = i
                    break
            
            if input_device is None:
                print("[ERROR] 사용 가능한 입력 장치를 찾을 수 없습니다.")
                return
            
            self.stream = self.p.open(
                format=FORMAT,
                channels=CHANNELS,
                rate=RATE,
                input=True,
                input_device_index=input_device,
                frames_per_buffer=CHUNK
            )
            
            def record():
                while self.is_recording:
                    try:
                        data = self.stream.read(CHUNK, exception_on_overflow=False)
                        self.frames.append(data)
                    except Exception as e:
                        print(f"[ERROR] 녹음 중 오류 발생: {str(e)}")
                        break
            
            self.record_thread = threading.Thread(target=record)
            self.record_thread.start()
            
        except Exception as e:
            print(f"[ERROR] 녹음 시작 중 오류 발생: {str(e)}")
            self._recording = False
            return None
    
    def stop_recording(self):
        try:
            if not self.is_recording:
                return None
                
            self._recording = False
            if hasattr(self, 'record_thread'):
                self.record_thread.join()
            if self.stream:
                self.stream.stop_stream()
                self.stream.close()
            if self.p:
                self.p.terminate()
            if not self.frames:
                print("[ERROR] 녹음된 프레임이 없습니다.")
                return None
            
            temp_path = "temp_recording.wav"
            with wave.open(temp_path, 'wb') as wf:
                wf.setnchannels(1)
                wf.setsampwidth(self.p.get_sample_size(pyaudio.paInt16))
                wf.setframerate(16000)
                wf.writeframes(b''.join(self.frames))
            return temp_path
            
        except Exception as e:
            print(f"[ERROR] 녹음 중지 중 오류 발생: {str(e)}")
            return None

def predict_audio_emotion(audio_path: str) -> str:
    try:
        waveform, sample_rate = torchaudio.load(audio_path, backend="soundfile")
        if sample_rate != 16000:
            resampler = torchaudio.transforms.Resample(orig_freq=sample_rate, new_freq=16000)
            waveform = resampler(waveform)
        inputs = processor(waveform.squeeze().numpy(), sampling_rate=16000, return_tensors="pt")
        with torch.no_grad():
            outputs = model(**inputs)
            predicted_label = torch.argmax(outputs.logits, dim=1).item()
        emotion_mapping = {
            0: "Anger", 1: "Disgust", 2: "Fear", 3: "Happy", 4: "Neutral", 5: "Sad"
        }
        return emotion_mapping[predicted_label]
    except Exception as e:
        print(f"[ERROR] 감정 예측 중 오류: {str(e)}")
        return "Neutral"

def transcribe_audio(audio_path: str) -> str:
    try:
        recognizer = sr.Recognizer()
        with sr.AudioFile(audio_path) as source:
            audio_data = recognizer.record(source)
            try:
                text = recognizer.recognize_google(audio_data, language='ko-KR')
                return text
            except sr.UnknownValueError:
                pass
            except sr.RequestError:
                pass
        # Whisper fallback
        model_whisper = whisper.load_model("base")
        result = model_whisper.transcribe(audio_path, language='ko')
        return result["text"].strip()
    except Exception as e:
        print(f"[ERROR] 음성 변환 중 오류 발생: {str(e)}")
        return None

def process_recorded_audio(return_audio_file=False):
    try:
        if not hasattr(st.session_state, 'audio_recorder') or not st.session_state.audio_recorder:
            print("[ERROR] AudioRecorder가 초기화되지 않았습니다.")
            return (None, "Neutral", None) if return_audio_file else (None, "Neutral")
        recorder = st.session_state.audio_recorder
        st.session_state.audio_recorder = None
        audio_path = recorder.stop_recording()
        if not audio_path or not os.path.exists(audio_path):
            print("[ERROR] 오디오 파일이 생성되지 않았습니다.")
            return (None, "Neutral", None) if return_audio_file else (None, "Neutral")
        try:
            text = transcribe_audio(audio_path)
            emotion = predict_audio_emotion(audio_path) if text else "Neutral"
            if return_audio_file:
                return text, emotion, audio_path
            else:
                try:
                    os.remove(audio_path)
                except Exception as e:
                    print(f"[WARNING] 임시 파일 삭제 실패: {str(e)}")
                return text, emotion
        except Exception as e:
            print(f"[ERROR] 음성 처리 중 오류 발생: {str(e)}")
            if os.path.exists(audio_path):
                try: os.remove(audio_path)
                except: pass
            return (None, "Neutral", None) if return_audio_file else (None, "Neutral")
    except Exception as e:
        print(f"[ERROR] 전체 처리 중 오류 발생: {str(e)}")
        return (None, "Neutral", None) if return_audio_file else (None, "Neutral")

# ==== [ TTS 영역 ] ====

def synthesize_speech(text, lang='ko'):
    """텍스트를 mp3로 변환하여 BytesIO 객체 반환"""
    tts = gTTS(text=text, lang=lang)
    mp3_fp = BytesIO()
    tts.write_to_fp(mp3_fp)
    mp3_fp.seek(0)
    return mp3_fp

def get_unique_filename():
    """고유 mp3 파일명 반환(필요시 파일 저장용)"""
    session_id = str(st.session_state.get('session_id', uuid.uuid4()))
    timestamp = datetime.now().strftime('%Y%m%d%H%M%S%f')
    return f"tts_{session_id}_{timestamp}.mp3"

def display_bot_message(text, enable_tts=True):
    st.markdown(f"<div class='bot-message'>{text}</div>", unsafe_allow_html=True)
    if enable_tts:
        mp3_fp = synthesize_speech(text, lang='ko')
        st.audio(mp3_fp, format='audio/mp3')

def sidebar_tts_toggle():
    if 'tts_enabled' not in st.session_state:
        st.session_state['tts_enabled'] = True
    st.sidebar.markdown('---')
    st.sidebar.write('🗣️ **음성 출력 (TTS)**')
    st.session_state['tts_enabled'] = st.sidebar.checkbox('답변을 음성으로 들려주기', value=st.session_state['tts_enabled'])

def clean_old_tts_files(tts_dir='./tts_cache', max_age_min=10):
    """지정 폴더의 mp3 파일 중 오래된 파일 삭제 (분 단위)"""
    now = datetime.now().timestamp()
    for fname in os.listdir(tts_dir):
        if fname.endswith('.mp3'):
            fpath = os.path.join(tts_dir, fname)
            mtime = os.path.getmtime(fpath)
            if (now - mtime) > (max_age_min * 60):
                try:
                    os.remove(fpath)
                except Exception:
                    pass

# ===== 사용 예시 =====
# from src.utils.audio_handler import process_recorded_audio, display_bot_message, sidebar_tts_toggle
# text, emotion = process_recorded_audio()
# display_bot_message("챗봇 답변", enable_tts=st.session_state.get('tts_enabled', True))
