package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security UserDetailsService 구현체
 * 
 * 인증 시 사용자 정보를 데이터베이스에서 로드하는 역할을 담당합니다.
 * 이메일을 username으로 사용하여 사용자를 조회합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 이메일(username)로 사용자 정보를 로드합니다.
     * Spring Security가 인증 과정에서 호출하는 메서드입니다.
     * 
     * @param username 사용자 이메일
     * @return UserDetails 객체
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("사용자 로그인 시도: {}", username);
        
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음: {}", username);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
                });

        // 계정이 비활성화되었거나 삭제된 경우
        if (!user.getIsActive() || user.getDeletedAt() != null) {
            log.warn("비활성화된 계정으로 로그인 시도: {}", username);
            throw new UsernameNotFoundException("비활성화된 계정입니다: " + username);
        }

        log.debug("사용자 로드 성공: {} (권한: {})", username, user.getRole().getAuthority());
        
        // User 엔티티를 CustomUserDetails로 변환하여 반환
        return new CustomUserDetails(user);
    }

    /**
     * 사용자 ID로 UserDetails를 로드합니다.
     * JWT 토큰 검증 시 사용됩니다.
     * 
     * @param userId 사용자 ID
     * @return UserDetails 객체
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("사용자 ID로 로드: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자 ID를 찾을 수 없음: {}", userId);
                    return new UsernameNotFoundException("사용자 ID를 찾을 수 없습니다: " + userId);
                });

        // 계정이 비활성화되었거나 삭제된 경우
        if (!user.getIsActive() || user.getDeletedAt() != null) {
            log.warn("비활성화된 계정 (ID: {})", userId);
            throw new UsernameNotFoundException("비활성화된 계정입니다: " + userId);
        }

        log.debug("사용자 ID 로드 성공: {} (이메일: {})", userId, user.getEmail());
        
        return new CustomUserDetails(user);
    }

    /**
     * 이메일로 User 엔티티를 조회합니다.
     * 서비스 레이어에서 사용자 정보가 필요할 때 사용됩니다.
     * 
     * @param email 사용자 이메일
     * @return User 엔티티
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    public User findUserByEmail(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
    }
} 