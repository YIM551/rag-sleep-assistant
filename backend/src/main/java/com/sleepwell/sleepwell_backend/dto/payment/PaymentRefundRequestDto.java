package com.sleepwell.sleepwell_backend.dto.payment;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 결제 환불 요청 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefundRequestDto {

    /**
     * 주문번호
     */
    @NotBlank(message = "주문번호는 필수입니다")
    private String orderId;

    /**
     * 환불 금액
     */
    @NotNull(message = "환불 금액은 필수입니다")
    @DecimalMin(value = "100", message = "최소 환불 금액은 100원입니다")
    private BigDecimal refundAmount;

    /**
     * 환불 사유
     */
    @NotBlank(message = "환불 사유는 필수입니다")
    @Size(max = 200, message = "환불 사유는 200자 이내로 입력해주세요")
    private String refundReason;

    /**
     * 요청자 ID
     */
    @NotNull(message = "요청자 ID는 필수입니다")
    private Long requesterId;

    /**
     * 환불 계좌 정보 (가상계좌 결제인 경우)
     */
    private RefundAccountInfo refundAccountInfo;

    /**
     * 환불 계좌 정보
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefundAccountInfo {
        /**
         * 은행 코드
         */
        private String bankCode;

        /**
         * 계좌번호
         */
        private String accountNumber;

        /**
         * 예금주
         */
        private String accountHolder;
    }
}