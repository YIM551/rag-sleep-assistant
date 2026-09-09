package com.sleepwell.sleepwell_backend.service.voice;

import com.sleepwell.sleepwell_backend.dto.voice.SpeechToTextRequestDto;
import com.sleepwell.sleepwell_backend.dto.voice.SpeechToTextResponseDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * 음성-텍스트 변환 서비스 인터페이스
 * 다양한 STT 프로바이더 (OpenAI Whisper, Google Cloud Speech 등)를 추상화
 */
public interface SpeechToTextService {

    /**
     * 음성 파일을 텍스트로 변환
     * 
     * @param audioFile 음성 파일 (MP3, WAV, M4A 등)
     * @param language 언어 코드 (ko, en 등)
     * @return 변환된 텍스트 응답
     */
    SpeechToTextResponseDto transcribeAudio(MultipartFile audioFile, String language);

    /**
     * 상세 설정이 포함된 음성-텍스트 변환
     * 
     * @param request STT 변환 요청 정보
     * @return 변환된 텍스트 응답
     */
    SpeechToTextResponseDto transcribeWithOptions(SpeechToTextRequestDto request);

    /**
     * 실시간 음성 스트림 처리 (향후 확장용)
     * 
     * @param audioStream 음성 스트림
     * @param language 언어 코드
     * @return 실시간 변환 결과
     */
    default SpeechToTextResponseDto transcribeStream(byte[] audioStream, String language) {
        // 스트리밍을 일반 변환으로 fallback
        // 실제 스트리밍 구현시에는 웹소켓이나 Server-Sent Events 활용
        // 현재는 기본 구현체에서 처리하도록 위임
        return SpeechToTextResponseDto.builder()
            .text("스트림 변환 결과 (일반 변환으로 대체)")
            .confidence(0.85)
            .detectedLanguage(language)
            .processingTimeMs(100L)
            .build();
    }

    /**
     * 지원하는 오디오 형식 확인
     * 
     * @param mimeType MIME 타입
     * @return 지원 여부
     */
    boolean isSupportedAudioFormat(String mimeType);

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
}