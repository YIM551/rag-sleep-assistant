package com.sleepwell.sleepwell_backend.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 텍스트-음성 변환 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextToSpeechResponseDto {

    /**
     * 생성된 오디오 파일 URL 또는 경로
     */
    private String audioUrl;

    /**
     * 오디오 데이터 (Base64 인코딩된 바이너리 데이터)
     */
    private String audioData;

    /**
     * 오디오 파일 형식 (mp3, wav 등)
     */
    private String audioFormat;

    /**
     * 오디오 파일 크기 (바이트)
     */
    private Long audioSizeBytes;

    /**
     * 오디오 길이 (초)
     */
    private Double audioDurationSeconds;

    /**
     * 처리 시간 (밀리초)
     */
    private Long processingTimeMs;

    /**
     * 사용된 모델
     */
    private String model;

    /**
     * 사용된 음성
     */
    private String voice;

    /**
     * 언어 코드
     */
    private String language;

    /**
     * 프로바이더 이름
     */
    private String provider;

    /**
     * 텍스트 길이 (문자 수)
     */
    private Integer textLength;

    /**
     * 생성 완료 시간
     */
    private LocalDateTime timestamp;

    /**
     * 요청 ID
     */
    private String requestId;

    /**
     * 성공 여부
     */
    @Builder.Default
    private Boolean success = true;

    /**
     * 에러 메시지 (실패 시)
     */
    private String errorMessage;

    /**
     * 메타데이터
     */
    private AudioMetadata metadata;

    /**
     * 스트리밍 정보 (스트리밍 TTS인 경우)
     */
    private StreamingInfo streamingInfo;

    /**
     * 오디오 메타데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioMetadata {
        private Integer sampleRate;
        private Integer bitRate;
        private Integer channels;
        private String encoding;
        private String checksum;
        private Double speed;
        private Double pitch;
        private String quality;
    }

    /**
     * 스트리밍 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreamingInfo {
        private String streamId;
        private String streamUrl;
        private Boolean isStreaming;
        private Integer chunkCount;
        private Long totalStreamSizeBytes;
    }
}