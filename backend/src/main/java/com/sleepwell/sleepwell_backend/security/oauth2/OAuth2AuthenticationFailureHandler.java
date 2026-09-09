package com.sleepwell.sleepwell_backend.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * OAuth2 로그인 실패 핸들러
 *
 * 소셜 로그인 실패 시 에러 메시지와 함께 프론트엔드로 리다이렉트합니다.
 * 세 가지 리다이렉트 방식을 지원합니다:
 *
 * 1. Deep Link 방식 (mode=deeplink)
 *    - sleepwell://oauth?error=xxx
 *
 * 2. Web 리다이렉트 방식 (mode=web)
 *    - https://restdawn.com?error=xxx
 *
 * 3. JSON Endpoint 방식 (기본값)
 *    - /oauth2/redirect?error=xxx
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.authorized-redirect-uri:http://localhost:3000/oauth2/redirect}")
    private String redirectUri;

    @Value("${app.deeplink.scheme:sleepwell}")
    private String deeplinkScheme;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String errorMessage = exception.getLocalizedMessage();
        String errorCode = "oauth2_authentication_failed";

        log.error("OAuth2 authentication failed - Error: {}, Exception Type: {}, Message: {}",
                 errorCode, exception.getClass().getSimpleName(), errorMessage, exception);

        // OAuth2 state에서 mode 파라미터 추출
        String state = request.getParameter("state");
        String mode = CustomAuthorizationRequestResolver.extractModeFromState(state);

        if (mode == null || mode.isEmpty()) {
            mode = request.getParameter("mode");
        }

        String targetUrl;

        if ("deeplink".equalsIgnoreCase(mode)) {
            // Deep Link 방식
            targetUrl = String.format("%s://oauth?error=%s", deeplinkScheme, errorCode);
            log.info("OAuth2 failure - Deep Link mode, redirecting to: {}", targetUrl);
        } else if ("web".equalsIgnoreCase(mode)) {
            // Web 리다이렉트 방식
            targetUrl = UriComponentsBuilder
                    .fromUriString("https://restdawn.com")
                    .queryParam("error", errorCode)
                    .queryParam("message", errorMessage != null ? errorMessage : "Authentication failed")
                    .build()
                    .toUriString();
            log.info("OAuth2 failure - Web mode, redirecting to: restdawn.com with error", targetUrl);
        } else {
            // JSON Endpoint 방식 (기본값)
            targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("token", "")
                    .queryParam("error", errorCode)
                    .queryParam("message", errorMessage != null ? errorMessage : "Authentication failed")
                    .build().toUriString();
            log.info("OAuth2 failure - JSON mode, redirecting to: {}", targetUrl);
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
} 