package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 수면 기록 엔티티 (SleepRecord Entity)
 * 
 * 사용자의 상세한 수면 데이터를 저장하고 관리하는 핵심 엔티티입니다.
 * 다양한 웨어러블 디바이스와 플랫폼으로부터 수집된 수면 정보를 
 * 통합하여 저장하며, AI 분석의 기초 데이터로 활용됩니다.
 * 
 * 주요 수면 데이터:
 * - 수면 시간 정보 (시작/종료 시간, 총 수면 시간)
 * - 수면 단계별 시간 (깊은 잠, 얕은 잠, REM 수면)
 * - 수면 품질 지표 (효율성, 중간 각성, 만족도)
 * - 생체 신호 (심박수, 호흡수, 혈중 산소 포화도)
 * - 환경 데이터 (온도, 습도, 조도, 소음)
 * - 수면 방해 요소 (코골이, 이갈이, 잠꼬대)
 * 
 * 데이터 소스:
 * - Apple Health (HealthKit)
 * - Samsung Health
 * - Flutter Health Plugin
 * - 수동 입력 데이터
 * - 음성 분석 결과
 * 
 * 성능 최적화:
 * - 사용자별 날짜 조회 인덱스
 * - 수면 품질 분석용 인덱스
 * - 환경 데이터 분석용 복합 인덱스
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see User
 * @see SleepAnalysis
 * @see WearableSource
 * @see BaseEntity
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_SLEEP_RECORD_USER_DATE",
            columnNames = {"user_id", "recordDate"}
        )
    },
    indexes = {
        // 사용자별 최신 수면 기록 조회 (메인 대시보드)
        @Index(name = "IDX_SLEEP_RECORD_USER_DATE", columnList = "user_id, recordDate"),
        // 사용자별 생성 시간 정렬 조회 (Using filesort 제거)
        @Index(name = "IDX_SLEEP_RECORD_USER_CREATED", columnList = "user_id, createdAt DESC"),
        // 기간별 수면 기록 조회 (날짜 범위 검색)
        @Index(name = "IDX_SLEEP_RECORD_DATE_RANGE", columnList = "recordDate, user_id"),
        // 수면 품질 기반 분석 (품질 점수별 조회)
        @Index(name = "IDX_SLEEP_RECORD_QUALITY", columnList = "sleepQualityScore, recordDate"),
        // 수면 시간대 분석용 (수면 패턴 분석)
        @Index(name = "IDX_SLEEP_RECORD_TIME_ANALYSIS", columnList = "sleepStartTime, totalSleepMinutes"),
        // 웨어러블 데이터 소스별 조회 (데이터 품질 관리)
        @Index(name = "IDX_SLEEP_RECORD_SOURCE", columnList = "wearableSource, recordDate"),
        // 환경 데이터 분석용 (최적 환경 조건 분석)
        @Index(name = "IDX_SLEEP_RECORD_ENVIRONMENT", columnList = "temperature, humidity, recordDate"),
        // 수면 방해 요소 분석 (코골이, 이갈이 등)
        @Index(name = "IDX_SLEEP_RECORD_DISRUPTION", columnList = "snoreDetected, bruxismDetected, recordDate")
    }
)
public class SleepRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 수면 기록 소유자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_SLEEP_RECORD_USER"))
    private User user;

    /**
     * 수면 시작 시간
     */
    @Column(nullable = false)
    private LocalDateTime sleepStartTime;

    /**
     * 수면 종료 시간
     */
    @Column(nullable = false)
    private LocalDateTime sleepEndTime;

    /**
     * 총 수면 시간 (분)
     */
    @Column(nullable = false)
    private Integer totalSleepMinutes;

    /**
     * 깊은 잠 시간 (분)
     */
    private Integer deepSleepMinutes;

    /**
     * 얕은 잠 시간 (분)
     */
    private Integer lightSleepMinutes;

    /**
     * REM 수면 시간 (분)
     */
    private Integer remSleepMinutes;

    /**
     * 수면 중 깬 횟수
     */
    @Builder.Default
    private Integer wakeupCount = 0;

    /**
     * 침대에 있던 총 시간 (분) - Flutter Health SLEEP_IN_BED
     * 수면 효율성 계산에 사용: (totalSleepMinutes / sleepInBedMinutes) * 100
     */
    private Integer sleepInBedMinutes;

    /**
     * 수면 중 깨어있던 시간 (분) - Flutter Health SLEEP_AWAKE
     */
    private Integer sleepAwakeMinutes;

    /**
     * 환경 데이터 - 조도 (lux)
     */
    private Double lightLevel;

    /**
     * 환경 데이터 - 소음 레벨 (dB)
     */
    private Double noiseLevel;

    /**
     * 환경 데이터 - 온도 (°C)
     */
    private Double temperature;

    /**
     * 환경 데이터 - 습도 (%)
     */
    private Double humidity;

    /**
     * 수면 중 심박수 데이터 (JSON 형태) - Flutter Health HEART_RATE
     * 예: {"average": 65, "min": 55, "max": 75, "data_points": [...]}
     */
    @Column(columnDefinition = "TEXT")
    private String heartRateData;

    /**
     * 수면 중 호흡수 데이터 (JSON 형태) - Flutter Health RESPIRATORY_RATE
     * 예: {"average": 16, "min": 12, "max": 20, "data_points": [...]}
     */
    @Column(columnDefinition = "TEXT")
    private String respiratoryRateData;
    
    /**
     * 수면 중 혈중 산소 포화도 데이터 (JSON 형태) - Flutter Health BLOOD_OXYGEN
     * 예: {"average": 97, "min": 94, "max": 99, "data_points": [...]}
     */
    @Column(columnDefinition = "TEXT")
    private String spo2Data;

    /**
     * 코골이 감지 여부
     */
    @Builder.Default
    private Boolean snoreDetected = false;

    /**
     * 이갈이 감지 여부
     */
    @Builder.Default
    private Boolean bruxismDetected = false;

    /**
     * 잠꼬대 감지 여부
     */
    @Builder.Default
    private Boolean sleepTalkDetected = false;

    /**
     * 음성 데이터 파일 경로
     */
    private String audioDataPath;

    /**
     * 웨어러블 기기 데이터 소스
     */
    @Enumerated(EnumType.STRING)
    private WearableSource wearableSource;

    /**
     * 수면 품질 점수 (1-100)
     */
    private Integer sleepQualityScore;

    /**
     * 사용자 수면 만족도 (1-5)
     */
    private Integer userSatisfaction;

    /**
     * 기록 날짜 (검색 최적화용)
     */
    @Column(nullable = false)
    private LocalDate recordDate;

    // === 양방향 관계 매핑 ===

    /**
     * 이 수면 기록에 대한 분석들
     */
    @Builder.Default
    @OneToMany(mappedBy = "sleepRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SleepAnalysis> analyses = new ArrayList<>();

    /**
     * 이 수면 기록에 대한 오디오 이벤트들
     */
    @Builder.Default
    @OneToMany(mappedBy = "sleepRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SleepAudioEvent> audioEvents = new ArrayList<>();

    /**
     * 이 수면 기록의 수면 단계들
     * - 수면 중 발생한 모든 단계별 시간대 기록
     * - 시간 순서대로 정렬하여 수면 주기 분석 가능
     */
    @Builder.Default
    @OneToMany(mappedBy = "sleepRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SleepStage> sleepStages = new ArrayList<>();

    // === 비즈니스 로직 메서드 ===

    /**
     * 수면 효율성 계산 (%)
     * 수면 효율성 = (실제 수면 시간 / 침대에 있던 시간) * 100
     */
    public Double calculateSleepEfficiency() {
        if (sleepInBedMinutes == null || sleepInBedMinutes == 0) {
            return null;
        }
        return (double) totalSleepMinutes / sleepInBedMinutes * 100;
    }

    /**
     * 총 수면 단계 시간 계산 (분)
     * Deep + Light + REM 수면 시간의 합
     */
    public Integer calculateTotalSleepStageMinutes() {
        int total = 0;
        if (deepSleepMinutes != null) total += deepSleepMinutes;
        if (lightSleepMinutes != null) total += lightSleepMinutes;
        if (remSleepMinutes != null) total += remSleepMinutes;
        return total;
    }

    /**
     * 깊은 수면 비율 계산 (%)
     */
    public Double calculateDeepSleepRatio() {
        if (deepSleepMinutes == null || totalSleepMinutes == null || totalSleepMinutes == 0) {
            return null;
        }
        return (double) deepSleepMinutes / totalSleepMinutes * 100;
    }

    /**
     * REM 수면 비율 계산 (%)
     */
    public Double calculateRemSleepRatio() {
        if (remSleepMinutes == null || totalSleepMinutes == null || totalSleepMinutes == 0) {
            return null;
        }
        return (double) remSleepMinutes / totalSleepMinutes * 100;
    }

    /**
     * 얕은 수면 비율 계산 (%)
     */
    public Double calculateLightSleepRatio() {
        if (lightSleepMinutes == null || totalSleepMinutes == null || totalSleepMinutes == 0) {
            return null;
        }
        return (double) lightSleepMinutes / totalSleepMinutes * 100;
    }

    // === 오디오 이벤트 관련 비즈니스 로직 ===

    /**
     * 특정 유형의 오디오 이벤트 개수 조회
     */
    public long getAudioEventCount(AudioEventType eventType) {
        return audioEvents.stream()
                .filter(event -> event.getEventType() == eventType)
                .count();
    }

    /**
     * 고강도 오디오 이벤트 개수 조회
     */
    public long getHighIntensityAudioEventCount() {
        return audioEvents.stream()
                .filter(SleepAudioEvent::isHighIntensity)
                .count();
    }

    /**
     * 의료진 상담이 필요한 오디오 이벤트 존재 여부
     */
    public boolean hasAudioEventsRequiringMedicalAttention() {
        return audioEvents.stream()
                .anyMatch(SleepAudioEvent::requiresMedicalAttention);
    }

    /**
     * 오디오 이벤트 기반 수면 방해 점수 계산 (0-100)
     */
    public Double calculateAudioDisruptionScore() {
        if (audioEvents.isEmpty()) {
            return 0.0;
        }

        double totalDisruption = audioEvents.stream()
                .mapToDouble(event -> {
                    double baseScore = event.getIntensityLevel() * 10.0; // 강도 기반 점수
                    double durationFactor = Math.min(event.getDurationSeconds() / 60.0, 5.0); // 최대 5분까지만 고려
                    return baseScore * (1 + durationFactor * 0.1); // 지속시간에 따른 가중치
                })
                .sum();

        return Math.min(totalDisruption / audioEvents.size(), 100.0);
    }

    /**
     * 수면 단계별 오디오 이벤트 분포 분석
     */
    public java.util.Map<String, Long> getAudioEventsBySleepStage() {
        return audioEvents.stream()
                .filter(event -> event.getSleepStageContext() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                    SleepAudioEvent::getSleepStageContext,
                    java.util.stream.Collectors.counting()
                ));
    }

    /**
     * 오디오 이벤트를 고려한 종합 수면 품질 점수 재계산
     */
    public Integer calculateAdjustedSleepQualityScore() {
        if (sleepQualityScore == null) {
            return null;
        }

        Double audioDisruption = calculateAudioDisruptionScore();
        if (audioDisruption == null || audioDisruption == 0) {
            return sleepQualityScore;
        }

        // 오디오 방해 점수에 따른 품질 점수 조정 (최대 20점 감점)
        double adjustment = Math.min(audioDisruption * 0.2, 20.0);
        return Math.max((int) (sleepQualityScore - adjustment), 0);
    }

    // === Setter 메서드 ===

    public void setDate(LocalDate date) {
        this.recordDate = date;
    }

    public void setDeepSleepMinutes(Integer minutes) {
        this.deepSleepMinutes = minutes;
    }

    public void setLightSleepMinutes(Integer minutes) {
        this.lightSleepMinutes = minutes;
    }

    public void setRemSleepMinutes(Integer minutes) {
        this.remSleepMinutes = minutes;
    }

    public void setTotalSleepMinutes(Integer minutes) {
        this.totalSleepMinutes = minutes;
    }

    public void setSleepStart(LocalDateTime startTime) {
        this.sleepStartTime = startTime;
    }

    public void setSleepEnd(LocalDateTime endTime) {
        if (sleepStartTime != null && endTime.isBefore(sleepStartTime)) {
            throw new IllegalArgumentException("수면 종료 시간은 시작 시간보다 이후여야 합니다.");
        }
        this.sleepEndTime = endTime;
    }

    public boolean isSnoreDetected() {
        return Boolean.TRUE.equals(this.snoreDetected);
    }

    public boolean isBruxismDetected() {
        return Boolean.TRUE.equals(this.bruxismDetected);
    }

    public boolean isSleepTalkDetected() {
        return Boolean.TRUE.equals(this.sleepTalkDetected);
    }
} 