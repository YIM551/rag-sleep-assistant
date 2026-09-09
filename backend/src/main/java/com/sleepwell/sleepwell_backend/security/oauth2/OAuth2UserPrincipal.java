package com.sleepwell.sleepwell_backend.security.oauth2;

import com.sleepwell.sleepwell_backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * OAuth2 사용자 주체 클래스
 * 
 * OAuth2 인증된 사용자 정보를 감싸는 주체 클래스입니다.
 * Spring Security의 OAuth2User 인터페이스를 구현하여
 * OAuth2 인증 플로우와 통합됩니다.
 * 
 * 주요 기능:
 * - User 엔티티와 OAuth2 속성을 함께 관리
 * - Spring Security 인증 시스템과의 호환성 제공
 * - JWT 토큰 생성을 위한 사용자 정보 제공
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public class OAuth2UserPrincipal implements OAuth2User {
    
    private final User user;
    private final Map<String, Object> attributes;
    
    /**
     * OAuth2UserPrincipal 생성
     * 
     * @param user 사용자 엔티티
     * @param attributes OAuth2 제공자로부터 받은 원본 속성
     * @return OAuth2UserPrincipal 인스턴스
     */
    public static OAuth2UserPrincipal create(User user, Map<String, Object> attributes) {
        return new OAuth2UserPrincipal(user, attributes);
    }
    
    @Override
    public String getName() {
        // OAuth2User 인터페이스의 getName()은 주체의 이름을 반환
        // 여기서는 사용자의 이메일을 사용
        return user.getEmail();
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // User 엔티티가 UserDetails를 구현하므로 권한 정보를 가져옴
        return user.getAuthorities();
    }
    
    /**
     * 사용자 ID 반환
     * JWT 토큰 생성 시 사용
     * 
     * @return 사용자 ID
     */
    public Long getUserId() {
        return user.getId();
    }
    
    /**
     * 사용자 이메일 반환
     * JWT 토큰 생성 시 사용
     * 
     * @return 사용자 이메일
     */
    public String getEmail() {
        return user.getEmail();
    }
    
    /**
     * 사용자 역할 반환
     * JWT 토큰 생성 시 사용
     * 
     * @return 사용자 역할
     */
    public String getRole() {
        return user.getRole().name();
    }
} 