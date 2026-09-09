package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.UserRole;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDto {

    private Long id;
    private String email;
    private String name;
    private String phoneNumber;
    private Integer age;
    private String gender;
    private String occupation;
    private UserRole role;
    private Boolean isActive;
    private Boolean marketingConsent;
    private String socialProvider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * User 엔티티로부터 UserProfileResponseDto를 생성하는 정적 메서드
     */
    public static UserProfileResponseDto from(com.sleepwell.sleepwell_backend.entity.User user) {
        return UserProfileResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .age(user.getAge())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .occupation(user.getOccupation())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .marketingConsent(user.getMarketingConsent())
                .socialProvider(user.getSocialProvider() != null ? user.getSocialProvider().name() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
} 