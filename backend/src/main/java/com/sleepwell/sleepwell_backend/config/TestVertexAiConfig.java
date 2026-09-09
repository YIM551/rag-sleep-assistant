package com.sleepwell.sleepwell_backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import java.io.IOException;
import java.util.Date;

/**
 * 테스트 환경용 Vertex AI 설정
 * 실제 Google Cloud 인증 없이 테스트 실행 가능하도록 더미 인증 제공
 */
@Configuration
@Profile("test")
public class TestVertexAiConfig {
    
    @Bean
    @ConditionalOnProperty(name = "spring.ai.vertex.ai.gemini.project-id", havingValue = "dummy-project-id")
    public GoogleCredentials googleCredentials() {
        // 테스트용 더미 인증 정보 반환
        return new GoogleCredentials() {
            @Override
            public AccessToken refreshAccessToken() throws IOException {
                // 테스트용 더미 토큰 반환
                return new AccessToken("dummy-token", new Date(System.currentTimeMillis() + 3600000));
            }
        };
    }
}