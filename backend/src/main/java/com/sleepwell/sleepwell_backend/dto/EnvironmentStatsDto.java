package com.sleepwell.sleepwell_backend.dto;

import lombok.Getter;

@Getter
public class EnvironmentStatsDto {

    private final Double avgLightLevel;
    private final Double avgNoiseLevel;
    private final Double avgTemperature;
    private final Double avgHumidity;

    public EnvironmentStatsDto(Double avgLightLevel, Double avgNoiseLevel, Double avgTemperature, Double avgHumidity) {
        this.avgLightLevel = avgLightLevel;
        this.avgNoiseLevel = avgNoiseLevel;
        this.avgTemperature = avgTemperature;
        this.avgHumidity = avgHumidity;
    }
} 