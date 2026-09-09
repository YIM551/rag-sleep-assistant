package com.sleepwell.sleepwell_backend.dto.payment;

import com.sleepwell.sleepwell_backend.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 완료 응답 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompleteResponseDto {

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
     * 결제 상태
     */
    private PaymentStatus status;

    /**
     * 결제 금액
     */
    private BigDecimal amount;

    /**
     * 결제 완료 시간
     */
    private LocalDateTime paidAt;

    /**
     * 영수증 URL
     */
    private String receiptUrl;

    /**
     * 결제 수단 정보
     */
    private PaymentMethodInfo paymentMethodInfo;

    /**
     * 결제 수단 정보
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentMethodInfo {
        /**
         * 결제 수단 타입 (카드, 계좌이체 등)
         */
        private String type;

        /**
         * 카드번호 (마스킹)
         */
        private String cardNumber;

        /**
         * 카드사명
         */
        private String cardCompany;

        /**
         * 은행명
         */
        private String bankName;
    }
}