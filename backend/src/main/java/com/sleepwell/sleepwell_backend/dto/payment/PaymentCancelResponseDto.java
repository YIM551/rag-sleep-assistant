package com.sleepwell.sleepwell_backend.dto.payment;

import com.sleepwell.sleepwell_backend.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 취소 응답 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCancelResponseDto {

    /**
     * 주문번호
     */
    private String orderId;

    /**
     * 거래번호
     */
    private String transactionId;

    /**
     * 결제 상태
     */
    private PaymentStatus status;

    /**
     * 원 결제 금액
     */
    private BigDecimal originalAmount;

    /**
     * 취소 금액
     */
    private BigDecimal cancelledAmount;

    /**
     * 남은 금액
     */
    private BigDecimal remainingAmount;

    /**
     * 취소 사유
     */
    private String cancelReason;

    /**
     * 취소 일시
     */
    private LocalDateTime cancelledAt;

    /**
     * 취소 영수증 URL
     */
    private String cancelReceiptUrl;
}