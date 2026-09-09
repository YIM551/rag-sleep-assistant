package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.NotificationTemplate;
import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.enums.Priority;
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
 * 알림 템플릿 Repository
 * 템플릿 조회, 관리, 통계 등을 위한 쿼리 메서드들을 제공합니다.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    /**
     * 활성화된 템플릿만 조회
     */
    List<NotificationTemplate> findByIsActiveTrue();

    /**
     * 활성화된 템플릿만 페이징 조회
     */
    Page<NotificationTemplate> findByIsActiveTrue(Pageable pageable);

    /**
     * 특정 타입의 활성화된 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.type = :type AND t.isActive = true ORDER BY t.usageCount DESC")
    List<NotificationTemplate> findActiveByType(@Param("type") NotificationType type);

    /**
     * 특정 타입과 로케일의 활성화된 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.type = :type AND t.locale = :locale AND t.isActive = true ORDER BY t.usageCount DESC")
    List<NotificationTemplate> findActiveByTypeAndLocale(@Param("type") NotificationType type, @Param("locale") String locale);

    /**
     * 가장 많이 사용된 활성화된 템플릿 조회 (특정 타입과 로케일)
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.type = :type AND t.locale = :locale AND t.isActive = true ORDER BY t.usageCount DESC LIMIT 1")
    Optional<NotificationTemplate> findMostUsedByTypeAndLocale(@Param("type") NotificationType type, @Param("locale") String locale);

    /**
     * 템플릿 이름으로 조회
     */
    Optional<NotificationTemplate> findByName(String name);

    /**
     * 템플릿 이름과 활성화 상태로 조회
     */
    Optional<NotificationTemplate> findByNameAndIsActive(String name, boolean isActive);

    /**
     * 카테고리별 활성화된 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.category = :category AND t.isActive = true ORDER BY t.name")
    List<NotificationTemplate> findActiveByCategoryOrderByName(@Param("category") String category);

    /**
     * 로케일별 활성화된 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.locale = :locale AND t.isActive = true ORDER BY t.type, t.usageCount DESC")
    List<NotificationTemplate> findActiveByLocaleOrderByTypeAndUsage(@Param("locale") String locale);

    /**
     * 특정 우선순위의 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.defaultPriority = :priority AND t.isActive = true")
    List<NotificationTemplate> findActiveByDefaultPriority(@Param("priority") Priority priority);

    /**
     * 사용 횟수가 많은 상위 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true ORDER BY t.usageCount DESC")
    Page<NotificationTemplate> findActiveOrderByUsageCountDesc(Pageable pageable);

    /**
     * 최근에 사용된 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true AND t.lastUsedAt IS NOT NULL ORDER BY t.lastUsedAt DESC")
    Page<NotificationTemplate> findActiveOrderByLastUsedAtDesc(Pageable pageable);

    /**
     * 사용되지 않은 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.usageCount = 0 OR t.lastUsedAt IS NULL")
    List<NotificationTemplate> findUnused();

    /**
     * 특정 기간 동안 사용된 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.lastUsedAt BETWEEN :startDate AND :endDate ORDER BY t.usageCount DESC")
    List<NotificationTemplate> findUsedBetween(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * 템플릿 타입별 통계 조회
     */
    @Query("SELECT t.type, COUNT(t), SUM(t.usageCount) FROM NotificationTemplate t WHERE t.isActive = true GROUP BY t.type ORDER BY SUM(t.usageCount) DESC")
    List<Object[]> getStatsByType();

    /**
     * 카테고리별 통계 조회
     */
    @Query("SELECT t.category, COUNT(t), SUM(t.usageCount) FROM NotificationTemplate t WHERE t.isActive = true GROUP BY t.category ORDER BY SUM(t.usageCount) DESC")
    List<Object[]> getStatsByCategory();

    /**
     * 로케일별 통계 조회
     */
    @Query("SELECT t.locale, COUNT(t), SUM(t.usageCount) FROM NotificationTemplate t WHERE t.isActive = true GROUP BY t.locale ORDER BY SUM(t.usageCount) DESC")
    List<Object[]> getStatsByLocale();

    /**
     * 우선순위별 통계 조회
     */
    @Query("SELECT t.defaultPriority, COUNT(t), SUM(t.usageCount) FROM NotificationTemplate t WHERE t.isActive = true GROUP BY t.defaultPriority ORDER BY t.defaultPriority")
    List<Object[]> getStatsByPriority();

    /**
     * 일별 템플릿 사용 통계
     */
    @Query("SELECT CAST(t.lastUsedAt AS DATE) as date, COUNT(DISTINCT t.id) as templateCount, SUM(t.usageCount) as totalUsage FROM NotificationTemplate t WHERE t.lastUsedAt BETWEEN :startDate AND :endDate GROUP BY CAST(t.lastUsedAt AS DATE) ORDER BY CAST(t.lastUsedAt AS DATE)")
    List<Object[]> getDailyUsageStats(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * 활성화된 템플릿 개수 조회
     */
    long countByIsActiveTrue();

    /**
     * 특정 타입의 활성화된 템플릿 개수 조회
     */
    long countByTypeAndIsActiveTrue(NotificationType type);

    /**
     * 특정 카테고리의 활성화된 템플릿 개수 조회
     */
    long countByCategoryAndIsActiveTrue(String category);

    /**
     * 특정 로케일의 활성화된 템플릿 개수 조회
     */
    long countByLocaleAndIsActiveTrue(String locale);

    /**
     * 템플릿 이름 중복 확인 (자신 제외)
     */
    @Query("SELECT COUNT(t) > 0 FROM NotificationTemplate t WHERE t.name = :name AND t.id != :excludeId")
    boolean existsByNameExcludingId(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 템플릿 이름 중복 확인
     */
    boolean existsByName(String name);

    /**
     * 특정 버전의 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.version = :version ORDER BY t.name")
    List<NotificationTemplate> findByVersion(@Param("version") String version);

    /**
     * 최신 버전의 템플릿들 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.version = (SELECT MAX(t2.version) FROM NotificationTemplate t2 WHERE t2.name = t.name) AND t.isActive = true ORDER BY t.name")
    List<NotificationTemplate> findLatestVersions();

    /**
     * 특정 생성자가 만든 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.createdBy = :createdBy ORDER BY t.createdAt DESC")
    List<NotificationTemplate> findByCreatedBy(@Param("createdBy") String createdBy);

    /**
     * 템플릿 검색 (이름, 설명, 카테고리에서 키워드 검색)
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE " +
           "(LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.category) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND t.isActive = true ORDER BY t.usageCount DESC")
    Page<NotificationTemplate> searchActiveTemplates(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 만료 시간이 설정된 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.defaultExpiryHours IS NOT NULL AND t.isActive = true ORDER BY t.defaultExpiryHours")
    List<NotificationTemplate> findActiveWithExpiryTime();

    /**
     * 만료 시간이 설정되지 않은 템플릿 조회
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.defaultExpiryHours IS NULL AND t.isActive = true")
    List<NotificationTemplate> findActiveWithoutExpiryTime();
} 