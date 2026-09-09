package com.sleepwell.sleepwell_backend.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 음성-텍스트 변환 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechToTextResponseDto {

    /**
     * 변환된 텍스트
     */
    private String text;

    /**
     * 언어 코드 (감지된 언어)
     */
    private String detectedLanguage;

    /**
     * 신뢰도 점수 (0.0 ~ 1.0)
     */
    private Double confidence;

    /**
     * 처리 시간 (밀리초)
     */
    private Long processingTimeMs;

    /**
     * 음성 파일 길이 (초)
     */
    private Double audioDurationSeconds;

    /**
     * 사용된 모델
     */
    private String model;

    /**
     * 프로바이더 이름
     */
    private String provider;

    /**
     * 세그먼트별 결과 (타임스탬프 포함)
     */
    private List<TranscriptionSegment> segments;

    /**
     * 화자 정보 (화자 구분이 활성화된 경우)
     */
    private List<SpeakerInfo> speakers;

    /**
     * 처리 완료 시간
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
    private TranscriptionMetadata metadata;

    /**
     * 변환 세그먼트 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptionSegment {
        private String text;
        private Double startTime;
        private Double endTime;
        private Double confidence;
        private Integer speakerId;
    }

    /**
     * 화자 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpeakerInfo {
        private Integer speakerId;
        private String speakerLabel;
        private Double totalSpeakingTime;
        private Integer segmentCount;
    }

    /**
     * 변환 메타데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptionMetadata {
        private String audioFormat;
        private Integer sampleRate;
        private Integer channels;
        private Long fileSizeBytes;
        private String checksum;
        private Boolean profanityFiltered;
        private Integer wordCount;
    }
}