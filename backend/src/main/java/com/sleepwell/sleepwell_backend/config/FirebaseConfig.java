package com.sleepwell.sleepwell_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Firebase Configuration
 * Firebase Cloud Messaging (FCM)을 위한 설정
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = "firebase",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Profile("!test")
public class FirebaseConfig {

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    @Value("${firebase.project-id:}")
    private String projectId;

    @Value("${firebase.private-key:}")
    private String privateKey;

    @Value("${firebase.client-email:}")
    private String clientEmail;

    @Value("${firebase.private-key-id:}")
    private String privateKeyId;

    @Value("${firebase.client-id:}")
    private String clientId;
    
    private final ResourceLoader resourceLoader;
    
    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        log.info("Firebase 초기화 시작");

        // 이미 초기화되었는지 확인
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase가 이미 초기화되어 있습니다");
            return FirebaseApp.getInstance();
        }

        try {
            GoogleCredentials credentials;
            
            // JSON 파일 경로가 제공된 경우
            if (StringUtils.hasText(credentialsPath)) {
                log.info("Firebase 서비스 계정 JSON 파일 사용: {}", credentialsPath);
                
                Resource resource = resourceLoader.getResource(credentialsPath);
                if (!resource.exists()) {
                    // 절대 경로로 시도
                    resource = resourceLoader.getResource("file:" + credentialsPath);
                }
                
                try (InputStream serviceAccount = resource.getInputStream()) {
                    // 빈 파일인 경우 Firebase 비활성화
                    if (serviceAccount.available() == 0) {
                        log.warn("Firebase 서비스 계정 파일이 비어있습니다. Firebase 비활성화됩니다.");
                        return null;
                    }
                    credentials = GoogleCredentials.fromStream(serviceAccount);
                }
            }
            // 환경 변수로부터 설정
            else if (StringUtils.hasText(projectId) && StringUtils.hasText(privateKey) && StringUtils.hasText(clientEmail)) {
                log.info("Firebase 환경 변수 사용");
                
                String serviceAccountJson = String.format("""
                    {
                      "type": "service_account",
                      "project_id": "%s",
                      "private_key_id": "%s",
                      "private_key": "%s",
                      "client_email": "%s",
                      "client_id": "%s",
                      "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                      "token_uri": "https://oauth2.googleapis.com/token",
                      "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                      "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/%s"
                    }
                    """, 
                    projectId,
                    privateKeyId != null ? privateKeyId : "default-key-id",
                    privateKey.replace("\\n", "\n"),
                    clientEmail,
                    clientId != null ? clientId : "default-client-id",
                    clientEmail
                );

                ByteArrayInputStream serviceAccountStream = 
                    new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
                credentials = GoogleCredentials.fromStream(serviceAccountStream);
            }
            else {
                log.error("Firebase 설정이 불완전합니다. credentials-path 또는 필수 환경 변수를 설정하세요.");
                throw new IllegalStateException("Firebase 설정이 불완전합니다");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId != null ? projectId : "sleepwell-app")
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            
            log.info("Firebase 초기화 완료 - 프로젝트 ID: {}", projectId);
            return app;
            
        } catch (Exception e) {
            log.error("Firebase 초기화 실패", e);
            throw new IOException("Firebase 초기화 실패", e);
        }
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "firebase",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
    )
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        if (firebaseApp == null) {
            log.warn("FirebaseApp이 null입니다. FirebaseMessaging을 생성할 수 없습니다.");
            return null;
        }
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}