package com.sleepwell.sleepwell_backend.config.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * Host 헤더 검증 필터
 *
 * 배경:
 * - 2025-10-11~13 기간 발생한 IllegalArgumentException: "The host [_] is not valid" 오류 해결
 * - 새벽 시간대(03:20, 04:28, 00:20)에 간헐적으로 발생한 3건의 오류
 * - 잘못된 Host 헤더(_)를 가진 요청으로 인한 로그 노이즈 제거
 *
 * 목적:
 * - 허용된 Host 헤더만 통과시켜 보안 강화
 * - 악의적인 요청이나 잘못 설정된 헬스체크 차단
 * - 로그 노이즈 감소
 *
 * 허용 대상:
 * - 43.202.140.2 (프로덕션 서버 IP)
 * - api.sleepwell.com (도메인 설정 시)
 * - localhost, 127.0.0.1 (로컬 개발)
 *
 * 차단 대상:
 * - '_' (Nginx 기본 서버명)
 * - null 또는 빈 문자열
 * - 허용 목록에 없는 Host
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
public class HostHeaderValidationFilter implements Filter {

    /**
     * 허용된 Host 헤더 목록
     * 포트 번호는 자동으로 제거되어 비교됨
     */
    private static final List<String> ALLOWED_HOSTS = List.of(
        "43.202.140.2",         // 프로덕션 서버 IP
        "api.restdawn.com",     // 새 도메인 (2025-10-21)
        "restdawn.com",         // 메인 도메인
        "www.restdawn.com",     // WWW 도메인
        "api.sleepwell.com",    // 이전 도메인 (호환성)
        "sleepwell.com",        // 이전 메인 도메인
        "localhost",            // 로컬 개발
        "127.0.0.1",            // 로컬 루프백
        "sleepwell-backend-prod" // Docker 내부 네트워크 (Prometheus용)
    );

    /**
     * 잘못된 Host 헤더 패턴 (명시적 차단)
     */
    private static final List<String> BLOCKED_HOSTS = List.of(
        "_",                    // Nginx 기본 서버명
        "unknown"               // 잘못된 설정
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String host = httpRequest.getHeader("Host");

        // Host 헤더 검증
        if (!isValidHost(host)) {
            log.warn("Invalid Host header rejected: {} from IP: {} (URI: {})",
                    host,
                    httpRequest.getRemoteAddr(),
                    httpRequest.getRequestURI());

            httpResponse.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid Host header"
            );
            return;
        }

        // 정상 요청 통과
        chain.doFilter(request, response);
    }

    /**
     * Host 헤더 유효성 검증
     *
     * @param host Host 헤더 값
     * @return 유효하면 true, 그렇지 않으면 false
     */
    private boolean isValidHost(String host) {
        // null 또는 빈 문자열
        if (host == null || host.trim().isEmpty()) {
            return false;
        }

        // 명시적 차단 목록 확인
        if (BLOCKED_HOSTS.contains(host)) {
            return false;
        }

        // 포트 번호 제거 (예: localhost:8080 → localhost)
        String hostWithoutPort = host.split(":")[0].trim();

        // 차단 목록 재확인 (포트 제거 후)
        if (BLOCKED_HOSTS.contains(hostWithoutPort)) {
            return false;
        }

        // 허용 목록 확인
        return ALLOWED_HOSTS.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(hostWithoutPort));
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("HostHeaderValidationFilter initialized - Allowed hosts: {}", ALLOWED_HOSTS);
        log.info("HostHeaderValidationFilter - Blocked hosts: {}", BLOCKED_HOSTS);
    }

    @Override
    public void destroy() {
        log.info("HostHeaderValidationFilter destroyed");
    }
}
