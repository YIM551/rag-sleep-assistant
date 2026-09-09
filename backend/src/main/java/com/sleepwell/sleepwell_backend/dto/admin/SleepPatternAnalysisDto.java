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
public class SleepPatternAnalysisDto {
    private LocalDate startDate;
    private LocalDate endDate;
    
    // 전체 통계
    private Double averageSleepDuration;
    private Double averageSleepQuality;
    private Double averageBedtime;
    private Double averageWakeTime;
    
    // 수면 단계별 평균
    private Double averageDeepSleepPercentage;
    private Double averageLightSleepPercentage;
    private Double averageRemSleepPercentage;
    
    // 수면 문제 통계
    private Map<String, Integer> sleepIssuesFrequency;
    
    // 요일별 패턴
    private Map<String, DayPattern> patternByDayOfWeek;
    
    // 시간대별 분포
    private Map<Integer, Integer> bedtimeDistribution;
    private Map<Integer, Integer> wakeTimeDistribution;
    
    // 수면 품질 트렌드
    private List<DailyTrend> dailyTrends;
    
    // 사용자 그룹별 분석
    private Map<String, GroupStatistics> statisticsByAgeGroup;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPattern {
        private String dayOfWeek;
        private Double averageDuration;
        private Double averageQuality;
        private Double averageBedtime;
        private Double averageWakeTime;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrend {
        private LocalDate date;
        private Double averageDuration;
        private Double averageQuality;
        private Long recordCount;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupStatistics {
        private String groupName;
        private Long userCount;
        private Double averageDuration;
        private Double averageQuality;
    }
}