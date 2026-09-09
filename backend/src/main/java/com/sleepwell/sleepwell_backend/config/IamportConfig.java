package com.sleepwell.sleepwell_backend.config;

import com.iamport.IamportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 아임포트 설정 클래스
 * 다중 PG사 지원을 위한 아임포트 연동 설정
 */
@Configuration
public class IamportConfig {

    @Value("${payment.iamport.api-key:test_api_key}")
    private String apiKey;

    @Value("${payment.iamport.api-secret:test_api_secret}")
    private String apiSecret;

    @Value("${payment.iamport.base-url:https://api.iamport.kr}")
    private String baseUrl;

    @Value("${payment.iamport.timeout:30}")
    private int timeout;

    @Value("${payment.iamport.max-retry:3}")
    private int maxRetry;

    @Value("${payment.iamport.sandbox:true}")
    private boolean sandbox;

    @Bean
    public IamportClient iamportClient() {
        return new IamportClient(apiKey, apiSecret);
    }

    // Getters
    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public boolean isSandbox() {
        return sandbox;
    }
}