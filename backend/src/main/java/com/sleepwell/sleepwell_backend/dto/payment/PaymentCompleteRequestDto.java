package com.sleepwell.sleepwell_backend.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * 결제 완료 요청 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompleteRequestDto {

    /**
     * 주문번호
     */
    @NotBlank(message = "주문번호는 필수입니다")
    private String orderId;

    /**
     * PG사 거래번호
     */
    @NotBlank(message = "거래번호는 필수입니다")
    private String transactionId;

    /**
     * 결제 금액 (검증용)
     */
    @NotNull(message = "결제 금액은 필수입니다")
    private BigDecimal amount;

    /**
     * PG사 응답 데이터
     */
    private String pgResponse;

    /**
     * 결제 키 (토스페이먼츠)
     */
    private String paymentKey;

    /**
     * 승인 번호
     */
    private String approvalNumber;

    /**
     * 카드 번호 (마스킹)
     */
    private String cardNumber;

    /**
     * 카드사 코드
     */
    private String cardCompanyCode;
}