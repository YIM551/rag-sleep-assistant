package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.Payment;
import com.sleepwell.sleepwell_backend.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 결제 Repository
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 주문번호로 결제 조회
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * 거래번호로 결제 조회
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * 사용자의 결제 내역 조회 (페이징)
     */
    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자의 특정 상태 결제 내역 조회
     */
    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);

    /**
     * 사용자의 결제 내역 조회 (날짜 범위)
     */
    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId " +
           "AND p.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY p.createdAt DESC")
    List<Payment> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 사용자의 결제 내역 조회 (날짜 범위) - 메서드 이름으로 쿼리 생성
     */
    List<Payment> findByUserIdAndCreatedAtBetween(
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    /**
     * 중복 결제 확인용 조회
     */
    List<Payment> findByUserIdAndAmountAndStatusAndCreatedAtAfter(
        Long userId,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime createdAt
    );

    /**
     * 사용자의 총 결제 금액 계산
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.user.id = :userId AND p.status = :status")
    BigDecimal calculateTotalAmountByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") PaymentStatus status
    );

    /**
     * 특정 기간 동안의 총 매출 계산
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' " +
           "AND p.paidAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalRevenue(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 구독별 결제 내역 조회
     */
    List<Payment> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);

    /**
     * 만료된 대기 중인 결제 조회 (타임아웃 처리용)
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' " +
           "AND p.createdAt < :expirationTime")
    List<Payment> findExpiredPendingPayments(
        @Param("expirationTime") LocalDateTime expirationTime
    );

    /**
     * PG사별 결제 통계
     */
    @Query("SELECT p.pgProvider, COUNT(p), SUM(p.amount) " +
           "FROM Payment p WHERE p.status = 'COMPLETED' " +
           "AND p.paidAt BETWEEN :startDate AND :endDate " +
           "GROUP BY p.pgProvider")
    List<Object[]> getPaymentStatsByProvider(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 결제 방법별 통계
     */
    @Query("SELECT p.paymentMethod, COUNT(p), SUM(p.amount) " +
           "FROM Payment p WHERE p.status = 'COMPLETED' " +
           "AND p.paidAt BETWEEN :startDate AND :endDate " +
           "GROUP BY p.paymentMethod")
    List<Object[]> getPaymentStatsByMethod(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 환불 가능한 결제 조회
     */
    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId " +
           "AND (p.status = 'COMPLETED' OR p.status = 'PARTIALLY_REFUNDED') " +
           "ORDER BY p.paidAt DESC")
    List<Payment> findRefundablePaymentsByUserId(@Param("userId") Long userId);

    /**
     * 중복 결제 확인
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p " +
           "WHERE p.user.id = :userId " +
           "AND p.amount = :amount " +
           "AND p.status IN ('PENDING', 'PROCESSING') " +
           "AND p.createdAt > :recentTime")
    boolean existsDuplicatePayment(
        @Param("userId") Long userId,
        @Param("amount") BigDecimal amount,
        @Param("recentTime") LocalDateTime recentTime
    );
    
    // Admin 기능을 위한 추가 메서드들
    
    /**
     * 월별 수익 계산
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p "
         + "WHERE p.status = 'COMPLETED' "
         + "AND YEAR(p.paidAt) = :year AND MONTH(p.paidAt) = :month")
    BigDecimal calculateMonthlyRevenue(@Param("year") int year, @Param("month") int month);
    
    /**
     * 특정 날짜의 수익 계산
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p "
         + "WHERE p.status = 'COMPLETED' "
         + "AND DATE(p.paidAt) = :date")
    BigDecimal calculateDailyRevenue(@Param("date") java.time.LocalDate date);
    
    /**
     * 필터링된 결제 검색
     */
    @Query("SELECT p FROM Payment p WHERE "
         + "(:userId IS NULL OR p.user.id = :userId) "
         + "AND (:status IS NULL OR p.status = :status) "
         + "AND (:startDate IS NULL OR p.createdAt >= :startDate) "
         + "AND (:endDate IS NULL OR p.createdAt <= :endDate)")
    Page<Payment> findWithFilters(
        @Param("userId") Long userId,
        @Param("status") String status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}