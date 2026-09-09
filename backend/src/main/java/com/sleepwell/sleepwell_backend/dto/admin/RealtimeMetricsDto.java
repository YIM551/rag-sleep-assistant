package com.sleepwell.sleepwell_backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeMetricsDto {
    private LocalDateTime timestamp;
    
    // 현재 활성 사용자
    private Long currentActiveUsers;
    private Long last5MinActiveUsers;
    private Long last1HourActiveUsers;
    
    // 실시간 API 통계
    private Long currentApiCalls;
    private Double avgResponseTime;
    private Long slowQueries;
    
    // 실시간 처리 상태
    private Long pendingAnalysisJobs;
    private Long processingAnalysisJobs;
    private Long pendingNotifications;
    
    // 시스템 리소스
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    
    // 최근 활동
    private List<RecentActivity> recentActivities;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private LocalDateTime timestamp;
        private String activityType;
        private String description;
        private Long userId;
        private String userEmail;
    }
}