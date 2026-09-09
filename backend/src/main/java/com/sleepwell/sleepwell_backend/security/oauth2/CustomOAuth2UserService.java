package com.sleepwell.sleepwell_backend.security.oauth2;

import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.SocialProvider;
import com.sleepwell.sleepwell_backend.enums.UserRole;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 커스텀 OAuth2 사용자 서비스
 * 
 * Spring Security OAuth2의 DefaultOAuth2UserService를 확장하여
 * OAuth2 인증 후 사용자 정보를 처리하는 서비스입니다.
 * 
 * 주요 기능:
 * - OAuth2 제공자별 사용자 정보 파싱
 * - 기존 사용자 연결 또는 신규 사용자 생성
 * - 소셜 계정 연결 및 사용자 정보 업데이트
 * 
 * 지원하는 OAuth2 제공자:
 * - Google
 * - Kakao (향후 추가 예정)
 * - Naver (향후 추가 예정)
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.debug("OAuth2 사용자 로드 시작: {}", userRequest.getClientRegistration().getRegistrationId());
        
        // 부모 클래스에서 OAuth2User 정보 로드
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception e) {
            log.error("OAuth2 사용자 처리 중 오류 발생", e);
            throw new OAuth2AuthenticationException("OAuth2 사용자 처리 실패: " + e.getMessage());
        }
    }
    
    /**
     * OAuth2 사용자 정보 처리
     * 
     * @param userRequest OAuth2 사용자 요청 정보
     * @param oauth2User OAuth2 사용자 정보
     * @return 처리된 OAuth2User
     */
    public OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialProvider socialProvider = getSocialProvider(registrationId);
        
        OAuth2UserInfo oauth2UserInfo = getOAuth2UserInfo(registrationId, oauth2User.getAttributes());
        
        // 필수 정보 검증
        validateOAuth2UserInfo(oauth2UserInfo);
        
        // 기존 사용자 찾기 또는 신규 생성
        User user = findOrCreateUser(socialProvider, oauth2UserInfo);
        
        // OAuth2UserPrincipal 생성 및 반환
        return OAuth2UserPrincipal.create(user, oauth2User.getAttributes());
    }
    
    /**
     * 등록 ID를 기반으로 소셜 제공자 확인
     */
    private SocialProvider getSocialProvider(String registrationId) {
        switch (registrationId.toLowerCase()) {
            case "google":
                return SocialProvider.GOOGLE;
            case "kakao":
                return SocialProvider.KAKAO;
            case "naver":
                return SocialProvider.NAVER;
            default:
                log.error("지원하지 않는 OAuth2 제공자: {}", registrationId);
                throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자: " + registrationId);
        }
    }
    
    /**
     * OAuth2 제공자별 사용자 정보 객체 생성
     */
    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, java.util.Map<String, Object> attributes) {
        switch (registrationId.toLowerCase()) {
            case "google":
                return new GoogleOAuth2UserInfo(attributes);
            case "naver":
                return new NaverOAuth2UserInfo(attributes);
            case "kakao":
                return new KakaoOAuth2UserInfo(attributes);
            default:
                log.error("지원하지 않는 OAuth2 제공자: {}", registrationId);
                throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자: " + registrationId);
        }
    }
    
    /**
     * OAuth2 사용자 정보 검증
     */
    private void validateOAuth2UserInfo(OAuth2UserInfo oauth2UserInfo) {
        if (oauth2UserInfo.getId() == null || oauth2UserInfo.getId().trim().isEmpty()) {
            throw new OAuth2AuthenticationException("OAuth2 사용자 ID가 없습니다.");
        }
        
        if (oauth2UserInfo.getName() == null || oauth2UserInfo.getName().trim().isEmpty()) {
            throw new OAuth2AuthenticationException("OAuth2 사용자 이름이 없습니다.");
        }
        
        // 이메일은 선택사항으로 변경 (카카오 등에서 제공하지 않을 수 있음)
        log.debug("OAuth2 사용자 정보 - ID: {}, 이름: {}, 이메일: {}", 
            oauth2UserInfo.getId(), oauth2UserInfo.getName(), oauth2UserInfo.getEmail());
    }
    
    /**
     * 기존 사용자 찾기 또는 신규 사용자 생성
     */
    private User findOrCreateUser(SocialProvider socialProvider, OAuth2UserInfo oauth2UserInfo) {
        // 1. 소셜 제공자와 소셜 ID로 기존 사용자 찾기
        Optional<User> existingUser = userRepository.findBySocialProviderAndSocialId(
            socialProvider, oauth2UserInfo.getId());
        
        if (existingUser.isPresent()) {
            log.debug("기존 소셜 사용자 발견: {}", oauth2UserInfo.getEmail());
            User user = existingUser.get();
            updateExistingUser(user, oauth2UserInfo);
            return user;
        }
        
        // 2. 이메일로 기존 일반 회원가입 사용자 찾기 (이메일이 있는 경우에만)
        if (oauth2UserInfo.getEmail() != null && !oauth2UserInfo.getEmail().trim().isEmpty()) {
            Optional<User> emailUser = userRepository.findByEmail(oauth2UserInfo.getEmail());
            
            if (emailUser.isPresent()) {
                log.debug("기존 이메일 사용자에 소셜 계정 연결: {}", oauth2UserInfo.getEmail());
                User user = emailUser.get();
                linkSocialAccount(user, socialProvider, oauth2UserInfo);
                return user;
            }
        }
        
        // 3. 신규 사용자 생성
        log.debug("신규 소셜 사용자 생성: {} (이메일: {})", oauth2UserInfo.getName(), oauth2UserInfo.getEmail());
        return createNewUser(socialProvider, oauth2UserInfo);
    }
    
    /**
     * 기존 사용자 정보 업데이트
     */
    private void updateExistingUser(User user, OAuth2UserInfo oauth2UserInfo) {
        boolean needUpdate = false;
        
        // 이름 업데이트 (변경된 경우)
        if (!oauth2UserInfo.getName().equals(user.getName())) {
            log.debug("사용자 이름 업데이트: {} -> {}", user.getName(), oauth2UserInfo.getName());
            needUpdate = true;
        }
        
        // 프로필 이미지 업데이트 (변경된 경우)
        if (oauth2UserInfo.getImageUrl() != null && 
            !oauth2UserInfo.getImageUrl().equals(user.getProfileImageUrl())) {
            log.debug("프로필 이미지 업데이트: {}", oauth2UserInfo.getImageUrl());
            needUpdate = true;
        }
        
        if (needUpdate) {
            User updatedUser = user.toBuilder()
                .name(oauth2UserInfo.getName())
                .profileImageUrl(oauth2UserInfo.getImageUrl())
                .lastLoginAt(LocalDateTime.now())
                .build();
            userRepository.save(updatedUser);
        } else {
            // 마지막 로그인 시간만 업데이트
            user.updateLastLoginAt();
            userRepository.save(user);
        }
    }
    
    /**
     * 기존 사용자에 소셜 계정 연결
     */
    private void linkSocialAccount(User user, SocialProvider socialProvider, OAuth2UserInfo oauth2UserInfo) {
        User linkedUser = user.toBuilder()
            .socialProvider(socialProvider)
            .socialId(oauth2UserInfo.getId())
            .profileImageUrl(oauth2UserInfo.getImageUrl())
            .lastLoginAt(LocalDateTime.now())
            .build();
        
        userRepository.save(linkedUser);
        log.info("소셜 계정 연결 완료 - 사용자: {}, 제공자: {}", user.getEmail(), socialProvider);
    }
    
    /**
     * 신규 사용자 생성
     */
    private User createNewUser(SocialProvider socialProvider, OAuth2UserInfo oauth2UserInfo) {
        // 이메일이 없는 경우 소셜 제공자와 ID를 조합한 대체 이메일 생성
        String email = oauth2UserInfo.getEmail();
        if (email == null || email.trim().isEmpty()) {
            email = String.format("%s_%s@%s.social", 
                socialProvider.name().toLowerCase(), 
                oauth2UserInfo.getId(), 
                socialProvider.name().toLowerCase());
            log.debug("이메일이 없어 대체 이메일 생성: {}", email);
        }
        
        User newUser = User.builder()
            .email(email)
            .name(oauth2UserInfo.getName())
            .socialProvider(socialProvider)
            .socialId(oauth2UserInfo.getId())
            .profileImageUrl(oauth2UserInfo.getImageUrl())
            .role(UserRole.USER)
            .isActive(true)
            .marketingConsent(false)
            .lastLoginAt(LocalDateTime.now())
            .build();
        
        User savedUser = userRepository.save(newUser);
        log.info("신규 소셜 사용자 생성 완료 - 이메일: {}, 제공자: {}", 
            email, socialProvider);
        
        return savedUser;
    }
} 