package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.SleepStageType;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 수면 단계 엔티티 (SleepStage Entity)
 *
 * 하나의 수면 기록(SleepRecord) 내에서 발생하는 개별 수면 단계의
 * 시작과 종료 시간을 기록하는 엔티티입니다.
 *
 * 주요 기능:
 * - 수면 단계별 상세 시간대 기록 (깊은 잠, 얕은 잠, REM, 각성)
 * - 웨어러블 기기의 단계별 측정 데이터 저장
 * - 수면 패턴 분석을 위한 세밀한 데이터 제공
 * - 수면 주기(Sleep Cycle) 분석 지원
 *
 * 설계 원칙:
 * - SleepRecord와 1:N 관계 (한 수면 기록은 여러 단계로 구성)
 * - 시간 순서 정렬을 위한 인덱스 최적화
 * - 도메인 규칙 검증 (시작 < 종료 시간, 음수 방지 등)
 * - 불변성 보장 (Builder 패턴, 검증 메서드)
 *
 * 사용 예시:
 * <pre>
 * SleepStage deepSleep = SleepStage.builder()
 *     .sleepRecord(record)
 *     .stageType(SleepStageType.DEEP)
 *     .startTime(LocalDateTime.of(2024, 1, 1, 23, 30))
 *     .endTime(LocalDateTime.of(2024, 1, 2, 1, 0))
 *     .build();
 * </pre>
 *
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepRecord
 * @see SleepStageType
 * @see BaseEntity
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "sleep_stage",
    indexes = {
        // 수면 기록별 단계 조회 (시간 순서대로)
        @Index(name = "IDX_SLEEP_STAGE_RECORD_TIME", columnList = "sleep_record_id, start_time"),
        // 수면 단계 타입별 분석
        @Index(name = "IDX_SLEEP_STAGE_TYPE", columnList = "stage_type, sleep_record_id"),
        // 시간대별 수면 패턴 분석
        @Index(name = "IDX_SLEEP_STAGE_TIME_RANGE", columnList = "start_time, end_time")
    }
)
public class SleepStage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연관된 수면 기록
     * - 필수 관계: 수면 단계는 반드시 하나의 수면 기록에 속함
     * - Lazy Loading으로 성능 최적화
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sleep_record_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_SLEEP_STAGE_RECORD"))
    private SleepRecord sleepRecord;

    /**
     * 수면 단계 타입 (DEEP, LIGHT, REM, AWAKE, UNKNOWN)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage_type", nullable = false)
    private SleepStageType stageType;

    /**
     * 해당 단계 시작 시간
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 해당 단계 종료 시간
     */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /**
     * 해당 단계 지속 시간 (분)
     * - 계산된 값이지만 조회 성능을 위해 저장
     * - startTime과 endTime으로부터 자동 계산
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /**
     * 측정 신뢰도 (0-100)
     * - 웨어러블 기기가 제공하는 측정 정확도
     * - null일 경우 신뢰도 정보 없음
     */
    @Column(name = "confidence_score")
    private Integer confidenceScore;

    /**
     * 추가 메타데이터 (JSON 형태)
     * - 기기별 특수 정보 저장
     * - 예: {"movement_intensity": 2.5, "hrv": 45}
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // === 도메인 로직 메서드 ===

    /**
     * 지속 시간 계산 (분 단위)
     * @return 시작부터 종료까지의 분 단위 시간
     */
    public long calculateDuration() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    /**
     * 지속 시간을 자동 계산하여 설정
     * - Builder 이후 또는 시간 변경 시 호출
     */
    public void updateDurationMinutes() {
        this.durationMinutes = (int) calculateDuration();
    }

    /**
     * 특정 시간이 이 수면 단계에 포함되는지 확인
     * @param time 확인할 시간
     * @return 포함 여부
     */
    public boolean containsTime(LocalDateTime time) {
        if (time == null || startTime == null || endTime == null) {
            return false;
        }
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    /**
     * 두 수면 단계가 시간적으로 겹치는지 확인
     * @param other 비교할 다른 수면 단계
     * @return 겹침 여부
     */
    public boolean overlaps(SleepStage other) {
        if (other == null || this.startTime == null || this.endTime == null
            || other.startTime == null || other.endTime == null) {
            return false;
        }

        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }

    /**
     * 이 단계가 깊은 수면인지 확인
     */
    public boolean isDeepSleep() {
        return SleepStageType.DEEP.equals(this.stageType);
    }

    /**
     * 이 단계가 REM 수면인지 확인
     */
    public boolean isRemSleep() {
        return SleepStageType.REM.equals(this.stageType);
    }

    /**
     * 이 단계가 각성 상태인지 확인
     */
    public boolean isAwake() {
        return SleepStageType.AWAKE.equals(this.stageType);
    }

    /**
     * 수면의 질에 긍정적인 단계인지 확인 (DEEP 또는 REM)
     */
    public boolean isRestorative() {
        return isDeepSleep() || isRemSleep();
    }

    // === 도메인 규칙을 적용한 상태 변경 메서드 ===

    /**
     * 시작 시간 변경 (도메인 규칙 검증)
     */
    public void changeStartTime(LocalDateTime newStartTime) {
        if (newStartTime == null) {
            throw new BusinessException("시작 시간은 null일 수 없습니다",
                HttpStatus.BAD_REQUEST, "INVALID_START_TIME");
        }

        if (this.endTime != null && !newStartTime.isBefore(this.endTime)) {
            throw new BusinessException("시작 시간은 종료 시간보다 이전이어야 합니다",
                HttpStatus.BAD_REQUEST, "START_TIME_AFTER_END_TIME");
        }

        this.startTime = newStartTime;
        updateDurationMinutes();
    }

    /**
     * 종료 시간 변경 (도메인 규칙 검증)
     */
    public void changeEndTime(LocalDateTime newEndTime) {
        if (newEndTime == null) {
            throw new BusinessException("종료 시간은 null일 수 없습니다",
                HttpStatus.BAD_REQUEST, "INVALID_END_TIME");
        }

        if (this.startTime != null && !newEndTime.isAfter(this.startTime)) {
            throw new BusinessException("종료 시간은 시작 시간보다 이후여야 합니다",
                HttpStatus.BAD_REQUEST, "END_TIME_BEFORE_START_TIME");
        }

        this.endTime = newEndTime;
        updateDurationMinutes();
    }

    /**
     * 신뢰도 점수 변경 (도메인 규칙 검증)
     */
    public void changeConfidenceScore(Integer score) {
        if (score != null && (score < 0 || score > 100)) {
            throw new BusinessException("신뢰도 점수는 0-100 사이여야 합니다",
                HttpStatus.BAD_REQUEST, "INVALID_CONFIDENCE_SCORE");
        }
        this.confidenceScore = score;
    }

    /**
     * 수면 단계 타입 변경
     */
    public void changeStageType(SleepStageType newType) {
        if (newType == null) {
            throw new BusinessException("수면 단계 타입은 null일 수 없습니다",
                HttpStatus.BAD_REQUEST, "INVALID_STAGE_TYPE");
        }
        this.stageType = newType;
    }

    /**
     * 시작/종료 시간을 동시에 변경 (순서 의존성 제거)
     */
    public void changeTimeRange(LocalDateTime newStartTime, LocalDateTime newEndTime) {
        if (newStartTime == null) {
            throw new BusinessException("시작 시간은 null일 수 없습니다",
                HttpStatus.BAD_REQUEST, "INVALID_START_TIME");
        }
        if (newEndTime == null) {
            throw new BusinessException("종료 시간은 null일 수 없습니다",
                HttpStatus.BAD_REQUEST, "INVALID_END_TIME");
        }
        if (!newStartTime.isBefore(newEndTime)) {
            throw new BusinessException("시작 시간은 종료 시간보다 이전이어야 합니다",
                HttpStatus.BAD_REQUEST, "START_TIME_AFTER_END_TIME");
        }

        this.startTime = newStartTime;
        this.endTime = newEndTime;
        updateDurationMinutes();
    }

    // === 연관관계 편의 메서드 ===

    /**
     * 수면 기록 설정 (연관관계 편의 메서드)
     */
    public void setSleepRecord(SleepRecord sleepRecord) {
        this.sleepRecord = sleepRecord;
    }

    /**
     * 엔티티 생성 시 지속 시간 자동 계산
     */
    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        updateDurationMinutes();
    }
}
