package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.WearableSource;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Flutter Health 패키지에서 전송하는 수면 데이터 형식에 맞는 DTO
 * iOS HealthKit과 Android Health Connect 데이터를 모두 지원
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlutterHealthSleepDataDto {

    @NotNull(message = "수면 세션 시작 시간은 필수입니다")
    private LocalDateTime sessionStartTime;

    @NotNull(message = "수면 세션 종료 시간은 필수입니다")
    private LocalDateTime sessionEndTime;

    // === Flutter Health 핵심 수면 데이터 ===
    
    /**
     * SLEEP_ASLEEP - 실제 잠든 시간 (분)
     */
    @NotNull(message = "실제 수면 시간은 필수입니다")
    @Min(value = 1, message = "실제 수면 시간은 1분 이상이어야 합니다")
    @Max(value = 1440, message = "실제 수면 시간은 24시간을 초과할 수 없습니다")
    private Integer sleepAsleepMinutes;

    /**
     * SLEEP_IN_BED - 침대에 있던 총 시간 (분)
     */
    @Min(value = 1, message = "침대에 있던 시간은 1분 이상이어야 합니다")
    @Max(value = 1440, message = "침대에 있던 시간은 24시간을 초과할 수 없습니다")
    private Integer sleepInBedMinutes;

    /**
     * SLEEP_AWAKE - 수면 중 깨어있던 시간 (분)
     */
    @Min(value = 0, message = "수면 중 깨어있던 시간은 0분 이상이어야 합니다")
    private Integer sleepAwakeMinutes;

    /**
     * SLEEP_DEEP - 깊은 수면 시간 (분)
     */
    @Min(value = 0, message = "깊은 수면 시간은 0분 이상이어야 합니다")
    private Integer sleepDeepMinutes;

    /**
     * SLEEP_LIGHT - 얕은 수면 시간 (분)
     */
    @Min(value = 0, message = "얕은 수면 시간은 0분 이상이어야 합니다")
    private Integer sleepLightMinutes;

    /**
     * SLEEP_REM - REM 수면 시간 (분)
     */
    @Min(value = 0, message = "REM 수면 시간은 0분 이상이어야 합니다")
    private Integer sleepRemMinutes;

    // === 추가 건강 데이터 (선택사항) ===

    /**
     * HEART_RATE - 수면 중 심박수 데이터
     */
    private List<HealthDataPoint> heartRateData;

    /**
     * RESPIRATORY_RATE - 수면 중 호흡수 데이터
     */
    private List<HealthDataPoint> respiratoryRateData;

    /**
     * BODY_TEMPERATURE - 체온 데이터
     */
    private List<HealthDataPoint> bodyTemperatureData;

    // === 메타데이터 ===

    /**
     * 데이터 소스 (iOS HealthKit / Android Health Connect)
     */
    @NotNull(message = "데이터 소스는 필수입니다")
    private WearableSource dataSource;

    /**
     * 웨어러블 기기 정보 (Apple Watch, Galaxy Watch 등)
     */
    private String deviceInfo;

    /**
     * 데이터 동기화 시간
     */
    private LocalDateTime syncTime;

    /**
     * 건강 데이터 포인트 클래스
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthDataPoint {
        private LocalDateTime timestamp;
        private Double value;
        private String unit;
    }

    /**
     * 기존 SleepRecordRequestDto로 변환하는 메서드
     */
    public SleepRecordRequestDto toSleepRecordRequestDto() {
        return SleepRecordRequestDto.builder()
                .sleepStartTime(sessionStartTime)
                .sleepEndTime(sessionEndTime)
                .totalSleepMinutes(sleepAsleepMinutes)
                .sleepInBedMinutes(sleepInBedMinutes)
                .sleepAwakeMinutes(sleepAwakeMinutes)
                .deepSleepMinutes(sleepDeepMinutes)
                .lightSleepMinutes(sleepLightMinutes)
                .remSleepMinutes(sleepRemMinutes)
                .wakeupCount(calculateWakeupCount())
                .heartRateData(convertHealthDataToJson(heartRateData))
                .respiratoryRateData(convertHealthDataToJson(respiratoryRateData))
                .temperature(calculateAverageTemperature())
                .wearableSource(dataSource)
                .build();
    }

    /**
     * 깨어난 횟수 계산 (awake time 기반 추정)
     */
    private Integer calculateWakeupCount() {
        if (sleepAwakeMinutes == null || sleepAwakeMinutes == 0) {
            return 0;
        }
        // 5분 이상의 각성을 한 번의 깨어남으로 계산
        return Math.max(1, sleepAwakeMinutes / 5);
    }

    /**
     * 건강 데이터 리스트를 JSON 문자열로 변환
     */
    private String convertHealthDataToJson(List<HealthDataPoint> dataPoints) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return null;
        }
        
        // 간단한 JSON 형태로 변환 (실제로는 ObjectMapper 사용 권장)
        StringBuilder json = new StringBuilder("{");
        
        // 평균값 계산
        double average = dataPoints.stream()
                .mapToDouble(HealthDataPoint::getValue)
                .average()
                .orElse(0.0);
        
        // 최소/최대값
        double min = dataPoints.stream()
                .mapToDouble(HealthDataPoint::getValue)
                .min()
                .orElse(0.0);
        
        double max = dataPoints.stream()
                .mapToDouble(HealthDataPoint::getValue)
                .max()
                .orElse(0.0);
        
        json.append("\"average\":").append(average).append(",");
        json.append("\"min\":").append(min).append(",");
        json.append("\"max\":").append(max).append(",");
        json.append("\"count\":").append(dataPoints.size());
        json.append("}");
        
        return json.toString();
    }

    /**
     * 평균 체온 계산
     */
    private Double calculateAverageTemperature() {
        if (bodyTemperatureData == null || bodyTemperatureData.isEmpty()) {
            return null;
        }
        
        OptionalDouble average = bodyTemperatureData.stream()
                .mapToDouble(HealthDataPoint::getValue)
                .average();
        
        return average.isPresent() ? average.getAsDouble() : null;
    }
} 