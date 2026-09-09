package com.sleepwell.sleepwell_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * 음성 처리 관련 예외 클래스
 * 
 * STT/TTS, 음성 파일 처리 등에서 발생하는 예외를 처리합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public class VoiceProcessingException extends BusinessException {

    public VoiceProcessingException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "VOICE_PROCESSING_ERROR");
    }

    public VoiceProcessingException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }

    public VoiceProcessingException(String message, HttpStatus status, String errorCode) {
        super(message, status, errorCode);
    }

    // STT 처리 실패
    public static VoiceProcessingException sttProcessingFailed(String reason) {
        return new VoiceProcessingException("음성 인식에 실패했습니다: " + reason, "STT_PROCESSING_FAILED");
    }

    // TTS 처리 실패
    public static VoiceProcessingException ttsProcessingFailed(String reason) {
        return new VoiceProcessingException("음성 합성에 실패했습니다: " + reason, "TTS_PROCESSING_FAILED");
    }

    // 지원하지 않는 오디오 형식
    public static VoiceProcessingException unsupportedAudioFormat(String format) {
        return new VoiceProcessingException("지원하지 않는 오디오 형식입니다: " + format, "UNSUPPORTED_AUDIO_FORMAT");
    }

    // 오디오 파일 크기 초과
    public static VoiceProcessingException audioFileTooLarge(long maxSize) {
        return new VoiceProcessingException(
            "오디오 파일 크기가 너무 큽니다. 최대 크기: " + maxSize + " bytes", 
            HttpStatus.PAYLOAD_TOO_LARGE,
            "AUDIO_FILE_TOO_LARGE"
        );
    }

    // 오디오 파일 손상
    public static VoiceProcessingException corruptedAudioFile() {
        return new VoiceProcessingException("오디오 파일이 손상되었습니다", "CORRUPTED_AUDIO_FILE");
    }

    // NAVER CLOVA API 오류
    public static VoiceProcessingException clovaApiError(String error) {
        return new VoiceProcessingException(
            "CLOVA API 호출 실패: " + error, 
            HttpStatus.SERVICE_UNAVAILABLE,
            "CLOVA_API_ERROR"
        );
    }

    // 음성 데이터 저장 실패
    public static VoiceProcessingException voiceDataStorageFailed() {
        return new VoiceProcessingException(
            "음성 데이터 저장에 실패했습니다", 
            HttpStatus.INTERNAL_SERVER_ERROR,
            "VOICE_DATA_STORAGE_FAILED"
        );
    }

    // 음성 처리 시간 초과
    public static VoiceProcessingException processingTimeout() {
        return new VoiceProcessingException(
            "음성 처리 시간이 초과되었습니다", 
            HttpStatus.REQUEST_TIMEOUT,
            "VOICE_PROCESSING_TIMEOUT"
        );
    }
}