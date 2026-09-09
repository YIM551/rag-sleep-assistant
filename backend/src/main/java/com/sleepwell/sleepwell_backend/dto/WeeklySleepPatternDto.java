package com.sleepwell.sleepwell_backend.dto;

import lombok.Getter;

@Getter
public class WeeklySleepPatternDto {

    private final Integer dayOfWeek;
    private final Double avgSleepMinutes;
    private final Double avgQualityScore;

    public WeeklySleepPatternDto(Number dayOfWeek, Number avgSleepMinutes, Number avgQualityScore) {
        this.dayOfWeek = (dayOfWeek != null) ? dayOfWeek.intValue() : null;
        this.avgSleepMinutes = (avgSleepMinutes != null) ? avgSleepMinutes.doubleValue() : null;
        this.avgQualityScore = (avgQualityScore != null) ? avgQualityScore.doubleValue() : null;
    }
} 