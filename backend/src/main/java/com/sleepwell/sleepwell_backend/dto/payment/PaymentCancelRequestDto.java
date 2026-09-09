package com.sleepwell.sleepwell_backend.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 결제 취소 요청 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCancelRequestDto {

    /**
     * 주문번호
     */
    @NotBlank(message = "주문번호는 필수입니다")
    private String orderId;

    /**
     * 취소 사유
     */
    @NotBlank(message = "취소 사유는 필수입니다")
    @Size(max = 200, message = "취소 사유는 200자 이내로 입력해주세요")
    private String cancelReason;

    /**
     * 요청자 ID
     */
    @NotNull(message = "요청자 ID는 필수입니다")
    private Long requesterId;

    /**
     * 환불 계좌 은행 코드 (가상계좌 결제인 경우)
     */
    private String refundBankCode;

    /**
     * 환불 계좌번호 (가상계좌 결제인 경우)
     */
    private String refundAccountNumber;

    /**
     * 환불 계좌 예금주 (가상계좌 결제인 경우)
     */
    private String refundAccountHolder;
}