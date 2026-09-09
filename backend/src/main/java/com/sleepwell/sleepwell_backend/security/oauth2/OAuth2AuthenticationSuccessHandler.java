package com.sleepwell.sleepwell_backend.security.oauth2;

import com.sleepwell.sleepwell_backend.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 로그인 성공 핸들러
 *
 * 소셜 로그인 성공 시 JWT 토큰을 생성하고 리다이렉트합니다.
 * 세 가지 리다이렉트 방식을 지원합니다:
 *
 * 1. Deep Link 방식 (mode=deeplink 파라미터 사용)
 *    - sleepwell://oauth?token=xxx&email=xxx&name=xxx&role=xxx
 *    - Flutter 앱으로 직접 리다이렉트
 *    - 자연스러운 UX, 업계 표준 방식
 *
 * 2. Web 리다이렉트 방식 (mode=web 파라미터 사용)
 *    - https://restdawn.com?token=xxx&refreshToken=xxx&email=xxx&name=xxx&role=xxx
 *    - 웹 브라우저에서 토큰을 URL 파라미터로 받음
 *    - 웹사이트 로그인에 최적화
 *
 * 3. JSON Endpoint 방식 (기본값)
 *    - https://api.restdawn.com/oauth2/redirect?token=xxx
 *    - AuthController의 /oauth2/redirect 엔드포인트로 리다이렉트
 *    - 웹뷰에서 JSON 응답을 파싱하여 사용
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;

    @Value("${app.oauth2.authorized-redirect-uri:http://localhost:3000/oauth2/redirect}")
    private String redirectUri;

    @Value("${app.oauth2.deeplink-scheme:sleepwell}")
    private String deeplinkScheme;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            // Principal 타입 검증
            if (!(authentication.getPrincipal() instanceof OAuth2UserPrincipal)) {
                log.error("OAuth2 Principal type mismatch. Expected: OAuth2UserPrincipal, Got: {}",
                         authentication.getPrincipal().getClass().getName());
                handleAuthenticationFailure(request, response, "invalid_principal_type");
                return;
            }

            OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();

            // JWT 토큰 생성 (이메일과 역할 사용)
            String token = tokenProvider.createToken(principal.getEmail(), principal.getRole());

            // OAuth2 state에서 mode 파라미터 추출
            // CustomAuthorizationRequestResolver에서 state에 인코딩한 mode 정보를 복원
            String state = request.getParameter("state");
            String mode = CustomAuthorizationRequestResolver.extractModeFromState(state);

            // fallback: 요청 파라미터에서도 확인 (직접 접근 시)
            if (mode == null || mode.isEmpty()) {
                mode = request.getParameter("mode");
            }

            String targetUrl;

            if ("deeplink".equalsIgnoreCase(mode)) {
                // 방식 1: Deep Link 방식 (Flutter 앱)
                targetUrl = buildDeepLinkUrl(token, principal);
                log.info("OAuth2 login success - Deep Link mode for user: {}", principal.getEmail());
            } else if ("web".equalsIgnoreCase(mode)) {
                // 방식 2: Web 리다이렉트 방식 (웹 브라우저)
                String refreshToken = tokenProvider.createRefreshToken(principal.getEmail());
                targetUrl = buildWebRedirectUrl(token, refreshToken, principal);
                log.info("OAuth2 login success - Web mode for user: {}, redirecting to: restdawn.com",
                         principal.getEmail());
            } else {
                // 방식 3: JSON Endpoint 방식 (기본값, Flutter WebView)
                targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                        .queryParam("token", token)
                        .queryParam("error", "")
                        .build().toUriString();
                log.info("OAuth2 login success - JSON mode for user: {}, redirecting to: {}",
                         principal.getEmail(), targetUrl);
            }

            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (ClassCastException e) {
            log.error("OAuth2 Principal casting failed", e);
            handleAuthenticationFailure(request, response, "principal_cast_error");
        } catch (Exception e) {
            log.error("OAuth2 authentication success handling failed: {}", e.getMessage(), e);
            handleAuthenticationFailure(request, response, "authentication_processing_failed");
        }
    }

    /**
     * Deep Link URL 생성
     *
     * sleepwell://oauth?token=xxx&email=xxx&name=xxx&role=xxx 형식
     *
     * @param token JWT 토큰
     * @param principal OAuth2 사용자 정보
     * @return Deep Link URL
     */
    private String buildDeepLinkUrl(String token, OAuth2UserPrincipal principal) {
        return String.format(
            "%s://oauth?token=%s&email=%s&name=%s&role=%s",
            deeplinkScheme,
            token,
            URLEncoder.encode(principal.getEmail(), StandardCharsets.UTF_8),
            URLEncoder.encode(principal.getUser().getName(), StandardCharsets.UTF_8),
            principal.getRole()
        );
    }

    /**
     * Web 리다이렉트 URL 생성
     *
     * https://restdawn.com?token=xxx&refreshToken=xxx&email=xxx&name=xxx&role=xxx 형식
     *
     * @param token JWT 액세스 토큰
     * @param refreshToken JWT 리프레시 토큰
     * @param principal OAuth2 사용자 정보
     * @return Web 리다이렉트 URL
     */
    private String buildWebRedirectUrl(String token, String refreshToken, OAuth2UserPrincipal principal) {
        return UriComponentsBuilder
                .fromUriString("https://restdawn.com")
                .queryParam("token", token)
                .queryParam("refreshToken", refreshToken)
                .queryParam("email", principal.getEmail())
                .queryParam("name", principal.getUser().getName())
                .queryParam("role", principal.getRole())
                .build()
                .encode()  // URL 인코딩 추가 (한글 이름 처리)
                .toUriString();
    }

    /**
     * 인증 실패 처리
     *
     * Deep Link 방식은 에러 파라미터와 함께 앱으로 리다이렉트
     * Web 방식은 restdawn.com으로 에러와 함께 리다이렉트
     * JSON 방식은 /oauth2/redirect?error=xxx로 리다이렉트
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param errorCode 에러 코드
     */
    private void handleAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, String errorCode)
            throws IOException {
        String state = request.getParameter("state");
        String mode = CustomAuthorizationRequestResolver.extractModeFromState(state);

        if (mode == null || mode.isEmpty()) {
            mode = request.getParameter("mode");
        }

        String targetUrl;

        if ("deeplink".equalsIgnoreCase(mode)) {
            targetUrl = String.format("%s://oauth?error=%s", deeplinkScheme, errorCode);
        } else if ("web".equalsIgnoreCase(mode)) {
            targetUrl = UriComponentsBuilder
                    .fromUriString("https://restdawn.com")
                    .queryParam("error", errorCode)
                    .build()
                    .toUriString();
        } else {
            targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("token", "")
                    .queryParam("error", errorCode)
                    .build().toUriString();
        }

        log.info("OAuth2 authentication failure redirect: {} with error: {}", targetUrl, errorCode);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
} 