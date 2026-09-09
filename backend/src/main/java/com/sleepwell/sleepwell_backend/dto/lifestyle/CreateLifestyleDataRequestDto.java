package com.sleepwell.sleepwell_backend.dto.lifestyle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CreateLifestyleDataRequestDto {

    @NotNull
    private LocalDate recordDate;

    // 신체 활동
    @Size(max = 50)
    private String exerciseType;

    @Size(max = 20)
    private String exerciseIntensity;

    private Integer exerciseDurationMinutes;
    private LocalDateTime exerciseStartTime;

    // 식음료 섭취
    @Min(0)
    private Integer caffeineIntake;

    private LocalDateTime lastCaffeineTime;

    @Min(0)
    private Integer alcoholIntake;

    private LocalDateTime lastAlcoholTime;

    // 심리 상태
    @Min(1)
    @Max(10)
    private Integer stressLevel;

    // 디지털 활동
    @Min(0)
    private Integer screenTimeMinutes;

    @Min(0)
    private Integer preBedrimeScreenTime;
} 