package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.WearableSource;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 수면 기록 생성 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SleepRecordRequestDto {

    @NotNull(message = "수면 시작 시간은 필수입니다")
    private LocalDateTime sleepStartTime;

    @NotNull(message = "수면 종료 시간은 필수입니다")
    private LocalDateTime sleepEndTime;

    @NotNull(message = "총 수면 시간은 필수입니다")
    @Min(value = 1, message = "총 수면 시간은 1분 이상이어야 합니다")
    @Max(value = 1440, message = "총 수면 시간은 24시간을 초과할 수 없습니다")
    private Integer totalSleepMinutes;

    @Min(value = 0, message = "깊은 잠 시간은 0분 이상이어야 합니다")
    private Integer deepSleepMinutes;

    @Min(value = 0, message = "얕은 잠 시간은 0분 이상이어야 합니다")
    private Integer lightSleepMinutes;

    @Min(value = 0, message = "REM 수면 시간은 0분 이상이어야 합니다")
    private Integer remSleepMinutes;

    @Min(value = 0, message = "깨어난 횟수는 0회 이상이어야 합니다")
    private Integer wakeupCount;

    @Min(value = 1, message = "침대에 있던 시간은 1분 이상이어야 합니다")
    @Max(value = 1440, message = "침대에 있던 시간은 24시간을 초과할 수 없습니다")
    private Integer sleepInBedMinutes;

    @Min(value = 0, message = "수면 중 깨어있던 시간은 0분 이상이어야 합니다")
    private Integer sleepAwakeMinutes;

    @DecimalMin(value = "0.0", message = "조도는 0 이상이어야 합니다")
    private Double lightLevel;

    @DecimalMin(value = "0.0", message = "소음 레벨은 0 이상이어야 합니다")
    private Double noiseLevel;

    @DecimalMin(value = "-50.0", message = "온도는 -50도 이상이어야 합니다")
    @DecimalMax(value = "60.0", message = "온도는 60도 이하여야 합니다")
    private Double temperature;

    @DecimalMin(value = "0.0", message = "습도는 0% 이상이어야 합니다")
    @DecimalMax(value = "100.0", message = "습도는 100% 이하여야 합니다")
    private Double humidity;

    private Boolean snoreDetected;

    private Boolean bruxismDetected;

    private Boolean sleepTalkDetected;

    private String audioDataPath;

    private String heartRateData;

    private String respiratoryRateData;

    private WearableSource wearableSource;

    @Min(value = 1, message = "수면 품질 점수는 1점 이상이어야 합니다")
    @Max(value = 100, message = "수면 품질 점수는 100점 이하여야 합니다")
    private Integer sleepQualityScore;

    @Min(value = 1, message = "사용자 만족도는 1점 이상이어야 합니다")
    @Max(value = 5, message = "사용자 만족도는 5점 이하여야 합니다")
    private Integer userSatisfaction;

    public void validateDataIntegrity() {
        validateSleepDuration();
        validateSleepStages();
        validateEnvironmentData();
    }

    private void validateSleepDuration() {
        if (sleepStartTime == null || sleepEndTime == null) {
            throw new IllegalArgumentException("수면 시작 및 종료 시간은 필수입니다.");
        }

        if (sleepStartTime.isAfter(sleepEndTime)) {
            throw new IllegalArgumentException("수면 시작 시간은 종료 시간보다 이전이어야 합니다.");
        }

        // 생리학적 최대 수면 시간 제한 (16시간)
        if (totalSleepMinutes != null && totalSleepMinutes > 960) {
            throw new IllegalArgumentException("수면 시간이 생리학적 한계를 초과했습니다.");
        }
    }

    private void validateSleepStages() {
        // 수면 단계 시간 검증
        if (totalSleepMinutes != null) {
            int stageMinutes = 0;
            
            if (deepSleepMinutes != null) {
                if (deepSleepMinutes < 0) {
                    throw new IllegalArgumentException("수면 단계의 시간은 음수일 수 없습니다.");
                }
                stageMinutes += deepSleepMinutes;
            }
            
            if (lightSleepMinutes != null) {
                if (lightSleepMinutes < 0) {
                    throw new IllegalArgumentException("수면 단계의 시간은 음수일 수 없습니다.");
                }
                stageMinutes += lightSleepMinutes;
            }
            
            if (remSleepMinutes != null) {
                if (remSleepMinutes < 0) {
                    throw new IllegalArgumentException("수면 단계의 시간은 음수일 수 없습니다.");
                }
                stageMinutes += remSleepMinutes;
            }

            // 수면 단계의 총 시간이 전체 수면 시간을 초과하는지 검증
            if (stageMinutes > totalSleepMinutes) {
                throw new IllegalArgumentException("수면 단계의 총 시간이 전체 수면 시간을 초과했습니다.");
            }
        }
    }

    private void validateEnvironmentData() {
        // 온도 범위 검증 (섭씨 기준)
        if (temperature != null && (temperature < -10 || temperature > 40)) {
            throw new IllegalArgumentException("온도 값이 허용 범위를 벗어났습니다.");
        }

        // 습도 범위 검증 (0-100%)
        if (humidity != null && (humidity < 0 || humidity > 100)) {
            throw new IllegalArgumentException("습도 값이 허용 범위를 벗어났습니다.");
        }
    }
} 