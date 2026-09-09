package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.SleepStage;
import com.sleepwell.sleepwell_backend.enums.SleepStageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 수면 단계 데이터 접근 레포지토리
 *
 * 수면 단계 엔티티의 데이터베이스 작업을 담당합니다.
 * SleepRecord와 연계하여 수면 주기 분석, 단계별 통계 등을 제공합니다.
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Repository
public interface SleepStageRepository extends JpaRepository<SleepStage, Long> {

    /**
     * 특정 수면 기록의 모든 수면 단계 조회 (시간 순서대로)
     * 수면 주기 분석에 사용
     *
     * @param sleepRecordId 수면 기록 ID
     * @return 시간 순서대로 정렬된 수면 단계 목록
     */
    List<SleepStage> findBySleepRecordIdOrderByStartTimeAsc(Long sleepRecordId);

    /**
     * 특정 수면 기록의 특정 타입 수면 단계 조회
     * 예: DEEP 단계만 조회하여 깊은 수면 패턴 분석
     *
     * @param sleepRecordId 수면 기록 ID
     * @param stageType 수면 단계 타입
     * @return 해당 타입의 수면 단계 목록
     */
    List<SleepStage> findBySleepRecordIdAndStageTypeOrderByStartTimeAsc(
            Long sleepRecordId, SleepStageType stageType);

    /**
     * 특정 수면 기록의 회복성 수면 단계만 조회 (DEEP + REM)
     * 수면의 질 분석에 사용
     *
     * @param sleepRecordId 수면 기록 ID
     * @return DEEP 또는 REM 수면 단계 목록
     */
    @Query("""
        SELECT ss FROM SleepStage ss
        WHERE ss.sleepRecord.id = :sleepRecordId
        AND ss.stageType IN ('DEEP', 'REM')
        ORDER BY ss.startTime ASC
        """)
    List<SleepStage> findRestorativeSleepStages(@Param("sleepRecordId") Long sleepRecordId);

    /**
     * 특정 수면 기록의 각성 단계만 조회
     * 수면 방해 패턴 분석에 사용
     *
     * @param sleepRecordId 수면 기록 ID
     * @return 각성 상태 수면 단계 목록
     */
    @Query("""
        SELECT ss FROM SleepStage ss
        WHERE ss.sleepRecord.id = :sleepRecordId
        AND ss.stageType = 'AWAKE'
        ORDER BY ss.startTime ASC
        """)
    List<SleepStage> findAwakeStages(@Param("sleepRecordId") Long sleepRecordId);

    /**
     * 특정 시간대에 해당하는 수면 단계 조회
     * 특정 시각의 수면 상태 파악에 사용
     *
     * @param sleepRecordId 수면 기록 ID
     * @param targetTime 조회 시각
     * @return 해당 시각의 수면 단계 (있다면)
     */
    @Query("""
        SELECT ss FROM SleepStage ss
        WHERE ss.sleepRecord.id = :sleepRecordId
        AND ss.startTime <= :targetTime
        AND ss.endTime >= :targetTime
        """)
    List<SleepStage> findStageAtTime(
            @Param("sleepRecordId") Long sleepRecordId,
            @Param("targetTime") LocalDateTime targetTime);

    /**
     * 특정 수면 기록의 수면 단계별 총 시간 통계
     * 단계별 비율 계산에 사용
     *
     * @param sleepRecordId 수면 기록 ID
     * @return 단계별 총 시간 (분) 통계 [stageType, totalMinutes]
     */
    @Query("""
        SELECT ss.stageType, SUM(ss.durationMinutes)
        FROM SleepStage ss
        WHERE ss.sleepRecord.id = :sleepRecordId
        GROUP BY ss.stageType
        ORDER BY ss.stageType
        """)
    List<Object[]> findStageStatistics(@Param("sleepRecordId") Long sleepRecordId);

    /**
     * 특정 사용자의 기간별 수면 단계 통계
     * 장기 수면 패턴 분석에 사용
     *
     * @param userId 사용자 ID
     * @param startDate 조회 시작 시간
     * @param endDate 조회 종료 시간
     * @return 단계별 평균 시간 통계 [stageType, avgMinutes]
     */
    @Query("""
        SELECT ss.stageType, AVG(ss.durationMinutes)
        FROM SleepStage ss
        WHERE ss.sleepRecord.user.id = :userId
        AND ss.sleepRecord.sleepStartTime >= :startDate
        AND ss.sleepRecord.sleepStartTime <= :endDate
        GROUP BY ss.stageType
        ORDER BY ss.stageType
        """)
    List<Object[]> findAverageStageStatisticsByUser(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 수면 기록의 수면 주기 개수 계산
     * 수면 주기는 NREM → REM을 1주기로 간주
     *
     * @param sleepRecordId 수면 기록 ID
     * @return 수면 주기 개수
     */
    @Query("""
        SELECT COUNT(*)
        FROM SleepStage ss
        WHERE ss.sleepRecord.id = :sleepRecordId
        AND ss.stageType = 'REM'
        """)
    long countSleepCycles(@Param("sleepRecordId") Long sleepRecordId);

    /**
     * 특정 수면 기록의 첫 REM 수면까지의 시간 (분)
     * REM Latency 계산에 사용
     *
     * @param sleepRecordId 수면 기록 ID
     * @return 첫 REM 수면 시작 시각 (없으면 null)
     */
    @Query("""
        SELECT MIN(ss.startTime)
        FROM SleepStage ss
        WHERE ss.sleepRecord.id = :sleepRecordId
        AND ss.stageType = 'REM'
        """)
    LocalDateTime findFirstRemStartTime(@Param("sleepRecordId") Long sleepRecordId);

    /**
     * 특정 수면 기록의 가장 긴 각성 시간 조회
     * 최장 중간 각성 시간 분석에 사용
     *
     * @param sleepRecordId 수면 기록 ID
     * @return 가장 긴 각성 단계 (없으면 null)
     */
    @Query("""
        SELECT ss FROM SleepStage ss
        WHERE ss.sleepRecord.id = :sleepRecordId
        AND ss.stageType = 'AWAKE'
        ORDER BY ss.durationMinutes DESC
        LIMIT 1
        """)
    SleepStage findLongestAwakeStage(@Param("sleepRecordId") Long sleepRecordId);

    /**
     * 특정 수면 기록의 신뢰도가 낮은 수면 단계 조회
     * 데이터 품질 관리에 사용
     *
     * @param sleepRecordId 수면 기록 ID
     * @param minConfidence 최소 신뢰도 (0-100)
     * @return 신뢰도가 기준 미만인 수면 단계 목록
     */
    @Query("""
        SELECT ss FROM SleepStage ss
        WHERE ss.sleepRecord.id = :sleepRecordId
        AND (ss.confidenceScore IS NULL OR ss.confidenceScore < :minConfidence)
        ORDER BY ss.startTime ASC
        """)
    List<SleepStage> findLowConfidenceStages(
            @Param("sleepRecordId") Long sleepRecordId,
            @Param("minConfidence") Integer minConfidence);

    /**
     * 특정 수면 기록의 수면 단계 전환 횟수 계산
     * 수면 안정성 지표에 사용
     *
     * @param sleepRecordId 수면 기록 ID
     * @return 총 수면 단계 개수 (전환 횟수 + 1)
     */
    long countBySleepRecordId(Long sleepRecordId);

    /**
     * 특정 수면 기록의 수면 단계 삭제
     * 수면 기록 업데이트 시 기존 단계 데이터 정리용
     *
     * @param sleepRecordId 수면 기록 ID
     */
    void deleteBySleepRecordId(Long sleepRecordId);
}
