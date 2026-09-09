package com.sleepwell.sleepwell_backend.service.voice;

import com.sleepwell.sleepwell_backend.dto.voice.TextToSpeechRequestDto;
import com.sleepwell.sleepwell_backend.dto.voice.TextToSpeechResponseDto;

/**
 * 텍스트-음성 변환 서비스 인터페이스
 * 다양한 TTS 프로바이더 (OpenAI TTS, Google Cloud Text-to-Speech 등)를 추상화
 */
public interface TextToSpeechService {

    /**
     * 텍스트를 음성으로 변환
     * 
     * @param text 변환할 텍스트
     * @param voice 음성 타입 (alloy, echo, fable 등)
     * @param language 언어 코드 (ko, en 등)
     * @return 생성된 음성 파일 정보
     */
    TextToSpeechResponseDto synthesizeSpeech(String text, String voice, String language);

    /**
     * 상세 설정이 포함된 텍스트-음성 변환
     * 
     * @param request TTS 변환 요청 정보
     * @return 생성된 음성 파일 정보
     */
    TextToSpeechResponseDto synthesizeWithOptions(TextToSpeechRequestDto request);

    /**
     * 스트리밍 TTS (실시간 음성 생성)
     * 
     * @param text 변환할 텍스트
     * @param voice 음성 타입
     * @param language 언어 코드
     * @return 스트리밍 응답 정보
     */
    default TextToSpeechResponseDto synthesizeStreaming(String text, String voice, String language) {
        // 스트리밍을 일반 합성으로 fallback
        // 실제 스트리밍 구현시에는 청크 단위 음성 생성으로 대체
        return synthesizeSpeech(text, voice, language);
    }

    /**
     * 사용 가능한 음성 목록 조회
     * 
     * @param language 언어 코드 (선택사항)
     * @return 음성 목록
     */
    String[] getAvailableVoices(String language);

    /**
     * 지원하는 언어 목록 조회
     * 
     * @return 언어 코드 목록
     */
    String[] getSupportedLanguages();

    /**
     * 서비스 가용성 확인
     * 
     * @return 서비스 사용 가능 여부
     */
    boolean isAvailable();

    /**
     * 프로바이더 이름 반환
     * 
     * @return 프로바이더 이름 (예: "openai", "google")
     */
    String getProviderName();

    /**
     * 텍스트 길이 제한 확인
     * 
     * @param text 확인할 텍스트
     * @return 제한 내 여부
     */
    boolean isWithinTextLimit(String text);
}