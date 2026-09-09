package com.sleepwell.sleepwell_backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailDto {
    // 기본 정보
    private Long id;
    private String email;
    private String name;
    private String phoneNumber;
    private Integer age;
    private String gender;
    private String occupation;
    private String role;
    private boolean isActive;
    private String statusReason;
    
    // 계정 정보
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private Integer loginCount;
    
    // 구독 정보
    private String subscriptionStatus;
    private String subscriptionPlan;
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionExpiryDate;
    private Double totalPaymentAmount;
    
    // 활동 통계
    private Long totalSleepRecords;
    private Long totalConsultations;
    private Long totalNotifications;
    private Double averageSleepQuality;
    private Double averageSleepDuration;
    
    // 플랫폼 연동
    private List<String> connectedPlatforms;
    private Map<String, LocalDateTime> lastSyncTimes;
    
    // 최근 활동
    private List<UserActivity> recentActivities;
    
    // 수면 패턴
    private SleepPattern sleepPattern;
    
    // AI 상담 이용 내역
    private ConsultationUsage consultationUsage;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivity {
        private LocalDateTime timestamp;
        private String activityType;
        private String description;
        private Map<String, Object> metadata;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SleepPattern {
        private Double averageBedtime;
        private Double averageWakeTime;
        private Double averageSleepDuration;
        private String sleepQualityTrend;
        private List<String> commonIssues;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsultationUsage {
        private Integer totalSessions;
        private Integer totalMessages;
        private List<String> frequentTopics;
        private Double averageSessionDuration;
        private Double totalTokensUsed;
    }
}