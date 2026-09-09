package com.sleepwell.sleepwell_backend.config;

import com.sleepwell.sleepwell_backend.security.JwtAuthenticationFilter;
import com.sleepwell.sleepwell_backend.security.oauth2.CustomAuthorizationRequestResolver;
import com.sleepwell.sleepwell_backend.security.oauth2.CustomOAuth2UserService;
import com.sleepwell.sleepwell_backend.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sleepwell.sleepwell_backend.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.sleepwell.sleepwell_backend.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import java.util.Arrays;

/**
 * Spring Security 설정
 * JWT 기반 인증 시스템 구성
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용으로 불필요)
            .csrf(csrf -> csrf.disable())
            
            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 세션 사용 안함 (JWT 기반 stateless 인증)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 요청별 인증 규칙 설정
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능한 경로들
                .requestMatchers("/api/auth/**").permitAll()           // 로그인, 회원가입
                .requestMatchers("/api/test/public").permitAll()       // 공개 테스트 API
                .requestMatchers("/api/test/health").permitAll()       // 헬스체크 (인증 없이 접근)
                .requestMatchers("/api/test/version").permitAll()      // 버전 확인 (인증 없이 접근)
                .requestMatchers("/oauth2/**").permitAll()             // OAuth2 관련 경로
                .requestMatchers("/login/oauth2/**").permitAll()       // OAuth2 로그인 경로
                .requestMatchers("/h2-console/**").permitAll()         // H2 Console (개발용)
                .requestMatchers("/swagger-ui/**").permitAll()        // Swagger UI
                .requestMatchers("/swagger-ui.html").permitAll()      // Swagger UI 메인
                .requestMatchers("/v3/api-docs/**").permitAll()       // OpenAPI 문서
                .requestMatchers("/v3/api-docs").permitAll()          // OpenAPI 문서 루트
                .requestMatchers("/api-docs/**").permitAll()          // API 문서
                .requestMatchers("/api-docs/swagger-config").permitAll() // Swagger 설정
                .requestMatchers("/favicon.ico").permitAll()          // 파비콘
                .requestMatchers("/error").permitAll()                // 에러 페이지
                .requestMatchers("/api/payments/webhook/**").permitAll() // 결제 웹훅 (인증 없이 접근 가능)
                .requestMatchers("/actuator/health").permitAll()      // Spring Boot Actuator 헬스체크
                .requestMatchers("/actuator/prometheus").permitAll() // Prometheus 메트릭 수집 (모니터링)
                .requestMatchers("/actuator/info").permitAll()       // 애플리케이션 정보
                .requestMatchers("/api/voice/status").permitAll()  // 음성 서비스 상태 확인 (인증 불필요)

                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            
            // 보안 헤더 설정
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .preload(true)
                    .maxAgeInSeconds(31536000)
                )
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("script-src 'self'; object-src 'self'")
                )
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                 // .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)) // Deprecated
                .contentTypeOptions(options -> options.disable()) // H2 Console을 위해 임시 비활성화, 추후 제거 필요
            )
            
            // OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                // 커스텀 Authorization Request Resolver 설정
                // mode 파라미터를 state에 저장하여 콜백까지 유지
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(customAuthorizationRequestResolver())
                )
                .tokenEndpoint(token -> token
                    .accessTokenResponseClient(accessTokenResponseClient())
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            )
            
            // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // 인증 실패 시 401 반환하도록 설정
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    // SSE 스트리밍 등으로 응답이 이미 커밋된 경우 무시 (Spring Security Issue #16266)
                    if (!response.isCommitted()) {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Unauthorized\"}");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // SSE 스트리밍 등으로 응답이 이미 커밋된 경우 무시 (Spring Security Issue #16266)
                    if (!response.isCommitted()) {
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Access Denied\"}");
                    }
                })
            );

        return http.build();
    }

    /**
     * 커스텀 OAuth2 Authorization Request Resolver
     *
     * mode 파라미터를 OAuth2 state에 인코딩하여 콜백 시점까지 유지합니다.
     * Google, Kakao, Naver 모두 동일하게 동작합니다.
     *
     * @return CustomAuthorizationRequestResolver
     */
    @Bean
    public CustomAuthorizationRequestResolver customAuthorizationRequestResolver() {
        return new CustomAuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
        );
    }

    /**
     * Kakao OAuth2 토큰 요청을 위한 커스텀 AccessTokenResponseClient
     *
     * Kakao는 client_secret_post 방식을 사용하는데,
     * Spring Security 기본 설정은 client_secret_basic만 지원합니다.
     *
     * 이 Bean은 모든 OAuth2 제공자(Google, Kakao, Naver)에 대해
     * client_secret_post 방식을 지원하도록 설정합니다.
     */
    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();
        // Kakao를 위해 client_secret_post 방식 지원
        // Google, Naver도 이 방식 호환
        return client;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 인증 매니저 Bean 등록
     * JWT 토큰 생성 시 사용자 인증에 필요
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * DAO 인증 제공자 설정
     * UserDetailsService와 PasswordEncoder를 연결
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * CORS 설정
     * 프론트엔드와의 통신을 위한 CORS 정책 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 오리진 (프로덕션 웹사이트 + 로컬 개발)
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "https://www.restdawn.com",     // 프로덕션 웹사이트
            "https://restdawn.com",         // www 없는 도메인도 허용
            "https://app.restdawn.com",     // 프로덕션 웹앱
            "https://admin.restdawn.com",   // 관리자 페이지
            "http://localhost:*",           // 로컬 개발 (포트 무관)
            "http://127.0.0.1:*"            // 로컬 개발 (IP)
        ));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 자격 증명 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);
        
        // 브라우저가 캐시할 수 있는 시간 (초)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
} 