package com.sleepwell.sleepwell_backend.dto;

import lombok.Getter;

@Getter
public class SleepPatternStatsDto {

    private final Double avgTotalSleep;
    private final Double avgDeepSleep;
    private final Double avgLightSleep;
    private final Double avgRemSleep;
    private final Double avgWakeupCount;
    private final Double avgQualityScore;

    public SleepPatternStatsDto(Double avgTotalSleep, Double avgDeepSleep, Double avgLightSleep, Double avgRemSleep, Double avgWakeupCount, Double avgQualityScore) {
        this.avgTotalSleep = avgTotalSleep;
        this.avgDeepSleep = avgDeepSleep;
        this.avgLightSleep = avgLightSleep;
        this.avgRemSleep = avgRemSleep;
        this.avgWakeupCount = avgWakeupCount;
        this.avgQualityScore = avgQualityScore;
    }
} 