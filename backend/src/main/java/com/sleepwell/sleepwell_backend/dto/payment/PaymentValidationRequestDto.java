package com.sleepwell.sleepwell_backend.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * 결제 검증 요청 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentValidationRequestDto {

    /**
     * 주문번호
     */
    @NotBlank(message = "주문번호는 필수입니다")
    private String orderId;

    /**
     * 결제 금액
     */
    @NotNull(message = "결제 금액은 필수입니다")
    private BigDecimal amount;
}