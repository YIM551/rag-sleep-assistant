package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.Gender;
import com.sleepwell.sleepwell_backend.enums.SocialProvider;
import com.sleepwell.sleepwell_backend.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 사용자 엔티티 (User Entity)
 * 
 * SleepWell 플랫폼의 핵심 사용자 정보를 관리하는 JPA 엔티티입니다.
 * 일반 로그인과 소셜 로그인을 모두 지원하며, Spring Security의 
 * UserDetails 인터페이스를 구현하여 인증 시스템과 완전히 통합됩니다.
 * 
 * 주요 기능:
 * - 일반 회원가입/로그인 (이메일 + 비밀번호)
 * - 소셜 로그인 (Google, Kakao, Naver)
 * - 사용자 프로필 관리 (이름, 나이, 성별 등)
 * - 계정 상태 관리 (활성화/비활성화, 소프트 삭제)
 * - 마케팅 수신 동의 관리
 * 
 * 연관 관계:
 * - 수면 기록 (SleepRecord) - 1:N
 * - 수면 분석 (SleepAnalysis) - 1:N  
 * - 구독 정보 (Subscription) - 1:N
 * - 알림 (Notification) - 1:N
 * - 상담 세션 (ConsultationSession) - 1:N
 * 
 * 인덱스 최적화:
 * - 소셜 로그인 조회 최적화
 * - 활성 사용자 조회 및 페이징 최적화
 * - 마케팅 대상자 조회 최적화
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see UserDetails
 * @see BaseTimeEntity
 * @see SleepRecord
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "users", indexes = {
    // 소셜 로그인 조회 (가장 빈번한 로그인 패턴)
    @Index(name = "IDX_USER_SOCIAL_LOGIN", columnList = "socialProvider, socialId", unique = true),
    // 활성 사용자 조회 및 페이징 (관리자, 통계 조회)
    @Index(name = "IDX_USER_ACTIVE_STATUS", columnList = "isActive, createdAt"),
    // 마케팅 수신 동의자 조회 (마케팅 캠페인)
    @Index(name = "IDX_USER_MARKETING_ACTIVE", columnList = "marketingConsent, isActive"),
    // 비활성 사용자 정리 (lastLoginAt 기준 조회)
    @Index(name = "IDX_USER_LAST_LOGIN", columnList = "lastLoginAt, isActive"),
    // 사용자 검색 (이름 검색 최적화)
    @Index(name = "IDX_USER_NAME_SEARCH", columnList = "name, isActive")
})
public class User extends BaseTimeEntity implements UserDetails {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private String phoneNumber;
    private Integer age;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    /**
     * 직업
     */
    private String occupation;

    /**
     * 비밀번호 (일반 로그인용, 소셜 로그인 시 null)
     */
    private String password;

    /**
     * 사용자 권한
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    /**
     * 소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER)
     * 일반 로그인 시 null
     */
    @Enumerated(EnumType.STRING)
    private SocialProvider socialProvider;

    /**
     * 소셜 로그인 제공자에서의 고유 ID
     * 일반 로그인 시 null
     */
    private String socialId;

    /**
     * 프로필 이미지 URL
     */
    private String profileImageUrl;

    /**
     * 계정 활성화 상태
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * 마케팅 수신 동의
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean marketingConsent = false;

    /**
     * 마지막 로그인 시간
     */
    private LocalDateTime lastLoginAt;

    /**
     * 계정 삭제 일시 (소프트 삭제)
     */
    private LocalDateTime deletedAt;

    /**
     * 알림 수신 동의 여부
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean notificationEnabled = true;
    
    /**
     * 선호 취침 시간 (HH:mm 형식)
     */
    private java.time.LocalTime preferredBedtime;
    
    /**
     * 선호 기상 시간 (HH:mm 형식)
     */
    private java.time.LocalTime preferredWakeupTime;

    @Column(columnDefinition = "TEXT")
    private String fcmToken;

    // === 양방향 관계 매핑 ===

    /**
     * 사용자의 수면 기록들
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SleepRecord> sleepRecords = new ArrayList<>();

    /**
     * 사용자의 수면 분석들
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SleepAnalysis> sleepAnalyses = new ArrayList<>();

    /**
     * 사용자의 구독 정보들
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Subscription> subscriptions = new ArrayList<>();

    /**
     * 사용자의 알림들
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Notification> notifications = new ArrayList<>();

    /**
     * 사용자가 참여한 음성 상담 세션들
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ConsultationSession> consultationSessions = new ArrayList<>();

    /**
     * 사용자가 보낸 상담 메시지들
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ConversationMessage> conversationMessages = new ArrayList<>();

    /**
     * 사용자의 수면일지들
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SleepDiary> sleepDiaries = new ArrayList<>();

    // === 비즈니스 메서드 ===

    /**
     * 프로필 정보 업데이트 (DTO 기반)
     * null이나 빈 문자열이 아닌 필드만 업데이트
     *
     * @param dto 프로필 업데이트 정보를 담은 DTO
     */
    public void updateProfile(com.sleepwell.sleepwell_backend.dto.UserProfileRequestDto dto) {
        if (isNotBlank(dto.getName())) {
            this.name = dto.getName();
        }
        if (isNotBlank(dto.getEmail())) {
            this.email = dto.getEmail();
        }
        if (isNotBlank(dto.getPhoneNumber())) {
            this.phoneNumber = dto.getPhoneNumber();
        }
        if (dto.getAge() != null) {
            this.age = dto.getAge();
        }
        if (isNotBlank(dto.getGender())) {
            try {
                this.gender = com.sleepwell.sleepwell_backend.enums.Gender.valueOf(dto.getGender());
            } catch (IllegalArgumentException e) {
                // 잘못된 gender 값은 무시
            }
        }
        if (isNotBlank(dto.getOccupation())) {
            this.occupation = dto.getOccupation();
        }
        if (dto.getMarketingConsent() != null) {
            this.marketingConsent = dto.getMarketingConsent();
        }
    }

    /**
     * 문자열이 null이 아니고 빈 문자열도 아닌지 확인
     *
     * @param value 확인할 문자열
     * @return null이 아니고 trim 후 빈 문자열이 아니면 true
     */
    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 소셜 로그인 제공자 이름 반환
     */
    public String getProvider() {
        return this.socialProvider != null ? this.socialProvider.name() : null;
    }

    /**
     * 소셜 로그인 사용자인지 확인
     */
    public boolean isSocialUser() {
        return socialProvider != null && socialId != null;
    }

    /**
     * 일반 로그인 사용자인지 확인
     */
    public boolean isRegularUser() {
        return password != null && !password.isEmpty();
    }

    /**
     * 마지막 로그인 시간 업데이트
     */
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 계정 비활성화 (소프트 삭제)
     */
    public void deactivate() {
        this.isActive = false;
        this.deletedAt = LocalDateTime.now();
    }
    
    public void activate() {
        this.isActive = true;
        this.deletedAt = null;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     * 사용자 역할 변경 (관리자용)
     * 도메인 로직과 비즈니스 규칙을 포함한 안전한 역할 변경
     * 
     * @param newRole 새로운 사용자 역할
     * @throws IllegalArgumentException 유효하지 않은 역할이거나 권한 부족시
     */
    public void changeRole(UserRole newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("역할은 null일 수 없습니다");
        }
        
        // 비즈니스 규칙: 이미 같은 역할이면 변경하지 않음
        if (this.role == newRole) {
            return;
        }
        
        // 로깅을 위한 이전 역할 보관
        UserRole previousRole = this.role;
        this.role = newRole;
        
        // JPA Auditing이 updatedAt을 자동 처리
        
        // 로그 (실제로는 이벤트 발행이 더 좋음)
        // 예: applicationEventPublisher.publishEvent(new UserRoleChangedEvent(this, previousRole, newRole));
    }

    /**
     * 비밀번호 재설정 (관리자용)
     * 암호화된 비밀번호로 안전하게 변경
     * 
     * @param encodedPassword 이미 암호화된 새 비밀번호
     * @throws IllegalArgumentException 유효하지 않은 비밀번호
     */
    public void resetPassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("암호화된 비밀번호는 비어있을 수 없습니다");
        }
        
        // 소셜 로그인 사용자는 비밀번호 설정 불가
        if (this.isSocialUser()) {
            throw new IllegalStateException("소셜 로그인 사용자는 비밀번호를 설정할 수 없습니다");
        }
        
        this.password = encodedPassword;
        
        // JPA Auditing이 updatedAt을 자동 처리
        
        // 비밀번호 재설정 이벤트 (보안 알림 등을 위해)
        // 예: applicationEventPublisher.publishEvent(new PasswordResetEvent(this));
    }

    // === UserDetails 인터페이스 구현 ===

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    public String getPassword() {
        // 소셜 로그인 사용자의 경우 빈 문자열 반환
        return password != null ? password : "";
    }

    @Override
    public String getUsername() {
        // 이메일을 username으로 사용, null 안전성 보장
        return email != null ? email : "";
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
        return isActive;
    }
} 