package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.WearableSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;

/**
 * 수면 기록 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SleepRecordResponseDto {

    private Long id;
    private Long userId;
    private LocalDateTime sleepStartTime;
    private LocalDateTime sleepEndTime;
    private Integer totalSleepMinutes;
    private Integer deepSleepMinutes;
    private Integer lightSleepMinutes;
    private Integer remSleepMinutes;
    private Integer wakeupCount;
    private Integer sleepInBedMinutes;
    private Integer sleepAwakeMinutes;
    private Double lightLevel;
    private Double noiseLevel;
    private Double temperature;
    private Double humidity;
    private Boolean snoreDetected;
    private Boolean bruxismDetected;
    private Boolean sleepTalkDetected;
    private String audioDataPath;
    private String heartRateData;
    private String respiratoryRateData;
    private String spo2Data;
    private WearableSource wearableSource;
    private Integer sleepQualityScore;
    private Integer userSatisfaction;
    private LocalDate recordDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 계산된 값들 (Flutter Health 데이터 기반)
    private Double sleepEfficiency;
    private Double deepSleepRatio;
    private Double lightSleepRatio;
    private Double remSleepRatio;

    public static SleepRecordResponseDto fromEntity(SleepRecord sleepRecord) {
        return SleepRecordResponseDto.builder()
                .id(sleepRecord.getId())
                .userId(sleepRecord.getUser().getId())
                .sleepStartTime(sleepRecord.getSleepStartTime())
                .sleepEndTime(sleepRecord.getSleepEndTime())
                .totalSleepMinutes(sleepRecord.getTotalSleepMinutes())
                .deepSleepMinutes(sleepRecord.getDeepSleepMinutes())
                .lightSleepMinutes(sleepRecord.getLightSleepMinutes())
                .remSleepMinutes(sleepRecord.getRemSleepMinutes())
                .sleepInBedMinutes(sleepRecord.getSleepInBedMinutes())
                .sleepAwakeMinutes(sleepRecord.getSleepAwakeMinutes())
                .wakeupCount(sleepRecord.getWakeupCount())
                .temperature(sleepRecord.getTemperature())
                .humidity(sleepRecord.getHumidity())
                .lightLevel(sleepRecord.getLightLevel())
                .noiseLevel(sleepRecord.getNoiseLevel())
                .snoreDetected(sleepRecord.isSnoreDetected())
                .bruxismDetected(sleepRecord.isBruxismDetected())
                .sleepTalkDetected(sleepRecord.isSleepTalkDetected())
                .heartRateData(sleepRecord.getHeartRateData())
                .respiratoryRateData(sleepRecord.getRespiratoryRateData())
                .spo2Data(sleepRecord.getSpo2Data())
                .wearableSource(sleepRecord.getWearableSource())
                .sleepQualityScore(sleepRecord.getSleepQualityScore())
                .userSatisfaction(sleepRecord.getUserSatisfaction())
                .recordDate(sleepRecord.getRecordDate())
                .createdAt(sleepRecord.getCreatedAt())
                .updatedAt(sleepRecord.getUpdatedAt())
                .sleepEfficiency(sleepRecord.calculateSleepEfficiency())
                .deepSleepRatio(sleepRecord.calculateDeepSleepRatio())
                .lightSleepRatio(sleepRecord.calculateLightSleepRatio())
                .remSleepRatio(sleepRecord.calculateRemSleepRatio())
                .build();
    }
} 