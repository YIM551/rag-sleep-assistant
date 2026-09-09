package com.sleepwell.sleepwell_backend.dto.payment;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 결제 통계 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatisticsDto {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 조회 기간 시작일
     */
    private LocalDateTime startDate;

    /**
     * 조회 기간 종료일
     */
    private LocalDateTime endDate;

    /**
     * 총 매출
     */
    private BigDecimal totalRevenue;

    /**
     * 총 결제 건수
     */
    private Long totalPaymentCount;

    /**
     * 성공한 결제 건수
     */
    private Long successfulPaymentCount;

    /**
     * 실패한 결제 건수
     */
    private Long failedPaymentCount;

    /**
     * 취소된 결제 건수
     */
    private Long cancelledPaymentCount;

    /**
     * 평균 결제 금액
     */
    private BigDecimal averagePaymentAmount;

    /**
     * PG사별 통계
     */
    private List<PgProviderStats> pgProviderStats;

    /**
     * 결제 방법별 통계
     */
    private List<PaymentMethodStats> paymentMethodStats;

    /**
     * 일별 매출 통계
     */
    private List<DailyRevenueStats> dailyRevenueStats;
    
    /**
     * 총 결제 금액
     */
    private BigDecimal totalAmount;
    
    /**
     * 총 환불 금액
     */
    private BigDecimal totalRefundedAmount;
    
    /**
     * 완료된 결제 건수
     */
    private long completedCount;
    
    /**
     * 실패한 결제 건수
     */
    private long failedCount;
    
    /**
     * 취소된 결제 건수
     */
    private long cancelledCount;
    
    /**
     * 평균 결제 금액
     */
    private BigDecimal averageAmount;

    /**
     * PG사별 통계
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PgProviderStats {
        private String pgProvider;
        private Long paymentCount;
        private BigDecimal totalAmount;
        private Double percentage;
    }

    /**
     * 결제 방법별 통계
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentMethodStats {
        private String paymentMethod;
        private Long paymentCount;
        private BigDecimal totalAmount;
        private Double percentage;
    }

    /**
     * 일별 매출 통계
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyRevenueStats {
        private LocalDateTime date;
        private BigDecimal revenue;
        private Long paymentCount;
    }
}