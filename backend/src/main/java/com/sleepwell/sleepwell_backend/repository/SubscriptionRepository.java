package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.Subscription;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.PaymentMethod;
import com.sleepwell.sleepwell_backend.enums.SubscriptionPlan;
import com.sleepwell.sleepwell_backend.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 구독 Repository
 * 구독 관리, 결제 조회, 만료 관리 등을 위한 쿼리 메서드들을 제공합니다.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * 사용자의 현재 활성 구독 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.user = :user AND s.status = :status")
    Optional<Subscription> findByUserAndStatus(@Param("user") User user, @Param("status") SubscriptionStatus status);

    /**
     * 사용자의 활성 구독 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.user = :user AND s.status = :status AND s.endDate > :currentTime")
    Optional<Subscription> findActiveSubscription(@Param("user") User user, 
                                                 @Param("status") SubscriptionStatus status, 
                                                 @Param("currentTime") LocalDateTime currentTime);

    /**
     * 사용자의 모든 구독 이력 조회 (최신순)
     */
    @Query("SELECT s FROM Subscription s WHERE s.user = :user ORDER BY s.createdAt DESC")
    List<Subscription> findByUser(@Param("user") User user);

    /**
     * 사용자의 구독 이력 페이징 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.user = :user ORDER BY s.createdAt DESC")
    Page<Subscription> findByUser(@Param("user") User user, Pageable pageable);

    /**
     * 특정 구독 플랜의 활성 구독자 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.planType = :planType AND s.status = :status")
    List<Subscription> findByPlanAndStatus(@Param("planType") SubscriptionPlan planType, 
                                          @Param("status") SubscriptionStatus status);

    /**
     * 특정 상태의 구독 조회
     */
    List<Subscription> findByStatus(SubscriptionStatus status);

    /**
     * 만료 예정 구독 조회 (특정 일수 이내)
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = :activeStatus AND s.endDate BETWEEN :now AND :expireThreshold")
    List<Subscription> findExpiringSubscriptions(@Param("now") LocalDateTime now, 
                                                @Param("expireThreshold") LocalDateTime expireThreshold,
                                                @Param("activeStatus") SubscriptionStatus activeStatus);

    /**
     * 자동 갱신 대상 구독 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = :activeStatus AND s.autoRenewal = true AND s.nextBillingDate <= :billingDate")
    List<Subscription> findAutoRenewalTargets(@Param("billingDate") LocalDateTime billingDate,
                                            @Param("activeStatus") SubscriptionStatus activeStatus);

    /**
     * 외부 결제 ID로 구독 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.externalPaymentId = :paymentId")
    Optional<Subscription> findByExternalPaymentId(@Param("paymentId") String externalPaymentId);

    /**
     * 쿠폰 코드로 구독 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.couponCode = :couponCode")
    List<Subscription> findByCouponCode(@Param("couponCode") String couponCode);

    /**
     * 결제 방법별 구독 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.paymentMethod = :paymentMethod AND s.status = :status")
    List<Subscription> findByPaymentMethodAndStatus(@Param("paymentMethod") PaymentMethod paymentMethod, 
                                                   @Param("status") SubscriptionStatus status);

    /**
     * 특정 기간 내 신규 구독자 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.createdAt BETWEEN :startDate AND :endDate AND s.status = :activeStatus")
    List<Subscription> findNewSubscriptions(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("activeStatus") SubscriptionStatus activeStatus);

    /**
     * 구독 플랜별 활성 구독자 수 조회
     */
    @Query("SELECT s.planType, COUNT(s) FROM Subscription s WHERE s.status = :activeStatus GROUP BY s.planType")
    List<Object[]> countActiveByPlan(@Param("activeStatus") SubscriptionStatus activeStatus);

    /**
     * 결제 방법별 활성 구독자 수 조회
     */
    @Query("SELECT s.paymentMethod, COUNT(s) FROM Subscription s WHERE s.status = :activeStatus GROUP BY s.paymentMethod")
    List<Object[]> countActiveByPaymentMethod(@Param("activeStatus") SubscriptionStatus activeStatus);

    /**
     * 월별 구독 수익 조회
     */
    @Query("SELECT YEAR(s.createdAt) as year, MONTH(s.createdAt) as month, " +
           "SUM(s.price - COALESCE(s.discountAmount, 0)) as totalRevenue, COUNT(s) as subscriptionCount " +
           "FROM Subscription s WHERE s.status = :activeStatus " +
           "GROUP BY YEAR(s.createdAt), MONTH(s.createdAt) " +
           "ORDER BY YEAR(s.createdAt) DESC, MONTH(s.createdAt) DESC")
    List<Object[]> getMonthlyRevenueStats(@Param("activeStatus") SubscriptionStatus activeStatus);

    /**
     * 구독 취소 사유별 통계
     */
    @Query("SELECT s.cancellationReason, COUNT(s) FROM Subscription s WHERE s.status = :cancelledStatus AND s.cancellationReason IS NOT NULL GROUP BY s.cancellationReason")
    List<Object[]> getCancellationStats(@Param("cancelledStatus") SubscriptionStatus cancelledStatus);

    /**
     * 사용자별 총 구독 비용 조회
     */
    @Query("SELECT SUM(s.price - COALESCE(s.discountAmount, 0)) FROM Subscription s WHERE s.user = :user")
    BigDecimal getTotalSpentByUser(@Param("user") User user);

    /**
     * 할인 적용된 구독 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.discountAmount IS NOT NULL AND s.discountAmount > 0")
    List<Subscription> findDiscountedSubscriptions();

    /**
     * 특정 기간 내 만료된 구독 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'EXPIRED' AND s.endDate BETWEEN :startDate AND :endDate")
    List<Subscription> findExpiredBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 가격 범위의 활성 구독 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.price BETWEEN :minPrice AND :maxPrice")
    List<Subscription> findByStatusAndPriceRange(@Param("status") SubscriptionStatus status, 
                                                 @Param("minPrice") BigDecimal minPrice, 
                                                 @Param("maxPrice") BigDecimal maxPrice);

    /**
     * 자동 갱신이 비활성화된 만료 예정 구독 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = :activeStatus AND s.autoRenewal = false AND s.endDate <= :expireDate")
    List<Subscription> findNonRenewingExpired(@Param("expireDate") LocalDateTime expireDate,
                                            @Param("activeStatus") SubscriptionStatus activeStatus);

    /**
     * 사용자의 최신 구독 조회
     */
    @Query("SELECT s FROM Subscription s WHERE s.user = :user ORDER BY s.createdAt DESC")
    Page<Subscription> findLatestByUser(@Param("user") User user, Pageable pageable);

    /**
     * 구독 상태별 개수 조회
     */
    long countByStatus(SubscriptionStatus status);

    /**
     * 사용자의 구독 개수 조회
     */
    long countByUser(User user);

    /**
     * 활성 프리미엄 구독자 조회 (PREMIUM 또는 PREMIUM_ANNUAL)
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = :activeStatus AND s.planType IN (:premiumPlans)")
    List<Subscription> findActivePremiumSubscriptions(@Param("activeStatus") SubscriptionStatus activeStatus,
                                                    @Param("premiumPlans") List<SubscriptionPlan> premiumPlans);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Subscription s WHERE s.user = :user")
    void deleteAllByUser(@Param("user") User user);
} 