package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.ConsultationSession;
import com.sleepwell.sleepwell_backend.entity.ConsultationSummary;
import com.sleepwell.sleepwell_backend.entity.User;
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
 * 상담 요약 Repository
 * AI 생성 요약과 권장사항 관리를 위한 최적화된 쿼리 제공
 */
@Repository
public interface ConsultationSummaryRepository extends JpaRepository<ConsultationSummary, Long> {

    /**
     * 상담 세션별 요약 조회
     */
    Optional<ConsultationSummary> findByConsultationSession(ConsultationSession session);

    /**
     * 사용자별 요약 조회 (최신순)
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.user = :user ORDER BY cs.createdAt DESC")
    Page<ConsultationSummary> findByUser(@Param("user") User user, Pageable pageable);

    /**
     * 사용자의 최근 요약 조회
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.user = :user " +
           "ORDER BY cs.createdAt DESC LIMIT 1")
    Optional<ConsultationSummary> findLatestByUser(@Param("user") User user);

    /**
     * 높은 평가 요약 조회 (평가 4점 이상)
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.summaryRating >= 4 " +
           "ORDER BY cs.summaryRating DESC, cs.createdAt DESC")
    Page<ConsultationSummary> findHighRatedSummaries(Pageable pageable);

    /**
     * 위험도별 요약 조회
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.riskAssessment = :riskLevel " +
           "ORDER BY cs.createdAt DESC")
    List<ConsultationSummary> findByRiskLevel(@Param("riskLevel") Integer riskLevel);

    /**
     * 높은 위험도 요약 조회 (위험도 4, 5)
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.riskAssessment >= 4 " +
           "ORDER BY cs.createdAt DESC")
    List<ConsultationSummary> findHighRiskSummaries();

    /**
     * 의료진 의뢰 권장 요약 조회
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.medicalReferral IS NOT NULL " +
           "AND LENGTH(cs.medicalReferral) > 0 ORDER BY cs.createdAt DESC")
    List<ConsultationSummary> findMedicalReferralRecommended();

    /**
     * 후속 조치 필요 요약 조회
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.nextConsultationRecommended IS NOT NULL " +
           "ORDER BY cs.createdAt DESC")
    List<ConsultationSummary> findFollowUpRequired();

    /**
     * 특정 기간 내 생성된 요약 조회
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY cs.createdAt DESC")
    List<ConsultationSummary> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자별 요약 개수 조회
     */
    @Query("SELECT COUNT(cs) FROM ConsultationSummary cs WHERE cs.user = :user")
    Long countByUser(@Param("user") User user);

    /**
     * 위험도별 통계
     */
    @Query("SELECT cs.riskAssessment, COUNT(cs) FROM ConsultationSummary cs " +
           "GROUP BY cs.riskAssessment ORDER BY COUNT(cs) DESC")
    List<Object[]> getRiskLevelStatistics();

    /**
     * 평균 요약 평가 점수
     */
    @Query("SELECT AVG(cs.summaryRating) FROM ConsultationSummary cs " +
           "WHERE cs.summaryRating IS NOT NULL")
    Double getAverageSummaryRating();

    /**
     * 평균 상담 효과성 점수
     */
    @Query("SELECT AVG(cs.effectivenessScore) FROM ConsultationSummary cs " +
           "WHERE cs.effectivenessScore IS NOT NULL")
    Double getAverageConsultationEffectiveness();

    /**
     * 평균 사용자 참여도 점수
     */
    @Query("SELECT AVG(cs.engagementScore) FROM ConsultationSummary cs " +
           "WHERE cs.engagementScore IS NOT NULL")
    Double getAverageUserEngagement();

    /**
     * 의료진 의뢰 비율
     */
    @Query("SELECT COUNT(CASE WHEN cs.medicalReferral IS NOT NULL AND LENGTH(cs.medicalReferral) > 0 THEN 1 END) * 100.0 / COUNT(cs) " +
           "FROM ConsultationSummary cs")
    Double getMedicalReferralRate();

    /**
     * 후속 조치 필요 비율
     */
    @Query("SELECT COUNT(CASE WHEN cs.nextConsultationRecommended IS NOT NULL THEN 1 END) * 100.0 / COUNT(cs) " +
           "FROM ConsultationSummary cs")
    Double getFollowUpRequiredRate();

    /**
     * 식별된 수면 문제별 통계
     */
    @Query("SELECT cs.identifiedIssues, COUNT(cs) FROM ConsultationSummary cs " +
           "WHERE cs.identifiedIssues IS NOT NULL " +
           "GROUP BY cs.identifiedIssues ORDER BY COUNT(cs) DESC")
    List<Object[]> getSleepIssuesStatistics();

    /**
     * 월별 요약 통계
     */
    @Query("SELECT FUNCTION('YEAR', cs.createdAt), FUNCTION('MONTH', cs.createdAt), " +
           "COUNT(cs), AVG(cs.summaryRating), AVG(cs.effectivenessScore) " +
           "FROM ConsultationSummary cs " +
           "GROUP BY FUNCTION('YEAR', cs.createdAt), FUNCTION('MONTH', cs.createdAt) " +
           "ORDER BY FUNCTION('YEAR', cs.createdAt) DESC, FUNCTION('MONTH', cs.createdAt) DESC")
    List<Object[]> getMonthlyStatistics();

    /**
     * 높은 효과성 요약 조회 (효과성 80점 이상)
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.effectivenessScore >= 80 " +
           "ORDER BY cs.effectivenessScore DESC")
    Page<ConsultationSummary> findHighEffectivenessSummaries(Pageable pageable);

    /**
     * 높은 참여도 요약 조회 (참여도 80점 이상)
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.engagementScore >= 80 " +
           "ORDER BY cs.engagementScore DESC")
    Page<ConsultationSummary> findHighEngagementSummaries(Pageable pageable);

    /**
     * 특정 수면 문제 관련 요약 조회
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.identifiedIssues LIKE %:sleepIssue% " +
           "ORDER BY cs.createdAt DESC")
    List<ConsultationSummary> findBySleepIssue(@Param("sleepIssue") String sleepIssue);

    /**
     * 권장사항이 있는 요약 조회
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.recommendations IS NOT NULL " +
           "AND LENGTH(cs.recommendations) > 0 ORDER BY cs.createdAt DESC")
    Page<ConsultationSummary> findSummariesWithRecommendations(Pageable pageable);

    /**
     * 일별 요약 통계
     */
    @Query("SELECT CAST(cs.createdAt AS DATE), COUNT(cs), " +
           "COUNT(CASE WHEN cs.medicalReferral IS NOT NULL AND LENGTH(cs.medicalReferral) > 0 THEN 1 END), " +
           "COUNT(CASE WHEN cs.nextConsultationRecommended IS NOT NULL THEN 1 END), " +
           "AVG(cs.summaryRating) " +
           "FROM ConsultationSummary cs " +
           "GROUP BY CAST(cs.createdAt AS DATE) " +
           "ORDER BY CAST(cs.createdAt AS DATE) DESC")
    List<Object[]> getDailySummaryStatistics();

    /**
     * 사용자의 요약 평가 트렌드 조회
     */
    @Query("SELECT cs.createdAt, cs.summaryRating, cs.effectivenessScore " +
           "FROM ConsultationSummary cs WHERE cs.user = :user " +
           "ORDER BY cs.createdAt ASC")
    List<Object[]> getUserSummaryTrend(@Param("user") User user);

    /**
     * 세션 ID로 요약 조회
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.consultationSession.id = :sessionId")
    Optional<ConsultationSummary> findBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 키워드로 요약 검색
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.summaryText LIKE %:keyword% " +
           "OR cs.mainSummary LIKE %:keyword% " +
           "OR cs.keyTopics LIKE %:keyword% " +
           "OR cs.recommendations LIKE %:keyword% " +
           "ORDER BY cs.createdAt DESC")
    List<ConsultationSummary> findByKeyword(@Param("keyword") String keyword);

    /**
     * 특정 사용자의 모든 요약 조회
     */
    @Query("SELECT cs FROM ConsultationSummary cs WHERE cs.consultationSession.user.id = :userId " +
           "ORDER BY cs.createdAt DESC")
    List<ConsultationSummary> findByUserId(@Param("userId") Long userId);

    /**
     * 최근 요약 조회
     */
    @Query("SELECT cs FROM ConsultationSummary cs ORDER BY cs.createdAt DESC")
    List<ConsultationSummary> findRecentSummaries();
} 