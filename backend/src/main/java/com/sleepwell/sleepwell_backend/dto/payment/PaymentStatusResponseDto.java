package com.sleepwell.sleepwell_backend.dto.payment;

import com.sleepwell.sleepwell_backend.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 상태 조회 응답 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusResponseDto {

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
     * 결제 금액
     */
    private BigDecimal amount;

    /**
     * PG사 상태
     */
    private String pgStatus;

    /**
     * 마지막 업데이트 시간
     */
    private LocalDateTime lastUpdated;

    /**
     * PG사 응답 메시지
     */
    private String pgMessage;

    /**
     * 동기화 여부 (DB와 PG사 상태 일치 여부)
     */
    private boolean isSynchronized;
    
    /**
     * 환불된 금액
     */
    private BigDecimal refundedAmount;
    
    /**
     * 결제 완료 시간
     */
    private LocalDateTime paidAt;
    
    /**
     * 취소 시간
     */
    private LocalDateTime cancelledAt;
    
    /**
     * 실패 사유
     */
    private String failureReason;
}