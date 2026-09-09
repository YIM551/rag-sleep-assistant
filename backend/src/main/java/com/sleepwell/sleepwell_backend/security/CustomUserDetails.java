package com.sleepwell.sleepwell_backend.security;

import com.sleepwell.sleepwell_backend.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security UserDetails 구현체
 * User 엔티티를 감싸서 Spring Security가 사용할 수 있는 형태로 변환
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    
    private final User user;

    /**
     * User ID 반환
     */
    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().getAuthority()));
    }
    
    @Override
    public String getPassword() {
        return user.getPassword() != null ? user.getPassword() : "";
    }
    
    @Override
    public String getUsername() {
        return user.getEmail();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return user.getIsActive() && user.getDeletedAt() == null;
    }
}