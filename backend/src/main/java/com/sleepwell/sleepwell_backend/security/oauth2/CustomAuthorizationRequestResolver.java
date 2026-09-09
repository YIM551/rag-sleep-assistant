package com.sleepwell.sleepwell_backend.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Base64;

/**
 * OAuth2 Authorization Request 커스터마이저
 *
 * mode 파라미터를 OAuth2 state에 저장하여 콜백 시점에도 유지되도록 처리합니다.
 *
 * 문제:
 * - /oauth2/authorization/google?mode=deeplink 로 시작
 * - Google 로그인 후 /login/oauth2/code/google 로 콜백
 * - 콜백 시점에 mode 파라미터가 사라짐
 *
 * 해결:
 * - mode 정보를 OAuth2 state 파라미터에 인코딩
 * - state는 OAuth2 스펙에서 클라이언트가 요청과 콜백 간 상태를 유지하기 위한 용도
 * - Google/Kakao/Naver가 state를 그대로 돌려줌
 * - SuccessHandler에서 state를 디코딩하여 mode 복원
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    /**
     * 생성자
     *
     * @param clientRegistrationRepository OAuth2 클라이언트 등록 정보 저장소
     * @param authorizationRequestBaseUri OAuth2 인증 요청 기본 URI (기본값: /oauth2/authorization)
     */
    public CustomAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository,
            String authorizationRequestBaseUri) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                authorizationRequestBaseUri
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        return customizeAuthorizationRequest(authorizationRequest, request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest =
                defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(authorizationRequest, request);
    }

    /**
     * Authorization Request 커스터마이징
     *
     * mode 파라미터를 state에 인코딩합니다.
     *
     * state 형식: "originalState|mode=deeplink" (Base64 인코딩)
     *
     * @param authorizationRequest 기본 Authorization Request
     * @param request HTTP 요청
     * @return 커스터마이징된 Authorization Request
     */
    private OAuth2AuthorizationRequest customizeAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request) {

        if (authorizationRequest == null) {
            return null;
        }

        // mode 파라미터 추출 (deeplink 또는 null)
        String mode = request.getParameter("mode");

        // mode가 없으면 기본 요청 그대로 반환
        if (mode == null || mode.isEmpty()) {
            log.debug("No mode parameter found, using default authorization request");
            return authorizationRequest;
        }

        // 원본 state 가져오기 (CSRF 방지용으로 Spring Security가 자동 생성)
        String originalState = authorizationRequest.getState();

        // mode 정보를 state에 인코딩
        // 형식: "originalState|mode=deeplink"
        String customState = originalState + "|mode=" + mode;

        // Base64 인코딩 (URL safe)
        String encodedState = Base64.getUrlEncoder().encodeToString(customState.getBytes());

        log.info("OAuth2 Authorization Request customized - mode: {}, originalState: {}, encodedState: {}",
                mode, originalState, encodedState);

        // state 파라미터를 수정한 새로운 Authorization Request 생성
        return OAuth2AuthorizationRequest
                .from(authorizationRequest)
                .state(encodedState)
                .build();
    }

    /**
     * state 파라미터에서 mode 정보 추출
     *
     * SuccessHandler에서 사용할 정적 메서드
     *
     * @param encodedState Base64 인코딩된 state
     * @return mode 값 (deeplink 또는 null)
     */
    public static String extractModeFromState(String encodedState) {
        if (encodedState == null || encodedState.isEmpty()) {
            return null;
        }

        try {
            // Base64 디코딩
            String decodedState = new String(Base64.getUrlDecoder().decode(encodedState));

            // "originalState|mode=deeplink" 형식에서 mode 추출
            if (decodedState.contains("|mode=")) {
                String[] parts = decodedState.split("\\|mode=");
                if (parts.length == 2) {
                    return parts[1];
                }
            }

            return null;
        } catch (Exception e) {
            log.warn("Failed to extract mode from state: {}", encodedState, e);
            return null;
        }
    }
}
