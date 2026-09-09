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
public class FeatureUsageDto {
    // 기능별 사용 횟수
    private Map<String, FeatureMetric> featureMetrics;
    
    // 가장 많이 사용하는 기능 Top 10
    private List<FeatureRanking> topFeatures;
    
    // 가장 적게 사용하는 기능
    private List<FeatureRanking> leastUsedFeatures;
    
    // 시간대별 사용 패턴
    private Map<Integer, Map<String, Integer>> hourlyUsagePattern;
    
    // 사용자 세그먼트별 기능 사용
    private Map<String, Map<String, Integer>> usageByUserSegment;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureMetric {
        private String featureName;
        private Long totalUsageCount;
        private Long uniqueUserCount;
        private Double averageUsagePerUser;
        private String trend; // UP, DOWN, STABLE
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureRanking {
        private Integer rank;
        private String featureName;
        private Long usageCount;
        private Double percentage;
    }
}