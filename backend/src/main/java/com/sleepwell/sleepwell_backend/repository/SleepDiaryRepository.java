package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.SleepDiary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 수면일지 데이터 접근 레포지토리
 *
 * 수면일지 엔티티의 데이터베이스 작업을 담당합니다.
 * Spring Data JPA를 활용하여 기본적인 CRUD 기능과
 * 수면일지 특화된 조회 메서드들을 제공합니다.
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Repository
public interface SleepDiaryRepository extends JpaRepository<SleepDiary, Long> {

    /**
     * 특정 사용자의 특정 날짜 수면일지 조회
     * 하루에 하나의 수면일지만 작성 가능한 비즈니스 규칙 지원
     *
     * @param userId 사용자 ID
     * @param diaryDate 일지 날짜
     * @return 해당 날짜의 수면일지 (있다면)
     */
    Optional<SleepDiary> findByUserIdAndDiaryDate(Long userId, LocalDate diaryDate);

    /**
     * 특정 사용자의 모든 수면일지 조회 (페이징)
     * 최신 날짜 순으로 정렬
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 사용자의 수면일지 목록
     */
    Page<SleepDiary> findByUserIdOrderByDiaryDateDesc(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 기간별 수면일지 조회
     * 트렌드 분석 및 통계 생성에 사용
     *
     * @param userId 사용자 ID
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @return 기간 내 수면일지 목록
     */
    List<SleepDiary> findByUserIdAndDiaryDateBetweenOrderByDiaryDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 사용자의 기간별 수면일지 조회 (페이징)
     * 대용량 데이터 처리를 위한 페이징 지원
     *
     * @param userId 사용자 ID
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @param pageable 페이징 정보
     * @return 기간 내 수면일지 페이지
     */
    Page<SleepDiary> findByUserIdAndDiaryDateBetween(
            Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 특정 사용자의 최근 N일간 수면일지 조회
     * 대시보드 및 최근 동향 분석에 사용
     *
     * @param userId 사용자 ID
     * @param fromDate 조회 시작 날짜 (N일 전)
     * @return 최근 수면일지 목록
     */
    List<SleepDiary> findTop30ByUserIdAndDiaryDateGreaterThanEqualOrderByDiaryDateDesc(
            Long userId, LocalDate fromDate);

    /**
     * 특정 사용자의 운동한 날 수면일지 조회
     * 운동과 수면의 상관관계 분석에 사용
     *
     * @param userId 사용자 ID
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @return 운동한 날의 수면일지 목록
     */
    List<SleepDiary> findByUserIdAndDidExerciseTrueAndDiaryDateBetween(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 사용자의 카페인 섭취량별 수면일지 조회
     * 카페인과 수면의 상관관계 분석에 사용
     *
     * @param userId 사용자 ID
     * @param minCaffeine 최소 카페인 섭취량
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @return 조건에 맞는 수면일지 목록
     */
    List<SleepDiary> findByUserIdAndCaffeineMgGreaterThanEqualAndDiaryDateBetween(
            Long userId, Integer minCaffeine, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 사용자의 알코올 섭취량별 수면일지 조회
     * 알코올과 수면의 상관관계 분석에 사용
     *
     * @param userId 사용자 ID
     * @param minAlcohol 최소 알코올 섭취량
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @return 조건에 맞는 수면일지 목록
     */
    List<SleepDiary> findByUserIdAndAlcoholMlGreaterThanEqualAndDiaryDateBetween(
            Long userId, Integer minAlcohol, LocalDate startDate, LocalDate endDate);

    /**
     * 수면 품질 점수별 통계 조회 (네이티브 쿼리)
     * 사용자별 수면 품질 분포 분석에 사용
     *
     * @param userId 사용자 ID
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @return 품질 점수별 개수 통계
     */
    @Query("""
        SELECT sd.subjectiveSleepQuality as score, COUNT(*) as count
        FROM SleepDiary sd
        WHERE sd.user.id = :userId
        AND sd.diaryDate BETWEEN :startDate AND :endDate
        AND sd.subjectiveSleepQuality IS NOT NULL
        GROUP BY sd.subjectiveSleepQuality
        ORDER BY sd.subjectiveSleepQuality
        """)
    List<Object[]> findSleepQualityStatistics(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 생활습관별 평균 수면 품질 조회
     * 운동/카페인/알코올이 수면에 미치는 영향 분석
     *
     * @param userId 사용자 ID
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @return 생활습관별 평균 수면 품질
     */
    @Query("""
        SELECT
            sd.didExercise as exercised,
            AVG(CASE WHEN sd.caffeineMg > 200 THEN 1 ELSE 0 END) as highCaffeineRate,
            AVG(CASE WHEN sd.alcoholMl > 0 THEN 1 ELSE 0 END) as alcoholRate,
            AVG(CAST(sd.subjectiveSleepQuality as double)) as avgSleepQuality,
            COUNT(*) as count
        FROM SleepDiary sd
        WHERE sd.user.id = :userId
        AND sd.diaryDate BETWEEN :startDate AND :endDate
        AND sd.subjectiveSleepQuality IS NOT NULL
        GROUP BY sd.didExercise
        """)
    List<Object[]> findLifestyleImpactStatistics(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 사용자의 수면일지 개수 조회
     * 사용자의 일지 작성 빈도 파악에 사용
     *
     * @param userId 사용자 ID
     * @return 작성된 수면일지 개수
     */
    long countByUserId(Long userId);

    /**
     * 특정 기간 내 수면일지를 작성한 사용자 수 조회
     * 서비스 사용률 통계에 활용
     *
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @return 해당 기간 내 수면일지 작성 사용자 수
     */
    @Query("""
        SELECT COUNT(DISTINCT sd.user.id)
        FROM SleepDiary sd
        WHERE sd.diaryDate BETWEEN :startDate AND :endDate
        """)
    long countDistinctUsersByDiaryDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 사용자의 연속 작성일 조회
     * 사용자 참여도 분석에 사용
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보 (최근부터)
     * @return 날짜 순으로 정렬된 수면일지 목록
     */
    @Query("""
        SELECT sd FROM SleepDiary sd
        WHERE sd.user.id = :userId
        ORDER BY sd.diaryDate DESC
        """)
    Page<SleepDiary> findByUserIdOrderByDiaryDateDescForStreakAnalysis(
            @Param("userId") Long userId, Pageable pageable);
}