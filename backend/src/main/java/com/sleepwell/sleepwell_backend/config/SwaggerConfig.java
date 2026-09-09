package com.sleepwell.sleepwell_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Swagger/OpenAPI 포괄적 설정
 * 
 * SleepWell 백엔드 API에 대한 상세한 문서화를 제공합니다.
 * 
 * 주요 기능:
 * - JWT 인증 스키마 설정
 * - API 태그 그룹화
 * - 서버 환경 정보 제공
 * - 연락처 및 라이센스 정보 포함
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Configuration
public class SwaggerConfig {
    
    @Value("${swagger.server.url:http://localhost:8080}")
    private String serverUrl;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .tags(createTags())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", createSecurityScheme()));
    }

    /**
     * API 정보 생성
     */
    private Info createApiInfo() {
        return new Info()
                .title("SleepWell 백엔드 API")
                .version("1.0.0")
                .description("수면 분석 및 AI 상담 서비스를 위한 백엔드 API\n\n" +
                        "## 주요 기능\n" +
                        "- 🌙 다중 플랫폼 수면 데이터 통합 (Samsung Health, Apple Health, Google Fit)\n" +
                        "- 🤖 AI 기반 수면 분석 (OpenAI GPT-4, Claude)\n" +
                        "- 💬 실시간 AI 수면 상담 서비스\n" +
                        "- 📊 개인화된 수면 점수 및 트렌드 분석\n" +
                        "- 🔔 Firebase Cloud Messaging 기반 스마트 알림\n" +
                        "- 💳 아임포트/토스페이먼츠 연동 구독 결제\n" +
                        "- 🎙️ 수면 중 음성 이벤트 분석 (코골이, 수면무호흡)\n" +
                        "- 📈 SpO2 (혈중 산소 포화도) 데이터 분석\n\n" +
                        "## 인증\n" +
                        "JWT Bearer 토큰을 사용합니다.\n" +
                        "Authorization 헤더에 'Bearer {token}' 형식으로 전달하세요.\n\n" +
                        "## Rate Limiting\n" +
                        "- 인증 API: 분당 10회\n" +
                        "- 일반 API: 분당 60회\n" +
                        "- 분석 API: 시간당 10회\n\n" +
                        "## 응답 형식\n" +
                        "모든 API 응답은 JSON 형식으로 제공됩니다.")
                .contact(new Contact()
                        .name("SleepWell API Support")
                        .email("support@sleepwell.com")
                        .url("https://sleepwell.com/support"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * 서버 정보 생성
     */
    private List<Server> createServers() {
        List<Server> servers = new ArrayList<>();

        // 프로덕션 환경이면 프로덕션 서버를 먼저 추가
        if ("prod".equals(activeProfile)) {
            // HTTPS 도메인 (우선순위)
            servers.add(new Server()
                    .url("https://api.restdawn.com")
                    .description("Production Server (HTTPS)"));
            // HTTP IP (백업)
            servers.add(new Server()
                    .url("http://43.202.140.2")
                    .description("Production Server (HTTP)"));
        }

        // 로컬 서버는 항상 추가
        servers.add(new Server()
                .url("http://localhost:8080")
                .description("Local Development Server"));

        // 프로덕션이 아닌 환경에서는 프로덕션 서버를 두 번째로 추가
        if (!"prod".equals(activeProfile)) {
            servers.add(new Server()
                    .url("https://api.restdawn.com")
                    .description("Production Server (HTTPS)"));
            servers.add(new Server()
                    .url("http://43.202.140.2")
                    .description("Production Server (HTTP)"));
        }

        return servers;
    }

    /**
     * API 태그 정의
     *
     * 참고: 각 컨트롤러의 @Tag 어노테이션과 name이 정확히 일치해야 합니다.
     * 불일치 시 Swagger UI에 중복 태그가 표시됩니다.
     */
    private List<Tag> createTags() {
        return Arrays.asList(
                // 인증 및 사용자
                new Tag()
                        .name("인증")
                        .description("사용자 로그인, 회원가입, 토큰 검증 관련 API"),
                new Tag()
                        .name("사용자 프로필")
                        .description("사용자 프로필 관리 및 설정"),

                // 수면 관련
                new Tag()
                        .name("수면 기록")
                        .description("수면 기록 관리 API"),
                new Tag()
                        .name("수면일지")
                        .description("수면일지 관리 API"),
                new Tag()
                        .name("수면 분석")
                        .description("수면 데이터 분석 및 통계"),
                new Tag()
                        .name("수면 분석 실행")
                        .description("수면 분석 작업 예약 및 실행 관리 API"),
                new Tag()
                        .name("수면 설정")
                        .description("개인화된 수면 알림 및 스케줄 설정 관리"),

                // AI 기능
                new Tag()
                        .name("AI 수면 상담")
                        .description("AI 기반 개인화 수면 상담 및 코칭 서비스"),
                new Tag()
                        .name("RAG 질의응답")
                        .description("수면 의학 전문 문서 기반 AI 질의응답 API"),
                new Tag()
                        .name("RAG 관리")
                        .description("RAG 인덱싱 및 관리 API (관리자 전용)"),

                // 알림
                new Tag()
                        .name("알림")
                        .description("알림 관련 API"),
                new Tag()
                        .name("Firebase Cloud Messaging")
                        .description("FCM 푸시 알림 관련 API"),
                new Tag()
                        .name("알림 템플릿")
                        .description("알림 템플릿 관리 API"),

                // ASMR
                new Tag()
                        .name("ASMR")
                        .description("ASMR 콘텐츠 관리 API"),
                new Tag()
                        .name("ASMR 스트리밍")
                        .description("ASMR 오디오 스트리밍 및 품질 관리"),
                new Tag()
                        .name("ASMR 파일 관리")
                        .description("ASMR 오디오 파일 업로드 및 관리"),

                // 기타
                new Tag()
                        .name("구독 및 결제")
                        .description("결제 관련 API"),
                new Tag()
                        .name("음성 처리")
                        .description("음성 처리 API (STT/TTS)"),
                new Tag()
                        .name("데이터 시각화")
                        .description("수면 분석 대시보드 및 데이터 시각화 API"),
                new Tag()
                        .name("관리자")
                        .description("관리자 전용 API"),
                new Tag()
                        .name("테스트")
                        .description("인증 및 시스템 테스트 API"),
                new Tag()
                        .name("Sleep Setting Test")
                        .description("수면 설정 테스트 API")
        );
    }

    /**
     * JWT 보안 스키마 생성
     */
    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT 인증 토큰을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다.");
    }
} 