package com.sleepwell.sleepwell_backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto {
    
    // 기간 정보
    private LocalDate startDate;
    private LocalDate endDate;
    
    // 사용자 통계
    private Long totalUsers;
    private Long newUsers;
    private Long activeUsers;
    private Long premiumUsers;
    private Double userGrowthRate;
    
    // 수면 데이터 통계
    private Long totalSleepRecords;
    private Long todaySleepRecords;
    private Double averageSleepQuality;
    private Double averageSleepDuration;
    
    // AI 상담 통계
    private Long totalConsultations;
    private Long todayConsultations;
    private Double averageConsultationDuration;
    private Map<String, Integer> consultationsByTopic;
    
    // 플랫폼 동기화 통계
    private Map<String, Long> syncByPlatform;
    private Long totalSyncCount;
    private Long failedSyncCount;
    
    // 결제/구독 통계
    private Double totalRevenue;
    private Double monthlyRevenue;
    private Long activeSubscriptions;
    private Long trialUsers;
    
    // 시스템 상태
    private String systemStatus;
    private Double apiResponseTime;
    private Long errorCount;
    private Double serverUptime;
    
    // 트렌드 데이터 (최근 7일)
    private List<DailyMetric> dailyMetrics;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetric {
        private LocalDate date;
        private Long newUsers;
        private Long activeUsers;
        private Long sleepRecords;
        private Long consultations;
        private Double revenue;
    }
}