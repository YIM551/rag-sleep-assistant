package com.sleepwell.sleepwell_backend.dto;

import lombok.Getter;

@Getter
public class DisruptionStatsDto {

    private final Long snoreCount;
    private final Long bruxismCount;
    private final Long sleepTalkCount;

    public DisruptionStatsDto(Long snoreCount, Long bruxismCount, Long sleepTalkCount) {
        this.snoreCount = snoreCount;
        this.bruxismCount = bruxismCount;
        this.sleepTalkCount = sleepTalkCount;
    }
} 