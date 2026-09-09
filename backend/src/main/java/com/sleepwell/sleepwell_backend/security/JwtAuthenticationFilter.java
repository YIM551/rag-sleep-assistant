package com.sleepwell.sleepwell_backend.security;

import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.service.CustomUserDetailsService;
import com.sleepwell.sleepwell_backend.service.DailyActiveUserTracker;
import com.sleepwell.sleepwell_backend.util.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * 
 * 모든 HTTP 요청을 가로채서 JWT 토큰을 검증하고 
 * Spring Security Context에 인증 정보를 설정합니다.
 * 
 * 처리 흐름:
 * 1. Authorization 헤더에서 JWT 토큰 추출
 * 2. 토큰 유효성 검증
 * 3. 토큰에서 사용자 이메일 추출
 * 4. UserDetailsService로 사용자 정보 로드
 * 5. Spring Security Context에 인증 정보 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final DailyActiveUserTracker dailyActiveUserTracker;

    /**
     * 필터 실행 메서드
     * 모든 HTTP 요청에 대해 JWT 토큰을 검증합니다.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. Authorization 헤더에서 JWT 토큰 추출
            String token = getTokenFromRequest(request);
            
            if (token != null) {
                log.debug("JWT 토큰 발견: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
                
                // 2. 토큰 유효성 검증
                if (jwtTokenProvider.validateToken(token)) {
                    // 3. 토큰에서 사용자 이메일 추출
                    String email = jwtTokenProvider.getEmailFromToken(token);
                    
                    if (email != null) {
                        log.debug("토큰에서 이메일 추출: {}", email);
                        
                        // 4. Spring Security Context에 인증 정보가 없는 경우에만 처리
                        if (SecurityContextHolder.getContext().getAuthentication() == null) {
                            try {
                                // 5. UserDetailsService로 사용자 정보 로드
                                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                                
                                // 6. 인증 토큰 생성
                                UsernamePasswordAuthenticationToken authToken = 
                                    new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null, // credentials는 null (JWT 자체가 인증 수단)
                                        userDetails.getAuthorities()
                                    );
                                
                                // 7. 요청 상세 정보 설정
                                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                
                                // 8. Spring Security Context에 인증 정보 설정
                                SecurityContextHolder.getContext().setAuthentication(authToken);

                                log.debug("JWT 인증 성공: {} (권한: {})",
                                    email, userDetails.getAuthorities());

                                // 9. ✅ DAU (일일 활성 사용자) 추적
                                // JWT 자동 인증 포함 모든 접속을 추적 (하루 1번만 카운트)
                                try {
                                    if (userDetails instanceof CustomUserDetails) {
                                        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
                                        User user = customUserDetails.getUser();
                                        dailyActiveUserTracker.trackIfFirstToday(user.getId(), user.getEmail());
                                    }
                                } catch (Exception ex) {
                                    log.warn("DAU 추적 실패 (메인 인증 로직은 정상): {}", ex.getMessage());
                                }

                            } catch (Exception e) {
                                log.warn("사용자 정보 로드 실패: {}", e.getMessage());
                                SecurityContextHolder.clearContext();
                            }
                        }
                    } else {
                        log.warn("토큰에서 이메일 추출 실패");
                    }
                } else {
                    log.warn("유효하지 않은 JWT 토큰");
                }
            }
            
        } catch (Exception e) {
            log.error("JWT 인증 필터에서 오류 발생: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰을 추출합니다.
     * 
     * @param request HTTP 요청
     * @return JWT 토큰 (Bearer 접두사 제거된 순수 토큰) 또는 null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 접두사 제거
        }
        
        return null;
    }

    /**
     * 특정 경로에 대해 필터를 적용하지 않을지 결정합니다.
     * 현재는 모든 요청에 대해 필터를 적용합니다.
     * 
     * @param request HTTP 요청
     * @return false (모든 요청에 필터 적용)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // 인증이 필요 없는 경로들 (예: 로그인, 회원가입, 공개 API)
        // /api/auth/me는 인증이 필요하므로 제외
        return (path.startsWith("/api/auth/") && !path.equals("/api/auth/me")) ||
               path.startsWith("/h2-console") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/favicon.ico");
    }
} 