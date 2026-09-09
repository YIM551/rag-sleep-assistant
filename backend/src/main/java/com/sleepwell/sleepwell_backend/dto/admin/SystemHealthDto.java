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
public class SystemHealthDto {
    private LocalDateTime timestamp;
    private String overallStatus; // GREEN, YELLOW, RED
    
    // 시스템 리소스
    private ResourceStatus resourceStatus;
    
    // 데이터베이스 상태
    private DatabaseStatus databaseStatus;
    
    // API 상태
    private ApiStatus apiStatus;
    
    // 외부 서비스 상태
    private Map<String, ServiceStatus> externalServices;
    
    // 최근 에러
    private List<RecentError> recentErrors;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceStatus {
        private Double cpuUsage;
        private Double memoryUsage;
        private Double diskUsage;
        private Integer activeThreads;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseStatus {
        private String status;
        private Integer activeConnections;
        private Integer maxConnections;
        private Double queryResponseTime;
        private Long slowQueryCount;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiStatus {
        private String status;
        private Double averageResponseTime;
        private Long requestsPerMinute;
        private Double errorRate;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceStatus {
        private String serviceName;
        private String status;
        private LocalDateTime lastCheckTime;
        private String message;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentError {
        private LocalDateTime timestamp;
        private String errorType;
        private String message;
        private Integer count;
    }
}