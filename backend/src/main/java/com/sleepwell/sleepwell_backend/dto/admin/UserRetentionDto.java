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
public class UserRetentionDto {
    private Integer periodDays;
    
    // 리텐션 통계
    private Double overallRetentionRate;
    private Map<Integer, Double> retentionByDay;
    
    // 코호트 분석
    private List<CohortAnalysis> cohorts;
    
    // 사용자 세그먼트별 리텐션
    private Map<String, Double> retentionBySegment;
    
    // 이탈 원인 분석
    private List<ChurnReason> topChurnReasons;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CohortAnalysis {
        private String cohortDate;
        private Long cohortSize;
        private Map<Integer, Double> retentionByWeek;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChurnReason {
        private String reason;
        private Integer count;
        private Double percentage;
    }
}