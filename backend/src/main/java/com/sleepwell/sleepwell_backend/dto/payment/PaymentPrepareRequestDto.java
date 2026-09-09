package com.sleepwell.sleepwell_backend.dto.payment;

import com.sleepwell.sleepwell_backend.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 결제 준비 요청 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPrepareRequestDto {

    /**
     * 사용자 ID
     */
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;

    /**
     * 결제 금액
     */
    @NotNull(message = "결제 금액은 필수입니다")
    @DecimalMin(value = "100", message = "최소 결제 금액은 100원입니다")
    @DecimalMax(value = "10000000", message = "최대 결제 금액은 1천만원입니다")
    private BigDecimal amount;

    /**
     * 통화
     */
    @NotBlank(message = "통화는 필수입니다")
    @Pattern(regexp = "^[A-Z]{3}$", message = "통화는 3자리 대문자입니다 (예: KRW, USD)")
    @Builder.Default
    private String currency = "KRW";

    /**
     * 결제 방법
     */
    @NotNull(message = "결제 방법은 필수입니다")
    private PaymentMethod paymentMethod;

    /**
     * PG사 (TOSSPAYMENTS, IAMPORT 등)
     */
    @NotBlank(message = "PG사 정보는 필수입니다")
    private String pgProvider;

    /**
     * 상품명/결제 설명
     */
    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 100, message = "상품명은 100자 이내로 입력해주세요")
    private String productName;

    /**
     * 구독 ID (구독 결제인 경우)
     */
    private Long subscriptionId;

    /**
     * 구매자 이메일
     */
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String buyerEmail;

    /**
     * 구매자 이름
     */
    @Size(max = 50, message = "구매자 이름은 50자 이내로 입력해주세요")
    private String buyerName;

    /**
     * 구매자 전화번호
     */
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", 
             message = "전화번호 형식이 올바르지 않습니다 (예: 010-0000-0000)")
    private String buyerTel;

    /**
     * 구매자 주소
     */
    @Size(max = 200, message = "주소는 200자 이내로 입력해주세요")
    private String buyerAddr;

    /**
     * 구매자 우편번호
     */
    @Pattern(regexp = "^\\d{5}$", message = "우편번호는 5자리 숫자입니다")
    private String buyerPostcode;

    /**
     * 메타데이터 (추가 정보)
     */
    private String metadata;

    /**
     * 성공 시 리다이렉트 URL
     */
    private String successUrl;

    /**
     * 실패 시 리다이렉트 URL
     */
    private String failUrl;
}