package com.sleepwell.sleepwell_backend.dto.lifestyle;

import com.sleepwell.sleepwell_backend.entity.LifestyleData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class LifestyleDataResponseDto {

    private Long id;
    private LocalDate recordDate;
    private String exerciseType;
    private String exerciseIntensity;
    private Integer exerciseDurationMinutes;
    private LocalDateTime exerciseStartTime;
    private Integer caffeineIntake;
    private LocalDateTime lastCaffeineTime;
    private Integer alcoholIntake;
    private LocalDateTime lastAlcoholTime;
    private Integer stressLevel;
    private Integer screenTimeMinutes;
    private Integer preBedrimeScreenTime;

    public static LifestyleDataResponseDto from(LifestyleData entity) {
        return new LifestyleDataResponseDto(
                entity.getId(),
                entity.getRecordDate(),
                entity.getExerciseType(),
                entity.getExerciseIntensity(),
                entity.getExerciseDurationMinutes(),
                entity.getExerciseStartTime(),
                entity.getCaffeineIntake(),
                entity.getLastCaffeineTime(),
                entity.getAlcoholIntake(),
                entity.getLastAlcoholTime(),
                entity.getStressLevel(),
                entity.getScreenTimeMinutes(),
                entity.getPreBedrimeScreenTime()
        );
    }
} 