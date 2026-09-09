package com.sleepwell.sleepwell_backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성, 검증, 파싱을 담당하는 핵심 유틸리티 클래스
 * 
 * SleepWell 플랫폼의 인증 시스템의 핵심 구성요소로, 
 * RFC 7519 JWT 표준을 준수하여 안전한 토큰 기반 인증을 제공합니다.
 * HMAC SHA-512 알고리즘을 사용하여 토큰 무결성을 보장합니다.
 * 
 * 주요 기능:
 * - JWT Access Token 생성 및 서명
 * - 토큰 유효성 검증 및 만료 확인
 * - 토큰에서 사용자 정보 추출 (이메일, 권한)
 * - Bearer 토큰 형식 처리
 * 
 * 보안 특징:
 * - HS512 서명 알고리즘 사용 (512비트 키 필수)
 * - 토큰 만료 시간 관리
 * - 상세한 예외 처리 및 로깅
 * - 토큰 형식 및 서명 무결성 검증
 * 
 * 설정 요구사항:
 * - jwt.secret: 최소 64바이트 길이의 비밀키
 * - jwt.expiration: 토큰 만료 시간 (밀리초)
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see JwtAuthenticationFilter
 * @see CustomUserDetailsService
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration:604800000}") long refreshTokenExpiration) {
        
        // Secret Key가 충분히 긴지 확인 (HS512는 최소 512비트 = 64바이트 필요)
        if (secret.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalArgumentException("JWT secret key must be at least 64 bytes for HS512");
        }
        
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        
        log.info("JwtTokenProvider initialized - Access: {} ms, Refresh: {} ms", 
                accessTokenExpiration, refreshTokenExpiration);
    }

    /**
     * JWT Access Token 생성
     * 
     * @param email 사용자 이메일 (Subject로 사용)
     * @param role 사용자 권한
     * @return 생성된 JWT 토큰
     */
    public String createToken(String email, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .setSubject(email) // 토큰 주체 (사용자 식별자)
                .claim("role", role) // 사용자 권한
                .claim("type", "access") // 토큰 타입
                .setIssuedAt(now) // 토큰 발급 시간
                .setExpiration(expiration) // 토큰 만료 시간
                .signWith(secretKey, SignatureAlgorithm.HS512) // 서명
                .compact();
    }
    
    /**
     * JWT Refresh Token 생성
     * 
     * @param email 사용자 이메일
     * @return 생성된 Refresh Token
     */
    public String createRefreshToken(String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .setSubject(email)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * JWT 토큰에서 이메일(Subject) 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 이메일
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("토큰에서 이메일 추출 실패", e);
            return null;
        }
    }

    /**
     * JWT 토큰에서 권한 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 권한
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.error("토큰에서 권한 추출 실패", e);
            return null;
        }
    }

    /**
     * JWT 토큰 유효성 검증
     * 
     * @param token 검증할 JWT 토큰
     * @return 토큰이 유효한지 여부
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("토큰이 만료되었습니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 토큰입니다: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 토큰입니다: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("토큰 서명이 올바르지 않습니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("토큰이 비어있습니다: {}", e.getMessage());
        } catch (Exception e) {
            log.error("토큰 검증 중 오류 발생", e);
        }
        
        return false;
    }

    /**
     * JWT 토큰이 만료되었는지 확인
     * 
     * @param token JWT 토큰
     * @return 토큰이 만료되었는지 여부
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true; // 만료된 토큰
        } catch (Exception e) {
            log.error("토큰 만료 확인 중 오류 발생", e);
            return true; // 오류 발생 시 만료된 것으로 간주
        }
    }

    /**
     * JWT 토큰에서 만료 시간 추출
     * 
     * @param token JWT 토큰
     * @return 만료 시간
     */
    public Date getExpirationFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("토큰에서 만료 시간 추출 실패", e);
            return null;
        }
    }

    /**
     * JWT 토큰을 파싱하여 Claims 객체 반환
     * 
     * @param token JWT 토큰
     * @return Claims 객체
     * @throws JwtException 토큰 파싱 실패 시
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰에서 Bearer 접두사 제거
     * 
     * @param bearerToken "Bearer " 접두사가 포함된 토큰
     * @return 순수 JWT 토큰
     */
    public String resolveToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return bearerToken;
    }
} 