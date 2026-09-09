package com.sleepwell.sleepwell_backend.adapter.impl;

import com.sleepwell.sleepwell_backend.adapter.PlatformSleepDataAdapter;
import com.sleepwell.sleepwell_backend.dto.*;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Android Health Connect 플랫폼 데이터 어댑터 구현체
 * 
 * Android Health Connect에서 제공하는 통합 수면 분석 데이터를
 * 통합 데이터 모델로 변환하는 어댑터입니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Component
public class HealthConnectSleepDataAdapter implements PlatformSleepDataAdapter {

    private static final String HEALTH_CONNECT_PLATFORM = "Health Connect";

    @Override
    @NonNull
    public UnifiedSleepAnalysisDto convertToUnifiedAnalysis(
            @NonNull PlatformSleepDataDto platformData, 
            @NonNull WearableSource source) {
        
        if (!supports(source)) {
            throw new IllegalArgumentException("Unsupported platform source: " + source);
        }

        log.debug("Converting Health Connect data to unified analysis");

        // 수면 단계 분석 생성
        log.debug("Converting sleep stage data - Deep: {}, Light: {}, REM: {}, Awake: {}",
                platformData.getDeepSleepMinutes(),
                platformData.getLightSleepMinutes(),
                platformData.getRemSleepMinutes(),
                platformData.getAwakeMinutes());

        UnifiedSleepAnalysisDto.SleepStageAnalysis sleepStages = UnifiedSleepAnalysisDto.SleepStageAnalysis.builder()
                .deepSleepMinutes(platformData.getDeepSleepMinutes())
                .lightSleepMinutes(platformData.getLightSleepMinutes())
                .remSleepMinutes(platformData.getRemSleepMinutes())
                .awakeMinutes(platformData.getAwakeMinutes())
                .build();

        return UnifiedSleepAnalysisDto.builder()
                .originalSource(source)
                .analyzedAt(LocalDateTime.now())
                .sleepSession(buildSleepSessionInfo(platformData))
                .sleepStages(sleepStages)  // 수면 단계 정보 추가!
                .unifiedSleepScore(calculateUnifiedScore(platformData))
                .platformScore(platformData.getPlatformSleepScore())
                .reliabilityScore(calculateReliabilityScore(platformData))
                .dataPointsUsed(countDataPoints(platformData))
                .build();
    }

    @Override
    @NonNull
    public Optional<PersonalBaselineDto> extractPersonalBaseline(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        if (!supports(source) || platformData.getPersonalBaseline() == null) {
            return Optional.empty();
        }

        Map<String, Object> baselineData = platformData.getPersonalBaseline();
        
        return Optional.of(PersonalBaselineDto.builder()
                .averageSleepDuration(extractDouble(baselineData, "averageSleepDuration"))
                .averageSleepEfficiency(extractDouble(baselineData, "averageSleepEfficiency"))
                .averageSleepScore(extractDouble(baselineData, "averageSleepScore"))
                .consistencyScore(extractInteger(baselineData, "consistencyScore"))
                .build());
    }

    @Override
    @NonNull
    public List<PatternAnalysisDto> extractPatternAnalysis(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        if (!supports(source) || platformData.getAiPatternAnalysis() == null) {
            return Collections.emptyList();
        }

        // Health Connect 특화 패턴 분석 로직
        return Collections.emptyList();
    }

    @Override
    @NonNull
    public Optional<SleepTrendDto> extractSleepTrend(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source,
            @NonNull LocalDate startDate,
            @NonNull LocalDate endDate) {
        
        if (!supports(source) || platformData.getSleepTrends() == null) {
            return Optional.empty();
        }

        // Health Connect 트렌드 분석 로직
        return Optional.empty();
    }

    @Override
    @NonNull
    public List<HealthAlertDto> extractHealthAlerts(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        if (!supports(source) || platformData.getHealthAlerts() == null) {
            return Collections.emptyList();
        }

        // Health Connect 건강 알림 추출
        return Collections.emptyList();
    }

    @Override
    public boolean supports(@NonNull WearableSource source) {
        return source == WearableSource.HEALTH_CONNECT;
    }

    @Override
    @NonNull
    public ValidationResult validatePlatformData(
            @Nullable PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        List<String> errors = new ArrayList<>();
        
        if (platformData == null) {
            errors.add("Platform data is null");
            return ValidationResult.failure(errors);
        }
        
        if (!supports(source)) {
            errors.add("Unsupported platform source: " + source);
            return ValidationResult.failure(errors);
        }
        
        // Health Connect 특화 검증 로직
        if (platformData.getSleepStartTime() == null) {
            errors.add("Sleep start time is required");
        }
        
        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        
        return ValidationResult.failure(errors);
    }

    @Override
    public int calculateDataQualityScore(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        if (!supports(source)) {
            return 0;
        }
        
        int score = 0;
        
        // Health Connect 데이터 품질 평가
        if (platformData.getTotalSleepMinutes() != null) score += 20;
        if (platformData.getDeepSleepMinutes() != null) score += 20;
        if (platformData.getRemSleepMinutes() != null) score += 20;
        if (platformData.getPlatformSleepScore() != null) score += 20;
        if (platformData.getSleepEfficiency() != null) score += 20;
        
        return Math.min(score, 100);
    }

    // Helper methods
    private UnifiedSleepAnalysisDto.SleepSessionInfo buildSleepSessionInfo(PlatformSleepDataDto data) {
        return UnifiedSleepAnalysisDto.SleepSessionInfo.builder()
                .bedTime(data.getSleepStartTime())
                .sleepTime(data.getSleepStartTime())
                .wakeTime(data.getSleepEndTime())
                .totalTimeInBed(data.getTimeInBedMinutes())
                .totalSleepTime(data.getTotalSleepMinutes())
                .wakeupCount(data.getWakeupCount())
                .build();
    }

    private Integer calculateUnifiedScore(PlatformSleepDataDto data) {
        if (data.getPlatformSleepScore() != null) {
            return data.getPlatformSleepScore();
        }
        
        // Health Connect 특화 점수 계산
        double score = 80.0; // Health Connect 기본 점수
        if (data.getSleepEfficiency() != null) {
            score = score * 0.6 + data.getSleepEfficiency() * 0.4;
        }
        
        return Math.min(100, Math.max(0, (int) Math.round(score)));
    }

    private Integer calculateReliabilityScore(PlatformSleepDataDto data) {
        return data.getDataQualityScore() != null ? data.getDataQualityScore() : 85;
    }

    private Integer countDataPoints(PlatformSleepDataDto data) {
        int count = 0;
        if (data.getTotalSleepMinutes() != null) count++;
        if (data.getDeepSleepMinutes() != null) count++;
        if (data.getLightSleepMinutes() != null) count++;
        if (data.getRemSleepMinutes() != null) count++;
        if (data.getSleepEfficiency() != null) count++;
        return count;
    }

    // Data extraction helpers
    private Integer extractInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Double extractDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
} 