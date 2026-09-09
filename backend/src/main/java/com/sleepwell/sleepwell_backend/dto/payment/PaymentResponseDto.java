package com.sleepwell.sleepwell_backend.dto.payment;

import com.sleepwell.sleepwell_backend.enums.PaymentMethod;
import com.sleepwell.sleepwell_backend.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 정보 응답 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {

    /**
     * 결제 ID
     */
    private Long paymentId;

    /**
     * 주문번호
     */
    private String orderId;

    /**
     * 거래번호
     */
    private String transactionId;

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 구독 ID
     */
    private Long subscriptionId;

    /**
     * 결제 금액
     */
    private BigDecimal amount;

    /**
     * 통화
     */
    private String currency;

    /**
     * 결제 방법
     */
    private PaymentMethod paymentMethod;

    /**
     * 결제 상태
     */
    private PaymentStatus status;

    /**
     * PG사
     */
    private String pgProvider;

    /**
     * 상품명/설명
     */
    private String description;

    /**
     * 환불된 금액
     */
    private BigDecimal refundedAmount;

    /**
     * 환불 가능 금액
     */
    private BigDecimal refundableAmount;

    /**
     * 결제 요청 시간
     */
    private LocalDateTime createdAt;

    /**
     * 결제 완료 시간
     */
    private LocalDateTime paidAt;

    /**
     * 취소/실패 시간
     */
    private LocalDateTime cancelledAt;

    /**
     * 실패/취소 사유
     */
    private String failureReason;
    
    /**
     * 상품명
     */
    private String productName;
    
    /**
     * 메타데이터
     */
    private String metadata;
}