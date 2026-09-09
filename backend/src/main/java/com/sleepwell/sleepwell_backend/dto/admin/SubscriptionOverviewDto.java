package com.sleepwell.sleepwell_backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionOverviewDto {
    // 구독 현황
    private Long totalSubscribers;
    private Long activeSubscriptions;
    private Long trialUsers;
    private Long expiredSubscriptions;
    
    // 플랜별 분포
    private Map<String, Long> subscriptionsByPlan;
    
    // 수익 통계
    private Double totalRevenue;
    private Double monthlyRecurringRevenue;
    private Double averageRevenuePerUser;
    
    // 전환율
    private Double trialToPaidConversionRate;
    private Double churnRate;
    private Double retentionRate;
    
    // 최근 구독 변경
    private List<RecentSubscriptionChange> recentChanges;
    
    // 만료 예정
    private List<ExpiringSubscription> expiringSubscriptions;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentSubscriptionChange {
        private Long userId;
        private String userEmail;
        private String changeType; // NEW, UPGRADE, DOWNGRADE, CANCEL
        private String fromPlan;
        private String toPlan;
        private String changeDate;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpiringSubscription {
        private Long userId;
        private String userEmail;
        private String plan;
        private String expiryDate;
        private Integer daysUntilExpiry;
    }
}