package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.UserActivityStatistics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 활동 통계 Repository
 *
 * UserActivityStatistics 엔티티에 대한 데이터베이스 접근을 제공합니다.
 * 사용자별 집계 정보를 빠르게 조회하기 위한 메서드들을 제공합니다.
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Repository
public interface UserActivityStatisticsRepository extends JpaRepository<UserActivityStatistics, Long> {

    /**
     * 사용자 ID로 통계 조회
     * PK 조회이므로 가장 빠른 성능
     *
     * @param userId 사용자 ID
     * @return 사용자 활동 통계 (Optional)
     */
    Optional<UserActivityStatistics> findByUserId(Long userId);

    /**
     * 로그인 횟수가 특정 값 이상인 사용자 조회
     * 활성 사용자 분석용
     *
     * @param minCount 최소 로그인 횟수
     * @return 조건에 맞는 사용자 통계 리스트
     */
    List<UserActivityStatistics> findByLoginCountGreaterThanEqual(Integer minCount);

    /**
     * AI 기능 총 사용 횟수가 특정 값 이상인 사용자 조회
     * 파워 유저 분석용
     *
     * @param minCount 최소 AI 기능 사용 횟수
     * @param pageable 페이지 정보
     * @return 페이징 처리된 사용자 통계
     */
    @Query("SELECT s FROM UserActivityStatistics s WHERE " +
           "(s.aiSleepyCount + s.aiInsomniaCount + s.aiSleepTestCount + " +
           " s.aiStressCount + s.aiAcupressureCount) >= :minCount " +
           "ORDER BY (s.aiSleepyCount + s.aiInsomniaCount + s.aiSleepTestCount + " +
           "          s.aiStressCount + s.aiAcupressureCount) DESC")
    Page<UserActivityStatistics> findByTotalAIFeatureCountGreaterThanEqual(
        @Param("minCount") Integer minCount,
        Pageable pageable
    );

    /**
     * 최근 로그인한 사용자 조회
     * 재방문 분석용
     *
     * @param sinceDate 기준 일시
     * @return 기준 일시 이후 로그인한 사용자 통계 리스트
     */
    List<UserActivityStatistics> findByLastLoginAtAfter(LocalDateTime sinceDate);

    /**
     * 최근 AI 기능을 사용한 사용자 조회
     * AI 기능 활성 사용자 분석용
     *
     * @param sinceDate 기준 일시
     * @return 기준 일시 이후 AI 기능을 사용한 사용자 통계 리스트
     */
    List<UserActivityStatistics> findByLastAiFeatureUsedAtAfter(LocalDateTime sinceDate);

    /**
     * 특정 AI 기능을 가장 많이 사용한 사용자 Top N 조회
     * 기능별 파워 유저 분석용
     *
     * @param pageable 페이지 정보 (limit 설정용)
     * @return 페이징 처리된 사용자 통계 (졸림 상담 기준)
     */
    Page<UserActivityStatistics> findAllByOrderByAiSleepyCountDesc(Pageable pageable);

    /**
     * 불면 상담을 가장 많이 사용한 사용자 Top N 조회
     *
     * @param pageable 페이지 정보
     * @return 페이징 처리된 사용자 통계 (불면 상담 기준)
     */
    Page<UserActivityStatistics> findAllByOrderByAiInsomniaCountDesc(Pageable pageable);

    /**
     * 수면검사를 가장 많이 사용한 사용자 Top N 조회
     *
     * @param pageable 페이지 정보
     * @return 페이징 처리된 사용자 통계 (수면검사 기준)
     */
    Page<UserActivityStatistics> findAllByOrderByAiSleepTestCountDesc(Pageable pageable);

    /**
     * 스트레스 상담을 가장 많이 사용한 사용자 Top N 조회
     *
     * @param pageable 페이지 정보
     * @return 페이징 처리된 사용자 통계 (스트레스 상담 기준)
     */
    Page<UserActivityStatistics> findAllByOrderByAiStressCountDesc(Pageable pageable);

    /**
     * 경혈 상담을 가장 많이 사용한 사용자 Top N 조회
     *
     * @param pageable 페이지 정보
     * @return 페이징 처리된 사용자 통계 (경혈 상담 기준)
     */
    Page<UserActivityStatistics> findAllByOrderByAiAcupressureCountDesc(Pageable pageable);

    /**
     * 전체 사용자의 평균 로그인 횟수 조회
     * 관리자 대시보드용
     *
     * @return 평균 로그인 횟수
     */
    @Query("SELECT AVG(s.loginCount) FROM UserActivityStatistics s")
    Double getAverageLoginCount();

    /**
     * 전체 사용자의 평균 AI 기능 사용 횟수 조회
     * 관리자 대시보드용
     *
     * @return 평균 AI 기능 사용 횟수
     */
    @Query("SELECT AVG(s.aiSleepyCount + s.aiInsomniaCount + s.aiSleepTestCount + " +
           "           s.aiStressCount + s.aiAcupressureCount) " +
           "FROM UserActivityStatistics s")
    Double getAverageTotalAIFeatureCount();

    /**
     * AI 기능별 총 사용 횟수 조회 (전체 사용자 합계)
     * PM 리포트용
     *
     * @return [졸림, 불면, 수면검사, 스트레스, 경혈] 순서의 합계
     */
    @Query("SELECT " +
           "SUM(s.aiSleepyCount), " +
           "SUM(s.aiInsomniaCount), " +
           "SUM(s.aiSleepTestCount), " +
           "SUM(s.aiStressCount), " +
           "SUM(s.aiAcupressureCount) " +
           "FROM UserActivityStatistics s")
    List<Long> getTotalAIFeatureUsageCounts();

    /**
     * 통계가 존재하는 총 사용자 수 조회
     *
     * @return 통계 레코드 수
     */
    @Query("SELECT COUNT(s) FROM UserActivityStatistics s")
    long getTotalUserCount();

    /**
     * 로그인 횟수 Top N 사용자 조회 (관리자 대시보드용)
     *
     * @param pageable 페이지 정보
     * @return 페이징 처리된 사용자 통계 (로그인 횟수 기준 내림차순)
     */
    Page<UserActivityStatistics> findAllByOrderByLoginCountDesc(Pageable pageable);
}
