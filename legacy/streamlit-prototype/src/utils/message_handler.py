import streamlit as st
from datetime import datetime
# 필요시 tts_handler 임포트
from src.utils import audio_handler as tts_handler

def handle_message_submission(prompt: str) -> None:
    """사용자 메시지를 처리하고 챗봇의 응답을 생성하는 함수 (수면상담 RAG + 페르소나 + CBTI + TTS)"""
    if not prompt or not prompt.strip():
        return
    
    # 중복 체크를 위한 키 생성
    message_key = f"{prompt}_{datetime.now().strftime('%Y%m%d%H%M%S')}"
    if 'processed_messages' not in st.session_state:
        st.session_state.processed_messages = set()
    if message_key in st.session_state.processed_messages:
        return

    try:
        chatbot = st.session_state.get('chatbot_service')
        if chatbot:
            # === [확장] 페르소나/CBTI 주차/TTS 상태 읽기 ===
            persona = st.session_state.get('selected_persona', '전문가 상담가')
            cbti_week = st.session_state.get('cbti_week', 1)
            tts_enabled = st.session_state.get('tts_enabled', False)

            # === [확장] 챗봇 응답 생성 (페르소나/주차 인자 추가) ===
            # chatbot.get_response()가 인자 받는 구조라면 이렇게:
            response, reference_docs = chatbot.get_response(
                prompt, persona=persona, cbti_week=cbti_week
            )
            # 만약 기존처럼 prompt만 받으면 기존 방식 유지

            # 디버그 출력
            print("\n=== Saving Message ===")
            print(f"Response: {response}")
            print(f"Reference docs: {reference_docs}")

            # 감정 분석 및 응답 생성 (기존 그대로 유지)
            emotions = chatbot.analyze_emotion(prompt)
            dominant_emotion = max(emotions.items(), key=lambda x: x[1])[0]

            # 메시지 목록 초기화
            if 'messages' not in st.session_state:
                st.session_state.messages = []

            # 새 메시지 추가
            current_time = datetime.now().strftime('%p %I:%M')
            st.session_state.messages.extend([
                {
                    "role": "user",
                    "content": prompt,
                    "timestamp": current_time,
                    "emotion": dominant_emotion
                },
                {
                    "role": "assistant",
                    "content": response,
                    "timestamp": current_time,
                    "reference_docs": reference_docs
                }
            ])

            # TTS(음성 출력) === [확장] ===
            if tts_enabled:
                try:
                    # 답변을 mp3로 변환
                    audio_path = tts_handler.synthesize_speech(response, lang='ko')
                    with open(audio_path, 'rb') as audio_file:
                        audio_bytes = audio_file.read()
                    st.audio(audio_bytes, format="audio/mp3")
                except Exception as tts_err:
                    st.warning(f"TTS 변환에 실패했습니다: {tts_err}")

            # 처리된 메시지 기록
            st.session_state.processed_messages.add(message_key)
            st.session_state.current_emotion = dominant_emotion

            # 대화 통계 업데이트 (기존)
            if 'conversation_stats' not in st.session_state:
                st.session_state.conversation_stats = {'total': 0, 'positive': 0, 'negative': 0}
            st.session_state.conversation_stats['total'] += 1
            if dominant_emotion in ['joy', 'love', 'surprise']:
                st.session_state.conversation_stats['positive'] += 1
            elif dominant_emotion in ['anger', 'sadness', 'fear']:
                st.session_state.conversation_stats['negative'] += 1

        else:
            st.error("챗봇 서비스가 초기화되지 않았습니다.")

    except Exception as e:
        st.error(f"메시지 처리 중 오류가 발생했습니다: {str(e)}")
