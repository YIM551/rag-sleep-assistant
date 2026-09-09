package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 수면 오디오 이벤트 생성 요청 DTO
 * 
 * 웨어러블 기기에서 감지된 수면 중 오디오 이벤트 메타데이터를 받기 위한 DTO입니다.
 * 실제 오디오 파일이 아닌 분석된 메타데이터만을 처리하여 개인정보 보호와 성능을 보장합니다.
 * 
 * Flutter Health SDK 데이터 매핑:
 * - HealthDataType.ENVIRONMENTAL_AUDIO_EXPOSURE → decibelLevel
 * - 워치 자체 AI 분석 결과 → eventType, intensityLevel, confidenceScore
 * 
 * 지원하는 오디오 이벤트:
 * - SNORING: 코골이 (강도 8+ 시 의료진 상담 권장)
 * - BRUXISM: 이갈이 (강도 7+ 시 치과 상담 권장)
 * - SLEEP_TALKING: 잠꼬대 (빈도 모니터링)
 * - ENVIRONMENTAL_NOISE: 환경 소음 (수면 방해 요소 분석)
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepAudioEvent
 * @see AudioEventType
 * @see WearableSource
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SleepAudioEventRequestDto {

    /**
     * 오디오 이벤트 유형 (필수)
     */
    @NotNull(message = "오디오 이벤트 유형은 필수입니다")
    private AudioEventType eventType;

    /**
     * 이벤트 시작 시간 (필수)
     */
    @NotNull(message = "이벤트 시작 시간은 필수입니다")
    private LocalDateTime eventStartTime;

    /**
     * 이벤트 종료 시간 (선택사항)
     */
    private LocalDateTime eventEndTime;

    /**
     * 이벤트 지속 시간 (초) (필수)
     */
    @NotNull(message = "이벤트 지속 시간은 필수입니다")
    @Min(value = 1, message = "이벤트 지속 시간은 1초 이상이어야 합니다")
    @Max(value = 86400, message = "이벤트 지속 시간은 24시간을 초과할 수 없습니다")
    private Integer durationSeconds;

    /**
     * 이벤트 강도 레벨 (1-10 척도) (필수)
     */
    @NotNull(message = "이벤트 강도 레벨은 필수입니다")
    @Min(value = 1, message = "이벤트 강도는 1 이상이어야 합니다")
    @Max(value = 10, message = "이벤트 강도는 10 이하여야 합니다")
    private Integer intensityLevel;

    /**
     * 소음 레벨 (dB) - Flutter Health ENVIRONMENTAL_AUDIO_EXPOSURE
     */
    @DecimalMin(value = "0.0", message = "소음 레벨은 0dB 이상이어야 합니다")
    @DecimalMax(value = "200.0", message = "소음 레벨은 200dB 이하여야 합니다")
    @Digits(integer = 3, fraction = 2, message = "소음 레벨은 소수점 둘째 자리까지만 허용됩니다")
    private BigDecimal decibelLevel;

    /**
     * 이벤트 신뢰도 점수 (0.0-1.0)
     */
    @DecimalMin(value = "0.0", message = "신뢰도 점수는 0.0 이상이어야 합니다")
    @DecimalMax(value = "1.0", message = "신뢰도 점수는 1.0 이하여야 합니다")
    @Digits(integer = 1, fraction = 2, message = "신뢰도 점수는 소수점 둘째 자리까지만 허용됩니다")
    private BigDecimal confidenceScore;

    /**
     * 이벤트 품질 점수 (1-10)
     */
    @Min(value = 1, message = "품질 점수는 1 이상이어야 합니다")
    @Max(value = 10, message = "품질 점수는 10 이하여야 합니다")
    private Integer qualityScore;

    /**
     * 시간당 발생 빈도
     */
    @DecimalMin(value = "0.0", message = "발생 빈도는 0 이상이어야 합니다")
    @DecimalMax(value = "3600.0", message = "발생 빈도는 시간당 3600회를 초과할 수 없습니다")
    @Digits(integer = 4, fraction = 2, message = "발생 빈도는 소수점 둘째 자리까지만 허용됩니다")
    private BigDecimal frequencyPerHour;

    /**
     * 수면 단계와의 연관성 (DEEP, LIGHT, REM, AWAKE)
     */
    @Pattern(regexp = "^(DEEP|LIGHT|REM|AWAKE)$", 
             message = "수면 단계는 DEEP, LIGHT, REM, AWAKE 중 하나여야 합니다")
    private String sleepStageContext;

    /**
     * 연속 발생 여부
     */
    private Boolean isContinuous;

    /**
     * 반복 패턴 여부
     */
    private Boolean hasPattern;

    /**
     * 코골이 특화 데이터 (JSON 형태)
     * 예: {"vibration_intensity": 7, "breathing_pattern": "irregular"}
     */
    private String snoringMetadata;

    /**
     * 이갈이 특화 데이터 (JSON 형태)
     * 예: {"grinding_force": 8, "jaw_movement_pattern": "rhythmic"}
     */
    private String bruxismMetadata;

    /**
     * 잠꼬대 특화 데이터 (JSON 형태)
     * 예: {"speech_clarity": 3, "emotional_tone": "neutral"}
     */
    private String sleepTalkMetadata;

    /**
     * 환경 소음 특화 데이터 (JSON 형태)
     * 예: {"noise_type": "traffic", "external_source": true}
     */
    private String environmentalMetadata;

    /**
     * 데이터 소스 (필수)
     */
    @NotNull(message = "데이터 소스는 필수입니다")
    private WearableSource dataSource;

    /**
     * 플랫폼 고유 이벤트 ID
     */
    @Size(max = 255, message = "플랫폼 이벤트 ID는 255자를 초과할 수 없습니다")
    private String platformEventId;

    /**
     * 처리 상태 (RAW, PROCESSED, ANALYZED)
     */
    @Pattern(regexp = "^(RAW|PROCESSED|ANALYZED)$", 
             message = "처리 상태는 RAW, PROCESSED, ANALYZED 중 하나여야 합니다")
    private String processingStatus;

    /**
     * 추가 메타데이터 (JSON 형태)
     */
    private String additionalMetadata;

    /**
     * 데이터 수집 시간 (선택사항, 기본값: 현재 시간)
     */
    private LocalDateTime collectedAt;

    // === 비즈니스 로직 메서드 ===

    /**
     * 고강도 이벤트 여부 판단
     */
    public boolean isHighIntensity() {
        return intensityLevel != null && intensityLevel >= 7;
    }

    /**
     * 의료진 상담 필요 여부 판단
     */
    public boolean requiresMedicalAttention() {
        if (eventType == AudioEventType.SNORING && intensityLevel >= 8) {
            return true; // 심한 코골이는 수면무호흡 의심
        }
        if (eventType == AudioEventType.BRUXISM && intensityLevel >= 7) {
            return true; // 심한 이갈이는 치과 상담 필요
        }
        return false;
    }

    /**
     * 이벤트 심각도 레벨 계산
     */
    public String calculateSeverityLevel() {
        if (intensityLevel == null) return "UNKNOWN";
        
        if (intensityLevel <= 3) return "MILD";
        if (intensityLevel <= 6) return "MODERATE";
        if (intensityLevel <= 8) return "SEVERE";
        return "CRITICAL";
    }

    /**
     * 데이터 유효성 검증
     */
    public boolean isValidData() {
        return eventType != null && 
               eventStartTime != null && 
               durationSeconds != null && durationSeconds > 0 &&
               intensityLevel != null && intensityLevel >= 1 && intensityLevel <= 10 &&
               dataSource != null &&
               (confidenceScore == null || (confidenceScore.compareTo(BigDecimal.ZERO) >= 0 && confidenceScore.compareTo(BigDecimal.ONE) <= 0));
    }

    /**
     * 기본값 설정
     */
    public SleepAudioEventRequestDto withDefaults() {
        return this.toBuilder()
                .isContinuous(this.isContinuous != null ? this.isContinuous : false)
                .hasPattern(this.hasPattern != null ? this.hasPattern : false)
                .processingStatus(this.processingStatus != null ? this.processingStatus : "RAW")
                .collectedAt(this.collectedAt != null ? this.collectedAt : LocalDateTime.now())
                .build();
    }
} 