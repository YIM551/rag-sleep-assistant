package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.Gender;
import com.sleepwell.sleepwell_backend.enums.SocialProvider;
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
 * 사용자 엔티티 Repository
 * 소셜 로그인, 사용자 관리, 활성 상태 조회 등의 기능을 제공합니다.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일로 활성 사용자 조회 (JWT 인증용)
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
    Optional<User> findActiveByEmail(@Param("email") String email);

    /**
     * 소셜 제공자와 소셜 ID로 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.socialProvider = :provider AND u.socialId = :socialId")
    Optional<User> findBySocial(@Param("provider") SocialProvider socialProvider, @Param("socialId") String socialId);

    /**
     * 소셜 제공자와 소셜 ID로 사용자 조회 (OAuth2UserService용)
     */
    Optional<User> findBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);

    /**
     * 활성 사용자 여부로 조회
     */
    List<User> findByIsActive(Boolean isActive);

    /**
     * 활성 사용자 페이징 조회
     */
    Page<User> findByIsActive(Boolean isActive, Pageable pageable);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 소셜 제공자와 소셜 ID 존재 여부 확인
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.socialProvider = :provider AND u.socialId = :socialId")
    boolean existsBySocial(@Param("provider") SocialProvider socialProvider, @Param("socialId") String socialId);

    /**
     * 마케팅 수신 동의 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.marketingConsent = :consent AND u.isActive = :active")
    List<User> findByMarketingConsent(@Param("consent") Boolean marketingConsent, @Param("active") Boolean isActive);

    /**
     * 특정 기간 내 가입한 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate AND u.isActive = true")
    List<User> findActiveUsersByCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * 마지막 로그인이 특정 기간 이전인 비활성 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :threshold AND u.isActive = true")
    List<User> findInactiveUsers(@Param("threshold") LocalDateTime lastLoginThreshold);

    /**
     * 성별별 활성 사용자 수 조회
     */
    @Query("SELECT u.gender, COUNT(u) FROM User u WHERE u.isActive = true GROUP BY u.gender")
    List<Object[]> countActiveUsersByGender();

    /**
     * 소셜 제공자별 활성 사용자 수 조회
     */
    @Query("SELECT u.socialProvider, COUNT(u) FROM User u WHERE u.isActive = true GROUP BY u.socialProvider")
    List<Object[]> countActiveUsersByProvider();

    /**
     * 이름과 이메일로 사용자 검색 (LIKE 검색)
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchActiveUsers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 연령대별 활성 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.age BETWEEN :minAge AND :maxAge")
    List<User> findActiveUsersByAge(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);

    /**
     * 활성 상태이고 삭제되지 않은 사용자 조회
     */
    List<User> findByIsActiveTrueAndDeletedAtIsNull();

    /**
     * 활성 상태이고 삭제되지 않은 사용자 수 조회
     */
    long countByIsActiveTrueAndDeletedAtIsNull();

    /**
     * 특정 날짜 이후 생성된 사용자 수 조회
     */
    long countByCreatedAtAfter(LocalDateTime createdAt);
    
    /**
     * 모든 활성 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    List<User> findAllByActiveTrue();
    
    // Admin 기능을 위한 추가 메서드들
    
    /**
     * 기간별 신규 가입자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end")
    Long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 기간별 활성 사용자 수 조회
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u WHERE u.lastLoginAt BETWEEN :start AND :end AND u.isActive = true")
    Long countActiveUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 역할별 사용자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Long countByRole(@Param("role") com.sleepwell.sleepwell_backend.enums.UserRole role);
    
    /**
     * 최근 가입 사용자 조회
     */
    List<User> findTop10ByOrderByCreatedAtDesc();
    
    /**
     * 필터링된 사용자 검색
     */
    @Query("SELECT u FROM User u WHERE "
         + "(:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) "
         + "OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
         + "AND (:role IS NULL OR u.role = :role) "
         + "AND (:isActive IS NULL OR u.isActive = :isActive) "
         + "AND (:startDate IS NULL OR u.createdAt >= :startDate)")
    Page<User> findWithFilters(
        @Param("keyword") String keyword,
        @Param("role") String role, 
        @Param("isActive") Boolean isActive,
        @Param("startDate") java.time.LocalDate startDate,
        Pageable pageable
    );
    
    /**
     * 특정 기간 이전 가입자 수
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt < :before")
    Long countByCreatedAtBefore(@Param("before") LocalDateTime before);
    
    /**
     * 활성 구독 사용자 수
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.subscriptions s WHERE s.status = 'ACTIVE' AND s.endDate > CURRENT_TIMESTAMP")
    Long countActiveSubscriptions();
    
    /**
     * 무료/체험 사용자 수
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.subscriptions s WHERE s.status = 'ACTIVE'")
    Long countTrialUsers();
    
    /**
     * 최근 수면 기록이 있는 활성 사용자 조회
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.sleepRecords sr " +
           "WHERE u.isActive = true AND sr.sleepStartTime >= :since")
    List<User> findUsersWithRecentSleepRecords(@Param("since") LocalDateTime since);
} 