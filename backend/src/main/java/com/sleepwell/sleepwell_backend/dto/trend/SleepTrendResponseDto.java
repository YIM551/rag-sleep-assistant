package com.sleepwell.sleepwell_backend.dto.trend;

import com.sleepwell.sleepwell_backend.entity.SleepTrend;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class SleepTrendResponseDto {

    private Long id;
    private LocalDate trendDate;
    private String trendType;
    private Integer periodDays;
    private BigDecimal averageSleepMinutes;
    private BigDecimal averageSleepEfficiency;
    private BigDecimal averageSleepScore;
    private Integer trendScore;
    private String trendDirection;
    private Integer consistencyScore;

    public static SleepTrendResponseDto from(SleepTrend entity) {
        return new SleepTrendResponseDto(
                entity.getId(),
                entity.getTrendDate(),
                entity.getTrendType(),
                entity.getPeriodDays(),
                entity.getAverageSleepMinutes(),
                entity.getAverageSleepEfficiency(),
                entity.getAverageSleepScore(),
                entity.getTrendScore(),
                entity.getTrendDirection(),
                entity.getConsistencyScore()
        );
    }
} 