package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.Gender;
import com.sleepwell.sleepwell_backend.enums.UserRole;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.dto.UserProfileRequestDto;
import com.sleepwell.sleepwell_backend.dto.UserProfileResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.repository.ConversationMessageRepository;
import com.sleepwell.sleepwell_backend.repository.SleepFeedbackRepository;
import com.sleepwell.sleepwell_backend.repository.SubscriptionRepository;
import com.sleepwell.sleepwell_backend.repository.NotificationRepository;

/**
 * 사용자 관리 서비스
 * 
 * 사용자 생성, 수정, 삭제, 조회 등의 비즈니스 로직을 담당합니다.
 * 프로필 관리, 계정 상태 관리, 역할 관리 등의 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SleepRecordRepository sleepRecordRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final SleepFeedbackRepository sleepFeedbackRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;

    /**
     * 사용자 회원가입
     * 
     * @param email 이메일
     * @param password 비밀번호
     * @param name 이름
     * @return 생성된 사용자
     */
    @Transactional
    public User createUser(String email, String password, String name) {
        log.debug("새 사용자 생성 시도: {}", email);
        
        // 이메일 중복 확인
        if (userRepository.existsByEmail(email)) {
            log.warn("이미 존재하는 이메일로 회원가입 시도: {}", email);
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + email);
        }

        // 사용자 생성
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(UserRole.USER)
                .isActive(true)
                .marketingConsent(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("새 사용자 생성 완료: {} (ID: {})", email, savedUser.getId());
        
        return savedUser;
    }

    /**
     * 소셜 로그인 사용자 생성 또는 업데이트
     * 
     * @param email 이메일
     * @param name 이름
     * @param provider 소셜 로그인 제공자
     * @return 사용자
     */
    @Transactional
    public User createOrUpdateSocialUser(String email, String name, String provider) {
        log.debug("소셜 로그인 사용자 처리: {} ({})", email, provider);
        
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // 기존 사용자 정보 업데이트
            if (name != null && !name.equals(user.getName())) {
                user = user.toBuilder()
                        .name(name)
                        .build();
                user = userRepository.save(user);
                log.debug("기존 소셜 로그인 사용자 정보 업데이트: {}", email);
            }
            
            // 마지막 로그인 시간 업데이트
            user.updateLastLoginAt();
            userRepository.save(user);
            
            return user;
        } else {
            // 새 소셜 로그인 사용자 생성
            User user = User.builder()
                    .email(email)
                    .password(null) // 소셜 로그인은 비밀번호 없음
                    .name(name)
                    .role(UserRole.USER)
                    .isActive(true)
                    .marketingConsent(false)
                    .build();
            
            User savedUser = userRepository.save(user);
            log.info("새 소셜 로그인 사용자 생성: {} ({})", email, provider);
            
            return savedUser;
        }
    }

    /**
     * 사용자 프로필 업데이트
     * 
     * @param userId 사용자 ID
     * @param name 새 이름
     * @param marketingConsent 마케팅 수신 동의
     * @return 업데이트된 사용자
     */
    @Transactional
    public User updateUserProfile(Long userId, String name, Boolean marketingConsent) {
        log.debug("사용자 프로필 업데이트: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        User.UserBuilder builder = user.toBuilder();
        boolean updated = false;

        if (name != null && !name.equals(user.getName())) {
            builder.name(name);
            updated = true;
        }

        if (marketingConsent != null && !marketingConsent.equals(user.getMarketingConsent())) {
            builder.marketingConsent(marketingConsent);
            updated = true;
        }

        if (updated) {
            User updatedUser = userRepository.save(builder.build());
            log.info("사용자 프로필 업데이트 완료: {}", userId);
            return updatedUser;
        }

        return user;
    }

    /**
     * 비밀번호 변경
     * 
     * @param userId 사용자 ID
     * @param currentPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        log.debug("비밀번호 변경 요청: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 소셜 로그인 사용자는 비밀번호를 변경할 수 없음
        if (user.isSocialUser()) {
            throw new IllegalArgumentException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다");
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("비밀번호 변경 실패 - 현재 비밀번호 불일치: {}", userId);
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다");
        }

        // 새 비밀번호 설정
        User updatedUser = user.toBuilder()
                .password(passwordEncoder.encode(newPassword))
                .build();
        
        userRepository.save(updatedUser);
        log.info("비밀번호 변경 완료: {}", userId);
    }

    /**
     * 사용자 계정 비활성화
     * 
     * @param userId 사용자 ID
     */
    @Transactional
    public void deactivateUser(Long userId) {
        log.debug("사용자 계정 비활성화: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        User updatedUser = user.toBuilder()
                .isActive(false)
                .build();
        
        userRepository.save(updatedUser);
        log.info("사용자 계정 비활성화 완료: {}", userId);
    }

    /**
     * 사용자 계정 활성화
     * 
     * @param userId 사용자 ID
     */
    @Transactional
    public void activateUser(Long userId) {
        log.debug("사용자 계정 활성화: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        User updatedUser = user.toBuilder()
                .isActive(true)
                .build();
        
        userRepository.save(updatedUser);
        log.info("사용자 계정 활성화 완료: {}", userId);
    }

    /**
     * 사용자 소프트 삭제
     * 
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.debug("사용자 삭제: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        User updatedUser = user.toBuilder()
                .isActive(false)
                .deletedAt(LocalDateTime.now())
                .build();
        
        userRepository.save(updatedUser);
        log.info("사용자 삭제 완료: {}", userId);
    }

    /**
     * 사용자 역할 변경 (관리자 전용)
     * 
     * @param userId 사용자 ID
     * @param newRole 새 역할
     */
    @Transactional
    public void changeUserRole(Long userId, UserRole newRole) {
        log.debug("사용자 역할 변경: {} -> {}", userId, newRole);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        User updatedUser = user.toBuilder()
                .role(newRole)
                .build();
        
        userRepository.save(updatedUser);
        log.info("사용자 역할 변경 완료: {} -> {}", userId, newRole);
    }

    /**
     * 이메일로 사용자 조회
     * 
     * @param email 이메일
     * @return 사용자 (Optional)
     */
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * ID로 사용자 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 (Optional)
     */
    public Optional<User> findUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * 활성 사용자 조회 (관리자 전용)
     * 
     * @return 활성 사용자 목록
     */
    public List<User> findActiveUsers() {
        return userRepository.findByIsActiveTrueAndDeletedAtIsNull();
    }

    /**
     * 전체 사용자 조회 (관리자 전용)
     * 
     * @return 전체 사용자 목록
     */
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 이메일 중복 확인
     * 
     * @param email 이메일
     * @return 중복 여부
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 마지막 로그인 시간 업데이트
     * 
     * @param userId 사용자 ID
     */
    @Transactional
    public void updateLastLoginAt(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        user.updateLastLoginAt();
        userRepository.save(user);
    }

    /**
     * 사용자 통계 조회 (관리자 전용)
     * 
     * @return 사용자 통계 정보
     */
    public UserStats getUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrueAndDeletedAtIsNull();
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(
                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
        );
        
        return new UserStats(totalUsers, activeUsers, newUsersThisMonth);
    }

    /**
     * 사용자 통계 정보 클래스
     */
    public record UserStats(
            long totalUsers,
            long activeUsers,
            long newUsersThisMonth
    ) {}

    /**
     * 사용자 프로필 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 프로필 정보
     */
    public UserProfileResponseDto getUserProfile(Long userId) {
        log.debug("사용자 프로필 조회: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        return UserProfileResponseDto.from(user);
    }

    /**
     * 사용자 프로필 업데이트 (DTO 기반) - DEPRECATED
     *
     * @deprecated updateUserProfileByEmail()을 사용하세요. 이 메서드는 사용되지 않으며 향후 제거될 예정입니다.
     * @param userId 사용자 ID
     * @param request 프로필 업데이트 요청 DTO
     * @return 업데이트된 사용자 프로필 정보
     */
    @Deprecated
    @Transactional
    public UserProfileResponseDto updateUserProfile(Long userId, UserProfileRequestDto request) {
        log.debug("사용자 프로필 업데이트 (DEPRECATED): {}", userId);
        log.warn("이 메서드는 deprecated되었습니다. updateUserProfileByEmail()을 사용하세요.");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 이메일 중복 체크 (이메일이 제공되고 현재 사용자와 다를 경우)
        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail()) && existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + request.getEmail());
        }

        // User.updateProfile() 도메인 메서드 사용 (User 엔티티와 동일한 패턴 사용)
        user.updateProfile(request);

        User savedUser = userRepository.save(user);
        log.info("사용자 프로필 업데이트 완료 (DEPRECATED): {}", userId);

        return UserProfileResponseDto.from(savedUser);
    }

    public UserProfileResponseDto getUserProfileByEmail(String email) {
        log.debug("이메일로 사용자 프로필 조회: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다: " + email, HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        
        return new UserProfileResponseDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                user.getAge(),
                user.getGender() != null ? user.getGender().name() : null,
                user.getOccupation(),
                user.getRole(),
                user.getIsActive(),
                user.getMarketingConsent(),
                user.getProvider(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Transactional
    public UserProfileResponseDto updateUserProfileByEmail(String email, UserProfileRequestDto request) {
        log.debug("이메일로 사용자 프로필 업데이트: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다: " + email, HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        // 이메일 변경 시 중복 확인
        if (request.getEmail() != null && !email.equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("이미 사용 중인 이메일입니다: " + request.getEmail(), HttpStatus.BAD_REQUEST, "EMAIL_ALREADY_EXISTS");
        }

        user.updateProfile(request);

        // 변경 감지(Dirty Checking)에 의해 user 객체는 트랜잭션 종료 시 자동으로 DB에 반영됩니다.
        // 명시적으로 save를 호출할 필요는 없지만, 반환값을 사용하기 위해 호출합니다.
        User updatedUser = userRepository.save(user);

        return UserProfileResponseDto.from(updatedUser);
    }

    @Transactional
    public void changePasswordByEmail(String email, String currentPassword, String newPassword) {
        log.debug("비밀번호 변경 요청 (이메일): {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다: " + email, HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        // 소셜 로그인 사용자는 비밀번호를 변경할 수 없음
        if (user.isSocialUser()) {
            throw new BusinessException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다", HttpStatus.BAD_REQUEST, "SOCIAL_USER_PASSWORD_CHANGE_NOT_ALLOWED");
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("비밀번호 변경 실패 - 현재 비밀번호 불일치 (이메일): {}", email);
            Map<String, String> details = Map.of("currentPassword", "현재 비밀번호가 올바르지 않습니다");
            throw new BusinessException("현재 비밀번호가 올바르지 않습니다", HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", details);
        }

        // 새 비밀번호 설정
        User updatedUser = user.toBuilder()
                .password(passwordEncoder.encode(newPassword))
                .build();
        
        userRepository.save(updatedUser);
        log.info("비밀번호 변경 완료 (이메일): {}", email);
    }

    @Transactional
    public void deactivateUserByEmail(String email) {
        log.debug("이메일로 사용자 비활성화: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        User updatedUser = user.toBuilder()
                .isActive(false)
                .build();
        
        userRepository.save(updatedUser);
        log.info("사용자 계정 비활성화 완료 (이메일): {}", email);
    }

    /**
     * 사용자 계정 영구 삭제 (Hard Delete)
     * GDPR의 '잊힐 권리'를 준수하기 위한 기능.
     * 사용자와 관련된 모든 데이터를 영구적으로 삭제합니다.
     * 
     * @param userId 삭제할 사용자 ID
     */
    @Transactional
    public void deleteUserHard(Long userId) {
        log.warn("사용자 계정 영구 삭제 요청: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("영구 삭제할 사용자를 찾을 수 없습니다: " + userId, HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        // 연관된 데이터 삭제
        sleepRecordRepository.deleteAllByUser(user);
        conversationMessageRepository.deleteAllByUser(user);
        sleepFeedbackRepository.deleteAllByUser(user);
        subscriptionRepository.deleteAllByUser(user);
        notificationRepository.deleteAllByUser(user);

        // 사용자 캐시 무효화 (이메일 기반)
        // 이 작업은 마지막에 수행하여 다른 삭제 작업에 영향을 주지 않도록 함
        
        // 마지막으로 사용자 삭제
        userRepository.delete(user);

        log.info("사용자 계정 영구 삭제 완료: {} (Email: {})", userId, user.getEmail());
    }
} 