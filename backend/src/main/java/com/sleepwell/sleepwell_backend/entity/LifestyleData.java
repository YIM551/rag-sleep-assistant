package com.sleepwell.sleepwell_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 생활 패턴 데이터 엔티티 (LifestyleData Entity)
 * 
 * 사용자의 일상 활동과 생활 패턴을 수집하여 수면과의 상관관계 분석을 위한 
 * 머신러닝 모델 학습에 활용할 데이터를 저장하는 엔티티입니다.
 * 
 * 수집 데이터 범위:
 * - 신체 활동: 운동 유형, 강도, 지속 시간
 * - 식음료 섭취: 카페인, 알코올, 수분 섭취량과 시간
 * - 심리적 상태: 스트레스 수준, 기분 상태
 * - 환경적 요인: 활동 장소, 날씨 조건
 * - 디지털 활동: 화면 노출 시간, 블루라이트 노출
 * 
 * ML 활용 목적:
 * - Phase 2: 생활 패턴과 수면 품질 상관관계 분석
 * - Phase 3: 개인화된 수면 개선 권장사항 생성
 * - Phase 4: 예측 모델을 통한 수면 품질 예측
 * 
 * 성능 최적화:
 * - 사용자별 일자 기준 조회 최적화
 * - 기간별 생활 패턴 분석 최적화
 * - 활동 유형별 통계 분석 최적화
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see User
 * @see SleepRecord
 * @see BaseEntity
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(indexes = {
    // 사용자별 일자 기준 조회 (메인 대시보드 생활 패턴)
    @Index(name = "IDX_LIFESTYLE_USER_DATE", columnList = "user_id, recordDate"),
    // 기간별 생활 패턴 분석 (트렌드 분석)
    @Index(name = "IDX_LIFESTYLE_DATE_RANGE", columnList = "recordDate, user_id"),
    // 운동 활동 분석 (운동과 수면 상관관계)
    @Index(name = "IDX_LIFESTYLE_EXERCISE", columnList = "exerciseType, exerciseIntensity"),
    // 카페인 섭취 분석 (카페인과 수면 시간 상관관계)
    @Index(name = "IDX_LIFESTYLE_CAFFEINE", columnList = "caffeineIntake, lastCaffeineTime"),
    // 스트레스 수준 분석 (스트레스와 수면 품질 관계)
    @Index(name = "IDX_LIFESTYLE_STRESS", columnList = "stressLevel, recordDate"),
    // 화면 노출 시간 분석 (블루라이트와 수면 관계)
    @Index(name = "IDX_LIFESTYLE_SCREEN_TIME", columnList = "screenTimeMinutes, recordDate")
})
public class LifestyleData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 생활 패턴 데이터 소유자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_LIFESTYLE_DATA_USER"))
    private User user;

    /**
     * 기록 일자
     */
    @Column(nullable = false)
    private LocalDate recordDate;

    // === 신체 활동 데이터 ===

    /**
     * 운동 유형 (RUNNING, WALKING, CYCLING, SWIMMING, WEIGHT_TRAINING, YOGA, etc.)
     */
    @Column(length = 50)
    private String exerciseType;

    /**
     * 운동 강도 (LOW, MODERATE, HIGH, INTENSE)
     */
    @Column(length = 20)
    private String exerciseIntensity;

    /**
     * 운동 시간 (분)
     */
    private Integer exerciseDurationMinutes;

    /**
     * 운동 시작 시간
     */
    private LocalDateTime exerciseStartTime;

    /**
     * 일일 걸음 수
     */
    private Integer dailySteps;

    /**
     * 활동 칼로리 소모량 (kcal)
     */
    private Integer activeCalories;

    // === 식음료 섭취 데이터 ===

    /**
     * 카페인 섭취량 (mg)
     */
    @Builder.Default
    private Integer caffeineIntake = 0;

    /**
     * 마지막 카페인 섭취 시간
     */
    private LocalDateTime lastCaffeineTime;

    /**
     * 알코올 섭취량 (표준 잔 수)
     */
    @Builder.Default
    private Integer alcoholIntake = 0;

    /**
     * 마지막 알코올 섭취 시간
     */
    private LocalDateTime lastAlcoholTime;

    /**
     * 수분 섭취량 (ml)
     */
    @Builder.Default
    private Integer waterIntake = 0;

    /**
     * 저녁 식사 시간
     */
    private LocalDateTime dinnerTime;

    /**
     * 늦은 간식 섭취 여부
     */
    @Builder.Default
    private Boolean lateSnack = false;

    // === 심리적 상태 데이터 ===

    /**
     * 스트레스 수준 (1-10 척도)
     */
    private Integer stressLevel;

    /**
     * 기분 상태 (VERY_GOOD, GOOD, NEUTRAL, BAD, VERY_BAD)
     */
    @Column(length = 20)
    private String moodState;

    /**
     * 중요한 사건/이벤트 여부
     */
    @Builder.Default
    private Boolean significantEvent = false;

    /**
     * 사건/이벤트 설명
     */
    @Column(length = 500)
    private String eventDescription;

    // === 환경적 요인 ===

    /**
     * 주요 활동 장소 (HOME, OFFICE, OUTDOOR, MIXED)
     */
    @Column(length = 20)
    private String primaryLocation;

    /**
     * 날씨 조건 (SUNNY, CLOUDY, RAINY, SNOWY)
     */
    @Column(length = 20)
    private String weatherCondition;

    /**
     * 실외 활동 시간 (분)
     */
    private Integer outdoorTimeMinutes;

    /**
     * 자연광 노출 시간 (분)
     */
    private Integer naturalLightExposure;

    // === 디지털 활동 데이터 ===

    /**
     * 총 화면 노출 시간 (분)
     */
    private Integer screenTimeMinutes;

    /**
     * 잠자리 전 화면 노출 시간 (분) - 수면 1시간 전
     */
    private Integer preBedrimeScreenTime;

    /**
     * 블루라이트 필터 사용 여부
     */
    @Builder.Default
    private Boolean blueLightFilterUsed = false;

    /**
     * 소셜미디어 사용 시간 (분)
     */
    private Integer socialMediaMinutes;

    // === 수면 준비 활동 ===

    /**
     * 수면 루틴 실행 여부
     */
    @Builder.Default
    private Boolean sleepRoutineFollowed = false;

    /**
     * 수면 루틴 설명 (JSON 형태)
     * 예: {"meditation": 10, "reading": 20, "bath": 15}
     */
    @Column(columnDefinition = "TEXT")
    private String sleepRoutineDetails;

    /**
     * 이완 활동 (MEDITATION, READING, MUSIC, BATH, NONE)
     */
    @Column(length = 20)
    private String relaxationActivity;

    /**
     * 이완 활동 지속 시간 (분)
     */
    private Integer relaxationDurationMinutes;

    // === 기타 측정 데이터 ===

    /**
     * 체중 (kg) - 옵션
     */
    private Double weight;

    /**
     * 체온 (°C) - 옵션
     */
    private Double bodyTemperature;

    /**
     * 추가 메모/노트
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * 데이터 소스 (MANUAL, WEARABLE, APP_INTEGRATION, SURVEY)
     */
    @Column(length = 30)
    @Builder.Default
    private String dataSource = "MANUAL";

    /**
     * 데이터 수집 시간
     */
    @Builder.Default
    private LocalDateTime collectedAt = LocalDateTime.now();

    // === 비즈니스 메서드 ===
    
    /**
     * 카페인이 수면에 영향을 줄 수 있는지 확인
     * (수면 6시간 전까지의 카페인 섭취 고려)
     */
    public boolean isCaffeineAffectingSleep(LocalDateTime bedtime) {
        if (lastCaffeineTime == null) {
            return false;
        }
        return lastCaffeineTime.isAfter(bedtime.minusHours(6));
    }
    
    /**
     * 운동이 수면에 긍정적 영향을 줄 수 있는지 확인
     * (적절한 시간의 적절한 강도 운동)
     */
    public boolean isExerciseBeneficialForSleep(LocalDateTime bedtime) {
        if (exerciseStartTime == null || exerciseDurationMinutes == null) {
            return false;
        }
        
        // 수면 3시간 전까지의 중강도 이상 운동은 수면에 부정적 영향
        if (exerciseStartTime.isAfter(bedtime.minusHours(3)) && 
            ("HIGH".equals(exerciseIntensity) || "INTENSE".equals(exerciseIntensity))) {
            return false;
        }
        
        // 적절한 운동 시간 (20분 이상)
        return exerciseDurationMinutes >= 20;
    }
    
    /**
     * 화면 노출이 수면에 부정적 영향을 줄 수 있는지 확인
     */
    public boolean isScreenTimeAffectingSleep() {
        if (preBedrimeScreenTime == null) {
            return false;
        }
        
        // 수면 1시간 전 30분 이상의 화면 노출
        return preBedrimeScreenTime > 30 && !blueLightFilterUsed;
    }
} 