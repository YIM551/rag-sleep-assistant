package com.sleepwell.sleepwell_backend.dto.payment;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 준비 응답 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPrepareResponseDto {

    /**
     * 주문번호
     */
    private String orderId;

    /**
     * 결제 금액
     */
    private BigDecimal amount;

    /**
     * 상품명
     */
    private String productName;

    /**
     * PG사별 결제 데이터
     */
    private PgPaymentData pgData;

    /**
     * 결제 만료 시간
     */
    private LocalDateTime expiresAt;

    /**
     * PG사 클라이언트 키 (토스페이먼츠)
     */
    private String clientKey;
    
    /**
     * 결제창 타입
     */
    private String windowType;
    
    /**
     * 간편결제 옵션
     */
    private java.util.List<String> easyPay;
    
    /**
     * 고객 이름
     */
    private String customerName;
    
    /**
     * 고객 이메일
     */
    private String customerEmail;
    
    /**
     * 고객 휴대폰 번호
     */
    private String customerMobilePhone;
    
    /**
     * 성공 시 리다이렉트 URL
     */
    private String successUrl;
    
    /**
     * 실패 시 리다이렉트 URL
     */
    private String failUrl;
    
    /**
     * 주문명
     */
    private String orderName;
    
    /**
     * 고객 정보
     */
    private Customer customer;
    
    /**
     * 고객 정보 클래스
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Customer {
        private String email;
        private String name;
        private String phoneNumber;
    }

    /**
     * PG사별 결제 데이터
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PgPaymentData {
        /**
         * 토스페이먼츠 클라이언트 키
         */
        private String tossClientKey;

        /**
         * 아임포트 가맹점 식별자
         */
        private String iamportMerchantUid;

        /**
         * 결제 요청 URL
         */
        private String paymentUrl;

        /**
         * 추가 파라미터
         */
        private String extraParams;
    }
}