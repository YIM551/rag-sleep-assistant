package com.sleepwell.sleepwell_backend.config;

import com.tosspayments.TossPayments;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 토스페이먼츠 설정 클래스
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Getter
@Configuration
public class TossPaymentsConfig {

    @Value("${tosspayments.api.secret-key:test_sk_default}")
    private String secretKey;

    @Value("${tosspayments.api.client-key:test_ck_default}")
    private String clientKey;

    @Value("${tosspayments.api.is-production:false}")
    private boolean isProduction;

    @Value("${tosspayments.api.timeout:60000}")
    private int timeout;

    @Value("${tosspayments.api.max-retry:3}")
    private int maxRetry;

    /**
     * 토스페이먼츠 클라이언트 빈 생성
     */
    @Bean
    public TossPayments tossPayments() {
        log.info("토스페이먼츠 클라이언트 초기화 - Production Mode: {}", isProduction);
        
        TossPayments.Builder builder = TossPayments.builder()
                .secretKey(secretKey)
                .timeout(timeout)
                .maxRetry(maxRetry);

        if (isProduction) {
            builder.production();
        } else {
            builder.test();
        }

        return builder.build();
    }

    /**
     * 결제 성공/실패 리다이렉트 URL
     */
    @Value("${tosspayments.redirect.success-url:http://localhost:3000/payment/success}")
    private String successUrl;

    @Value("${tosspayments.redirect.fail-url:http://localhost:3000/payment/fail}")
    private String failUrl;

    /**
     * 웹훅 URL
     */
    @Value("${tosspayments.webhook.url:http://localhost:8080/api/payment/webhook/tosspayments}")
    private String webhookUrl;

    @Value("${tosspayments.webhook.secret:}")
    private String webhookSecret;

    /**
     * 결제 창 옵션
     */
    @Value("${tosspayments.window.type:iframe}")
    private String windowType;

    @Value("${tosspayments.window.easyPay.enabled:true}")
    private boolean easyPayEnabled;

    @Value("${tosspayments.window.easyPay:토스페이}")
    private String easyPayString;

    @Value("${tosspayments.api.base-url:https://api.tosspayments.com}")
    private String baseUrl;
    
    /**
     * 간편결제 옵션 리스트 반환
     */
    public List<String> getEasyPay() {
        return Arrays.asList(easyPayString.split(","));
    }
    
    /**
     * 베이스 URL 반환
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 환불 정책
     */
    @Value("${tosspayments.refund.reason.default:고객 요청}")
    private String defaultRefundReason;

    @Value("${tosspayments.refund.bank.code:004}")
    private String defaultRefundBankCode;

    /**
     * 결제 타임아웃 설정 (분)
     */
    @Value("${tosspayments.payment.timeout-minutes:15}")
    private int paymentTimeoutMinutes;
}