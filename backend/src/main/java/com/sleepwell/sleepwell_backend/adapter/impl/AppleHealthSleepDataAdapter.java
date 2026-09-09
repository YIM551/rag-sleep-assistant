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
 * 애플 헬스 플랫폼 데이터 어댑터 구현체
 * 
 * Apple HealthKit에서 제공하는 개인화된 수면 분석 데이터를
 * 통합 데이터 모델로 변환하는 어댑터입니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Component
public class AppleHealthSleepDataAdapter implements PlatformSleepDataAdapter {

    private static final String APPLE_HEALTH_PLATFORM = "Apple Health";

    @Override
    @NonNull
    public UnifiedSleepAnalysisDto convertToUnifiedAnalysis(
            @NonNull PlatformSleepDataDto platformData, 
            @NonNull WearableSource source) {
        
        log.debug("Converting Apple Health data to unified analysis");
        
        if (!supports(source)) {
            throw new IllegalArgumentException("Unsupported platform source: " + source);
        }

        // 개인화된 인사이트 추가
        List<UnifiedSleepAnalysisDto.PatternInsight> personalizedInsights = generatePersonalizedInsights(platformData);

        return UnifiedSleepAnalysisDto.builder()
                .originalSource(source)
                .analyzedAt(LocalDateTime.now())
                .sleepSession(buildSleepSessionInfo(platformData))
                .sleepStages(buildSleepStageAnalysis(platformData))
                .unifiedSleepScore(calculateUnifiedScore(platformData))
                .platformScore(platformData.getPlatformSleepScore())
                .reliabilityScore(calculateReliabilityScore(platformData))
                .dataPointsUsed(countDataPoints(platformData))
                .patternInsights(personalizedInsights)
                .build();
    }

    @Override
    @NonNull
    public Optional<PersonalBaselineDto> extractPersonalBaseline(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        log.debug("Extracting personal baseline for Apple Health");
        
        if (!supports(source) || platformData.getPersonalBaseline() == null) {
            log.warn("No personal baseline data available in Apple Health data");
            return Optional.empty();
        }

        try {
            Map<String, Object> baselineData = platformData.getPersonalBaseline();
            
            // Apple Health 특화 개인 기준선 추출
            PersonalBaselineDto baseline = PersonalBaselineDto.builder()
                    .baselineStartDate(LocalDate.now().minusDays(30))
                    .baselineEndDate(LocalDate.now().minusDays(1))
                    .dataPointsCount(extractInteger(baselineData, "dataPointsCount", 30))
                    .averageSleepDuration(extractDouble(baselineData, "averageSleepDuration", 440.0)) // 7.3시간 -> 440분
                    .averageSleepEfficiency(extractDouble(baselineData, "averageSleepEfficiency", 88.0))
                    .averageSleepScore(extractDouble(baselineData, "averageSleepScore", 80.0))
                    .averageDeepSleepRatio(extractDouble(baselineData, "averageDeepSleepRatio", 18.0))
                    .averageRemSleepRatio(extractDouble(baselineData, "averageRemSleepRatio", 22.0))
                    .averageBedTime(extractString(baselineData, "averageBedTime", "23:15"))
                    .averageWakeTime(extractString(baselineData, "averageWakeTime", "07:15"))
                    .consistencyScore(extractInteger(baselineData, "consistencyScore", 75))
                    .additionalMetrics(extractMap(baselineData, "additionalMetrics"))
                    .build();

            log.info("Successfully extracted personal baseline from Apple Health");
            return Optional.of(baseline);
            
        } catch (Exception e) {
            log.error("Error extracting personal baseline from Apple Health", e);
            return Optional.empty();
        }
    }

    @Override
    @NonNull
    public List<PatternAnalysisDto> extractPatternAnalysis(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        log.debug("Extracting pattern analysis for Apple Health");
        
        if (!supports(source) || platformData.getAiPatternAnalysis() == null) {
            log.warn("No AI pattern analysis data available in Apple Health data");
            return Collections.emptyList();
        }

        try {
            List<PatternAnalysisDto> patterns = new ArrayList<>();
            Map<String, Object> patternData = platformData.getAiPatternAnalysis();
            
            // Apple Health AI 패턴 분석 처리
            if (patternData.containsKey("sleepPatterns")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> sleepPatterns = (List<Map<String, Object>>) patternData.get("sleepPatterns");
                
                for (Map<String, Object> pattern : sleepPatterns) {
                    PatternAnalysisDto patternDto = PatternAnalysisDto.builder()
                            .patternType(extractString(pattern, "type", "UNKNOWN"))
                            .title(extractString(pattern, "title", "Apple Health 패턴 분석"))
                            .description(extractString(pattern, "description", "No description available"))
                            .severity(extractString(pattern, "severity", "LOW"))
                            .confidence(extractDouble(pattern, "confidence", 0.0))
                            .detectedAt(extractLocalDateTime(pattern, "detectedAt", LocalDateTime.now()))
                            .durationDays(extractInteger(pattern, "durationDays", 7))
                            .recommendation(extractRecommendation(pattern))
                            .metrics(extractMap(pattern, "metrics"))
                            .trend(extractString(pattern, "trend", "STABLE"))
                            .nextEvaluationDate(extractLocalDateTime(pattern, "nextEvaluationDate", LocalDateTime.now().plusDays(7)))
                            .analysisDetails(extractMap(pattern, "analysisDetails"))
                            .build();
                    
                    patterns.add(patternDto);
                }
            }

            // 추가 Apple Health 특화 패턴 감지
            patterns.addAll(detectAppleSpecificPatterns(platformData));
            
            log.info("Extracted {} pattern analysis results from Apple Health", patterns.size());
            return patterns;
            
        } catch (Exception e) {
            log.error("Error extracting pattern analysis from Apple Health", e);
            return Collections.emptyList();
        }
    }

    @Override
    @NonNull
    public Optional<SleepTrendDto> extractSleepTrend(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source,
            @NonNull LocalDate startDate,
            @NonNull LocalDate endDate) {
        
        log.debug("Extracting sleep trend for Apple Health from {} to {}", startDate, endDate);
        
        if (!supports(source) || platformData.getSleepTrends() == null) {
            log.warn("No sleep trend data available in Apple Health data");
            return Optional.empty();
        }

        try {
            Map<String, Object> trendData = platformData.getSleepTrends();
            
            SleepTrendDto trend = SleepTrendDto.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .periodType("MONTHLY")
                    .overallTrend(extractString(trendData, "direction", "STABLE"))
                    .confidence(extractDouble(trendData, "confidence", 0.85))
                    .sleepDurationTrend(createTrendMetric(trendData, "sleepDuration", "minutes"))
                    .sleepEfficiencyTrend(createTrendMetric(trendData, "sleepEfficiency", "%"))
                    .sleepScoreTrend(createTrendMetric(trendData, "sleepScore", "score"))
                    .bedtimeConsistencyTrend(createTrendMetric(trendData, "bedtimeConsistency", "score"))
                    .wakeTimeConsistencyTrend(createTrendMetric(trendData, "wakeTimeConsistency", "score"))
                    .keyInsights(extractStringList(trendData, "keyInsights"))
                    .recommendations(extractStringList(trendData, "recommendations"))
                    .detailedAnalysis(extractMap(trendData, "detailedAnalysis"))
                    .nextAnalysisDate(endDate.plusMonths(1))
                    .build();

            log.info("Successfully extracted sleep trend from Apple Health");
            return Optional.of(trend);
            
        } catch (Exception e) {
            log.error("Error extracting sleep trend from Apple Health", e);
            return Optional.empty();
        }
    }

    @Override
    @NonNull
    public List<HealthAlertDto> extractHealthAlerts(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        log.debug("Extracting health alerts for Apple Health");
        
        if (!supports(source)) {
            return Collections.emptyList();
        }

        try {
            List<HealthAlertDto> alerts = new ArrayList<>();
            
            // Apple Health에서 제공하는 건강 알림 처리
            if (platformData.getHealthAlerts() != null) {
                Map<String, Object> healthAlertsMap = platformData.getHealthAlerts();
                if (healthAlertsMap.containsKey("alerts")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> healthAlerts = (List<Map<String, Object>>) healthAlertsMap.get("alerts");

                    for (Map<String, Object> alert : healthAlerts) {
                        HealthAlertDto alertDto = HealthAlertDto.builder()
                                .alertType(extractString(alert, "type", "GENERAL"))
                                .severity(extractString(alert, "severity", "LOW"))
                                .title(extractString(alert, "title", "Apple Health 알림"))
                                .description(extractString(alert, "description", "No description available"))
                                .detectedAt(extractLocalDateTime(alert, "detectedAt", LocalDateTime.now()))
                                .confidence(extractDouble(alert, "confidence", 0.85))
                                .recommendedAction(extractString(alert, "recommendedAction", "Apple Health 권장사항을 확인하세요"))
                                .requiresMedicalConsultation(extractBoolean(alert, "requiresMedicalConsultation", false))
                                .alertDetails(extractMap(alert, "alertDetails"))
                                .relatedMetrics(extractMap(alert, "relatedMetrics"))
                                .build();

                        alerts.add(alertDto);
                    }
                }
            }

            // Apple Health 특화 건강 알림 생성 (수면 무호흡, 호흡 장애 등)
            alerts.addAll(generateAppleHealthAlerts(platformData));
            
            log.info("Extracted {} health alerts from Apple Health", alerts.size());
            return alerts;
            
        } catch (Exception e) {
            log.error("Error extracting health alerts from Apple Health", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean supports(@NonNull WearableSource source) {
        return source == WearableSource.APPLE_HEALTH;
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
        
        // 애플 헬스 특화 검증 로직
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
        
        // 애플 헬스 데이터 품질 평가
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

    private UnifiedSleepAnalysisDto.SleepStageAnalysis buildSleepStageAnalysis(PlatformSleepDataDto data) {
        log.debug("Converting sleep stage data - Deep: {}, Light: {}, REM: {}, Awake: {}",
                data.getDeepSleepMinutes(),
                data.getLightSleepMinutes(),
                data.getRemSleepMinutes(),
                data.getAwakeMinutes());

        return UnifiedSleepAnalysisDto.SleepStageAnalysis.builder()
                .deepSleepMinutes(data.getDeepSleepMinutes())
                .lightSleepMinutes(data.getLightSleepMinutes())
                .remSleepMinutes(data.getRemSleepMinutes())
                .awakeMinutes(data.getAwakeMinutes())
                .build();
    }

    private List<UnifiedSleepAnalysisDto.PatternInsight> generatePersonalizedInsights(PlatformSleepDataDto platformData) {
        List<UnifiedSleepAnalysisDto.PatternInsight> insights = new ArrayList<>();
        
        // Apple Health 개인화된 인사이트 생성
        if (platformData.getSleepEfficiency() != null) {
            if (platformData.getSleepEfficiency() > 95) {
                insights.add(UnifiedSleepAnalysisDto.PatternInsight.builder()
                        .patternType("SLEEP_EFFICIENCY")
                        .description("Apple Health: 수면 효율성이 매우 우수합니다 (95% 이상)")
                        .confidence(0.95)
                        .recommendations(List.of("현재의 우수한 수면 패턴을 유지하세요"))
                        .build());
            } else if (platformData.getSleepEfficiency() < 75) {
                insights.add(UnifiedSleepAnalysisDto.PatternInsight.builder()
                        .patternType("SLEEP_EFFICIENCY")
                        .description("Apple Health: 수면 효율성 개선이 권장됩니다 (75% 미만)")
                        .confidence(0.85)
                        .recommendations(List.of("취침 루틴 개선", "수면 환경 최적화", "Apple Watch 수면 모드 활용"))
                        .build());
            }
        }
        
        // REM 수면 분석 (Apple Health 특화)
        if (platformData.getRemSleepMinutes() != null && platformData.getTotalSleepMinutes() != null) {
            double remSleepRatio = (platformData.getRemSleepMinutes().doubleValue() / platformData.getTotalSleepMinutes().doubleValue()) * 100;
            if (remSleepRatio < 20) {
                insights.add(UnifiedSleepAnalysisDto.PatternInsight.builder()
                        .patternType("REM_SLEEP_RATIO")
                        .description("Apple Health: REM 수면 비율이 낮습니다. 꿈의 질과 기억력에 영향을 줄 수 있습니다.")
                        .confidence(0.8)
                        .recommendations(List.of("규칙적인 수면 스케줄", "스트레스 관리", "수면 전 전자기기 사용 제한"))
                        .build());
            }
        }
        
        return insights;
    }

    private List<PatternAnalysisDto> detectAppleSpecificPatterns(PlatformSleepDataDto platformData) {
        List<PatternAnalysisDto> patterns = new ArrayList<>();
        
        // Apple Health 특화 패턴 감지
        if (platformData.getTimeInBedMinutes() != null && platformData.getTotalSleepMinutes() != null) {
            double efficiency = (platformData.getTotalSleepMinutes().doubleValue() / platformData.getTimeInBedMinutes().doubleValue()) * 100;
            if (efficiency < 80) {
                patterns.add(PatternAnalysisDto.builder()
                        .patternType("POOR_SLEEP_EFFICIENCY")
                        .title("수면 효율성 저하")
                        .description("침대에서 보낸 시간 대비 실제 수면 시간이 부족합니다")
                        .confidence(0.85)
                        .severity("MEDIUM")
                        .detectedAt(LocalDateTime.now())
                        .recommendation("수면 위생 개선, Apple Watch의 수면 추적 기능 활용, 침실 환경 점검을 권장합니다")
                        .build());
            }
        }
        
        return patterns;
    }

    private List<HealthAlertDto> generateAppleHealthAlerts(PlatformSleepDataDto platformData) {
        List<HealthAlertDto> alerts = new ArrayList<>();
        
        // Apple Health 특화 건강 알림 생성
        if (platformData.getTotalSleepMinutes() != null && platformData.getTotalSleepMinutes() < 300) { // 5시간 미만
            alerts.add(HealthAlertDto.builder()
                    .alertType("SEVERE_SLEEP_DEPRIVATION")
                    .severity("CRITICAL")
                    .title("심각한 수면 부족")
                    .description("Apple Health: 수면 시간이 심각하게 부족합니다 (5시간 미만)")
                    .detectedAt(LocalDateTime.now())
                    .confidence(0.95)
                    .requiresMedicalConsultation(true)
                    .recommendedAction("즉시 수면 패턴 개선 및 의료진 상담 권장")
                    .build());
        }
        
        // Apple Watch 심박수 기반 수면 무호흡 의심 (가상 로직)
        if (platformData.getWakeupCount() != null && platformData.getWakeupCount() > 5) {
            alerts.add(HealthAlertDto.builder()
                    .alertType("POSSIBLE_SLEEP_APNEA")
                    .severity("HIGH")
                    .title("수면 무호흡 의심")
                    .description("Apple Health: 수면 중 잦은 각성이 감지되어 수면 무호흡이 의심됩니다")
                    .detectedAt(LocalDateTime.now())
                    .confidence(0.7)
                    .requiresMedicalConsultation(true)
                    .recommendedAction("수면 전문의 상담 및 정밀 검사 권장")
                    .build());
        }
        
        return alerts;
    }

    private Integer calculateUnifiedScore(PlatformSleepDataDto data) {
        // Apple Health 데이터를 기반으로 한 통합 점수 계산
        // 예: (수면 시간 점수 * 0.4) + (깊은 수면 점수 * 0.3) + (REM 수면 점수 * 0.3)
        int score = Optional.ofNullable(data.getPlatformSleepScore()).orElse(75);
        Integer deepSleepMinutes = data.getDeepSleepMinutes();
        Integer remSleepMinutes = data.getRemSleepMinutes();
        if (deepSleepMinutes != null && deepSleepMinutes < 60) score -= 5;
        if (remSleepMinutes != null && remSleepMinutes < 90) score -= 5;
        return Math.max(0, Math.min(100, score));
    }

    private Integer calculateReliabilityScore(PlatformSleepDataDto data) {
        return 90; // Apple Health 데이터는 신뢰도가 높다고 가정
    }

    private Integer countDataPoints(PlatformSleepDataDto data) {
        // 사용된 데이터 포인트 수 계산 (단순화된 예)
        return 5;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> data, String key) {
        if (data != null && data.get(key) instanceof Map) {
            return (Map<String, Object>) data.get(key);
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private SleepTrendDto.TrendMetric createTrendMetric(Map<String, Object> trendData, String metricKey, String unit) {
        Map<String, Object> metricMap = Collections.emptyMap();
        if (trendData.get(metricKey) instanceof Map) {
            metricMap = (Map<String, Object>) trendData.get(metricKey);
        }

        return SleepTrendDto.TrendMetric.builder()
                .currentValue(extractDouble(metricMap, "currentValue", 0.0))
                .previousValue(extractDouble(metricMap, "previousValue", 0.0))
                .changeAmount(extractDouble(metricMap, "changeAmount", 0.0))
                .changePercentage(extractDouble(metricMap, "changePercentage", 0.0))
                .direction(extractString(metricMap, "direction", "STABLE"))
                .isSignificant(extractBoolean(metricMap, "isSignificant", false))
                .unit(unit)
                .build();
    }

    private Integer extractInteger(Map<String, Object> data, String key, Integer defaultValue) {
        Object value = data.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private Double extractDouble(Map<String, Object> data, String key, Double defaultValue) {
        Object value = data.get(key);
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private String extractString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    private Boolean extractBoolean(Map<String, Object> data, String key, Boolean defaultValue) {
        Object value = data.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    private LocalDateTime extractLocalDateTime(Map<String, Object> data, String key, LocalDateTime defaultValue) {
        Object value = data.get(key);
        if (value instanceof String) {
            try {
                return LocalDateTime.parse((String) value);
            } catch (Exception e) {
                log.warn("Failed to parse LocalDateTime from string: {}", value);
            }
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractStringList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return new ArrayList<>();
    }

    private String extractRecommendation(Map<String, Object> pattern) {
        Object recommendation = pattern.get("recommendation");
        if (recommendation instanceof String) {
            return (String) recommendation;
        }
        @SuppressWarnings("unchecked")
        List<String> recommendations = (List<String>) pattern.get("recommendations");
        if (recommendations != null && !recommendations.isEmpty()) {
            return String.join(", ", recommendations);
        }
        return "Apple Health 권장사항을 확인하세요";
    }
} 