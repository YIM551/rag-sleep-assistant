package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 수면 오디오 이벤트 엔티티 (SleepAudioEvent Entity)
 * 
 * 웨어러블 기기에서 감지된 수면 중 오디오 이벤트의 상세 메타데이터를 저장하는 엔티티입니다.
 * 실제 오디오 파일이 아닌 분석된 메타데이터만을 처리하여 개인정보 보호와 성능을 보장합니다.
 * 
 * 지원하는 오디오 이벤트:
 * - 코골이 (SNORING): 강도, 빈도, 지속시간
 * - 이갈이 (BRUXISM): 강도, 빈도, 지속시간  
 * - 잠꼬대 (SLEEP_TALKING): 빈도, 지속시간
 * - 환경 소음 (ENVIRONMENTAL_NOISE): dB 수준, 소음 유형
 * 
 * Flutter Health 데이터 매핑:
 * - HealthDataType.ENVIRONMENTAL_AUDIO_EXPOSURE → dB 수치
 * - 워치 자체 AI 분석 결과 → 이벤트 감지 및 분류
 * 
 * 성능 최적화:
 * - 수면 기록별 오디오 이벤트 조회 최적화
 * - 이벤트 유형별 통계 분석 최적화
 * - 시간대별 패턴 분석 최적화
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepRecord
 * @see User
 * @see AudioEventType
 * @see BaseEntity
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(indexes = {
    // 수면 기록별 오디오 이벤트 조회 (메인 분석)
    @Index(name = "IDX_AUDIO_EVENT_SLEEP_RECORD", columnList = "sleep_record_id, eventType"),
    // 사용자별 오디오 이벤트 트렌드 분석
    @Index(name = "IDX_AUDIO_EVENT_USER_DATE", columnList = "user_id, eventDate, eventType"),
    // 이벤트 유형별 통계 분석 (코골이, 이갈이 등)
    @Index(name = "IDX_AUDIO_EVENT_TYPE_STATS", columnList = "eventType, intensityLevel, eventDate"),
    // 시간대별 패턴 분석 (수면 단계와 오디오 이벤트 상관관계)
    @Index(name = "IDX_AUDIO_EVENT_TIME_PATTERN", columnList = "eventStartTime, eventType"),
    // 데이터 소스별 품질 관리
    @Index(name = "IDX_AUDIO_EVENT_SOURCE", columnList = "dataSource, eventDate"),
    // 고강도 이벤트 우선 조회 (의료진 상담 필요 케이스)
    @Index(name = "IDX_AUDIO_EVENT_HIGH_INTENSITY", columnList = "intensityLevel, eventType, eventDate")
})
public class SleepAudioEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 오디오 이벤트 소유자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_AUDIO_EVENT_USER"))
    private User user;

    /**
     * 연관된 수면 기록
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sleep_record_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_AUDIO_EVENT_SLEEP_RECORD"))
    private SleepRecord sleepRecord;

    /**
     * 오디오 이벤트 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AudioEventType eventType;

    /**
     * 이벤트 발생 일자
     */
    @Column(nullable = false)
    private LocalDateTime eventDate;

    /**
     * 이벤트 시작 시간
     */
    @Column(nullable = false)
    private LocalDateTime eventStartTime;

    /**
     * 이벤트 종료 시간
     */
    private LocalDateTime eventEndTime;

    /**
     * 이벤트 지속 시간 (초)
     */
    @Column(nullable = false)
    private Integer durationSeconds;

    // === 강도 및 품질 메트릭 ===

    /**
     * 이벤트 강도 레벨 (1-10 척도)
     * 1: 매우 약함, 10: 매우 강함
     */
    @Column(nullable = false)
    private Integer intensityLevel;

    /**
     * 소음 레벨 (dB) - Flutter Health ENVIRONMENTAL_AUDIO_EXPOSURE
     */
    private BigDecimal decibelLevel;

    /**
     * 이벤트 신뢰도 점수 (0.0-1.0)
     * 워치 AI의 감지 신뢰도
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    /**
     * 이벤트 품질 점수 (1-10)
     * 데이터 품질 및 분석 정확도
     */
    private Integer qualityScore;

    // === 빈도 및 패턴 분석 ===

    /**
     * 시간당 발생 빈도
     */
    private BigDecimal frequencyPerHour;

    /**
     * 수면 단계와의 연관성
     * DEEP, LIGHT, REM, AWAKE
     */
    @Column(length = 10)
    private String sleepStageContext;

    /**
     * 연속 발생 여부
     */
    @Builder.Default
    private Boolean isContinuous = false;

    /**
     * 반복 패턴 여부
     */
    @Builder.Default
    private Boolean hasPattern = false;

    // === 이벤트별 특화 데이터 ===

    /**
     * 코골이 특화 데이터 (JSON 형태)
     * 예: {"vibration_intensity": 7, "breathing_pattern": "irregular"}
     */
    @Column(columnDefinition = "TEXT")
    private String snoringMetadata;

    /**
     * 이갈이 특화 데이터 (JSON 형태)
     * 예: {"grinding_force": 8, "jaw_movement_pattern": "rhythmic"}
     */
    @Column(columnDefinition = "TEXT")
    private String bruxismMetadata;

    /**
     * 잠꼬대 특화 데이터 (JSON 형태)
     * 예: {"speech_clarity": 3, "emotional_tone": "neutral"}
     */
    @Column(columnDefinition = "TEXT")
    private String sleepTalkMetadata;

    /**
     * 환경 소음 특화 데이터 (JSON 형태)
     * 예: {"noise_type": "traffic", "external_source": true}
     */
    @Column(columnDefinition = "TEXT")
    private String environmentalMetadata;

    // === 데이터 소스 및 메타데이터 ===

    /**
     * 데이터 소스 (Apple Watch, Galaxy Watch 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WearableSource dataSource;

    /**
     * 플랫폼 고유 이벤트 ID
     */
    @Column(length = 255)
    private String platformEventId;

    /**
     * 데이터 수집 시간
     */
    @Builder.Default
    private LocalDateTime collectedAt = LocalDateTime.now();

    /**
     * 처리 상태 (RAW, PROCESSED, ANALYZED)
     */
    @Column(length = 20)
    @Builder.Default
    private String processingStatus = "RAW";

    /**
     * 추가 메타데이터 (JSON 형태)
     * 플랫폼별 확장 데이터
     */
    @Column(columnDefinition = "TEXT")
    private String additionalMetadata;

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
     * 분당 평균 강도 계산
     */
    public BigDecimal calculateAverageIntensityPerMinute() {
        if (durationSeconds == null || durationSeconds == 0 || intensityLevel == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal durationMinutes = BigDecimal.valueOf(durationSeconds).divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);
        return BigDecimal.valueOf(intensityLevel).divide(durationMinutes, 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 데이터 품질 검증
     */
    public boolean isValidData() {
        return eventType != null && 
               eventStartTime != null && 
               durationSeconds != null && durationSeconds > 0 &&
               intensityLevel != null && intensityLevel >= 1 && intensityLevel <= 10 &&
               (confidenceScore == null || (confidenceScore.compareTo(BigDecimal.ZERO) >= 0 && confidenceScore.compareTo(BigDecimal.ONE) <= 0));
    }
} 