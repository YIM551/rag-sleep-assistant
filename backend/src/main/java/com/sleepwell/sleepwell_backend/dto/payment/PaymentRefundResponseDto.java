package com.sleepwell.sleepwell_backend.dto.payment;

import com.sleepwell.sleepwell_backend.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 환불 응답 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefundResponseDto {

    /**
     * 주문번호
     */
    private String orderId;

    /**
     * 거래번호
     */
    private String transactionId;

    /**
     * 환불 처리 ID
     */
    private String refundId;

    /**
     * 결제 상태
     */
    private PaymentStatus status;

    /**
     * 원 결제 금액
     */
    private BigDecimal originalAmount;

    /**
     * 환불 금액
     */
    private BigDecimal refundAmount;

    /**
     * 총 환불 금액
     */
    private BigDecimal totalRefundedAmount;

    /**
     * 남은 금액
     */
    private BigDecimal remainingAmount;

    /**
     * 환불 사유
     */
    private String refundReason;

    /**
     * 환불 처리 시간
     */
    private LocalDateTime refundedAt;

    /**
     * 환불 영수증 URL
     */
    private String refundReceiptUrl;

    /**
     * 환불 예정일
     */
    private LocalDateTime expectedRefundDate;
}