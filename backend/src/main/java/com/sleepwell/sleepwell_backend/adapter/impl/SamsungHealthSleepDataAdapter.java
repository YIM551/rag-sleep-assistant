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
 * 삼성 헬스 플랫폼 데이터 어댑터 구현체
 * 
 * 삼성 헬스에서 제공하는 개인화된 수면 분석 데이터를
 * 통합 데이터 모델로 변환하는 어댑터입니다.
 * 
 * Spring Boot 베스트 프랙티스:
 * - @Component로 스프링 빈 등록
 * - @Slf4j로 로깅 기능 제공
 * - 명시적인 null 안전성 어노테이션 사용
 * - 예외 처리와 검증 로직 포함
 * - 메서드별 단일 책임 원칙 적용
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Component
public class SamsungHealthSleepDataAdapter implements PlatformSleepDataAdapter {

    private static final String SAMSUNG_HEALTH_PLATFORM = "Samsung Health";
    private static final int MIN_DATA_QUALITY_THRESHOLD = 70;
    private static final int BASELINE_CALCULATION_DAYS = 30;

    @Override
    @NonNull
    public UnifiedSleepAnalysisDto convertToUnifiedAnalysis(
            @NonNull PlatformSleepDataDto platformData, 
            @NonNull WearableSource source) {
        
        if (!supports(source)) {
            throw new IllegalArgumentException("Unsupported platform source: " + source);
        }

        log.info("Converting Samsung Health data to unified analysis");

        // 수면 세션 정보 생성
        UnifiedSleepAnalysisDto.SleepSessionInfo sleepSession = UnifiedSleepAnalysisDto.SleepSessionInfo.builder()
                .sleepTime(platformData.getSleepStartTime())
                .wakeTime(platformData.getSleepEndTime())
                .totalSleepTime(platformData.getTotalSleepMinutes())
                .totalTimeInBed(platformData.getTotalSleepMinutes() + 
                    Optional.ofNullable(platformData.getAwakeMinutes()).orElse(0))
                .wakeupCount(Optional.ofNullable(platformData.getWakeupCount()).orElse(0))
                .build();

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

        // 개인화된 인사이트 추가
        List<UnifiedSleepAnalysisDto.PatternInsight> personalizedInsights = generatePersonalizedInsights(platformData);

        return UnifiedSleepAnalysisDto.builder()
                .originalSource(source)
                .analyzedAt(LocalDateTime.now())
                .sleepSession(sleepSession)
                .sleepStages(sleepStages)
                .unifiedSleepScore(platformData.getPlatformSleepScore())
                .platformScore(platformData.getPlatformSleepScore())
                .reliabilityScore(calculateDataQualityScore(platformData, source))
                .dataPointsUsed(5)
                .patternInsights(personalizedInsights)
                .build();
    }

    @Override
    @NonNull
    public Optional<PersonalBaselineDto> extractPersonalBaseline(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        log.debug("Extracting personal baseline for Samsung Health");
        
        if (platformData == null || !supports(source) || platformData.getPersonalBaseline() == null) {
            log.warn("No personal baseline data available in Samsung Health data");
            return Optional.empty();
        }

        try {
            Map<String, Object> baselineData = platformData.getPersonalBaseline();
            
            // Samsung Health 특화 개인 기준선 추출
            PersonalBaselineDto baseline = PersonalBaselineDto.builder()
                    .baselineStartDate(LocalDate.now().minusDays(BASELINE_CALCULATION_DAYS))
                    .baselineEndDate(LocalDate.now().minusDays(1))
                    .dataPointsCount(extractInteger(baselineData, "dataPointsCount", BASELINE_CALCULATION_DAYS))
                    .averageSleepDuration(extractDouble(baselineData, "avgSleepDuration", 450.0)) // 7.5시간 -> 450분
                    .averageSleepEfficiency(extractDouble(baselineData, "avgSleepEfficiency", 85.0))
                    .averageSleepScore(extractDouble(baselineData, "avgSleepScore", 75.0))
                    .averageDeepSleepRatio(extractDouble(baselineData, "avgDeepSleepRatio", 20.0))
                    .averageRemSleepRatio(extractDouble(baselineData, "avgRemSleepRatio", 25.0))
                    .averageBedTime(extractString(baselineData, "avgBedTime", "23:00"))
                    .averageWakeTime(extractString(baselineData, "avgWakeTime", "07:00"))
                    .consistencyScore(extractInteger(baselineData, "consistencyScore", 70))
                    .additionalMetrics(extractMap(baselineData, "additionalMetrics"))
                    .build();

            log.info("Successfully extracted personal baseline from Samsung Health");
            return Optional.of(baseline);
            
        } catch (Exception e) {
            log.error("Error extracting personal baseline from Samsung Health", e);
            return Optional.empty();
        }
    }

    @Override
    @NonNull
    public List<PatternAnalysisDto> extractPatternAnalysis(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        log.debug("Extracting pattern analysis for Samsung Health");
        
        if (platformData == null || !supports(source) || platformData.getAiPatternAnalysis() == null) {
            log.warn("No AI pattern analysis data available in Samsung Health data");
            return Collections.emptyList();
        }

        try {
            List<PatternAnalysisDto> patterns = new ArrayList<>();
            Map<String, Object> patternData = platformData.getAiPatternAnalysis();
            
            // Samsung Health AI 패턴 분석 처리
            if (patternData.containsKey("sleepPatterns")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> sleepPatterns = (List<Map<String, Object>>) patternData.get("sleepPatterns");
                
                for (Map<String, Object> pattern : sleepPatterns) {
                    PatternAnalysisDto patternDto = PatternAnalysisDto.builder()
                            .patternType(extractString(pattern, "type", "UNKNOWN"))
                            .title(extractString(pattern, "title", "Samsung Health 패턴 분석"))
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

            // 추가 Samsung Health 특화 패턴 감지
            patterns.addAll(detectSamsungSpecificPatterns(platformData));
            
            log.info("Extracted {} pattern analysis results from Samsung Health", patterns.size());
            return patterns;
            
        } catch (Exception e) {
            log.error("Error extracting pattern analysis from Samsung Health", e);
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
        
        log.debug("Extracting sleep trend for Samsung Health from {} to {}", startDate, endDate);
        
        if (!supports(source) || platformData.getSleepTrends() == null) {
            log.warn("No sleep trend data available in Samsung Health data");
            return Optional.empty();
        }

        try {
            Map<String, Object> trendData = platformData.getSleepTrends();
            
            SleepTrendDto trend = SleepTrendDto.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .periodType("MONTHLY")
                    .overallTrend(extractString(trendData, "direction", "STABLE"))
                    .confidence(extractDouble(trendData, "confidence", 0.8))
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

            log.info("Successfully extracted sleep trend from Samsung Health");
            return Optional.of(trend);
            
        } catch (Exception e) {
            log.error("Error extracting sleep trend from Samsung Health", e);
            return Optional.empty();
        }
    }

    @Override
    @NonNull
    public List<HealthAlertDto> extractHealthAlerts(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        log.debug("Extracting health alerts for Samsung Health");
        
        if (platformData == null || !supports(source)) {
            return Collections.emptyList();
        }

        try {
            List<HealthAlertDto> alerts = new ArrayList<>();
            
            // Samsung Health에서 제공하는 건강 알림 처리
            if (platformData.getHealthAlerts() != null) {
                Map<String, Object> healthAlertsMap = platformData.getHealthAlerts();
                if (healthAlertsMap.containsKey("alerts")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> healthAlerts = (List<Map<String, Object>>) healthAlertsMap.get("alerts");
                    
                    for (Map<String, Object> alert : healthAlerts) {
                        HealthAlertDto alertDto = HealthAlertDto.builder()
                                .alertType(extractString(alert, "type", "GENERAL"))
                                .severity(extractString(alert, "severity", "LOW"))
                                .title(extractString(alert, "title", "Samsung Health 알림"))
                                .description(extractString(alert, "description", "No description available"))
                                .detectedAt(extractLocalDateTime(alert, "detectedAt", LocalDateTime.now()))
                                .confidence(extractDouble(alert, "confidence", 0.8))
                                .recommendedAction(extractString(alert, "recommendedAction", "일반적인 수면 위생 권장사항을 따라주세요"))
                                .requiresMedicalConsultation(extractBoolean(alert, "requiresMedicalConsultation", false))
                                .alertDetails(extractMap(alert, "alertDetails"))
                                .relatedMetrics(extractMap(alert, "relatedMetrics"))
                                .build();
                        
                        alerts.add(alertDto);
                    }
                }
            }

            // Samsung Health 특화 건강 알림 생성 (수면 무호흡, 불규칙 수면 패턴 등)
            alerts.addAll(generateSamsungHealthAlerts(platformData));
            
            log.info("Extracted {} health alerts from Samsung Health", alerts.size());
            return alerts;
            
        } catch (Exception e) {
            log.error("Error extracting health alerts from Samsung Health", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean supports(@NonNull WearableSource source) {
        return source == WearableSource.SAMSUNG_HEALTH;
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
        
        // Samsung Health 특화 검증 로직
        if (platformData.getSleepStartTime() == null) {
            errors.add("Sleep start time is required for Samsung Health");
        }
        
        if (platformData.getTotalSleepMinutes() == null || platformData.getTotalSleepMinutes() <= 0) {
            errors.add("Valid total sleep minutes is required for Samsung Health");
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
        
        // Samsung Health 데이터 품질 평가
        if (platformData.getTotalSleepMinutes() != null) score += 15;
        if (platformData.getDeepSleepMinutes() != null) score += 15;
        if (platformData.getLightSleepMinutes() != null) score += 15;
        if (platformData.getRemSleepMinutes() != null) score += 15;
        if (platformData.getPlatformSleepScore() != null) score += 20;
        if (platformData.getSleepEfficiency() != null) score += 10;
        if (platformData.getPersonalBaseline() != null) score += 10;
        
        return Math.min(score, 100);
    }

    // Helper methods
    private List<UnifiedSleepAnalysisDto.PatternInsight> generatePersonalizedInsights(PlatformSleepDataDto platformData) {
        List<UnifiedSleepAnalysisDto.PatternInsight> insights = new ArrayList<>();
        
        // Samsung Health 개인화된 인사이트 생성
        if (platformData.getSleepEfficiency() != null) {
            if (platformData.getSleepEfficiency() > 90) {
                insights.add(UnifiedSleepAnalysisDto.PatternInsight.builder()
                        .patternType("SLEEP_EFFICIENCY")
                        .description("수면 효율성이 매우 좋습니다 (90% 이상)")
                        .confidence(0.9)
                        .recommendations(List.of("현재 수면 패턴을 유지하세요"))
                        .build());
            } else if (platformData.getSleepEfficiency() < 70) {
                insights.add(UnifiedSleepAnalysisDto.PatternInsight.builder()
                        .patternType("SLEEP_EFFICIENCY")
                        .description("수면 효율성 개선이 필요합니다 (70% 미만)")
                        .confidence(0.8)
                        .recommendations(List.of("수면 환경 개선", "수면 위생 점검"))
                        .build());
            }
        }
        
        if (platformData.getDeepSleepMinutes() != null && platformData.getTotalSleepMinutes() != null) {
            double deepSleepRatio = (platformData.getDeepSleepMinutes().doubleValue() / platformData.getTotalSleepMinutes().doubleValue()) * 100;
            if (deepSleepRatio < 15) {
                insights.add(UnifiedSleepAnalysisDto.PatternInsight.builder()
                        .patternType("DEEP_SLEEP_RATIO")
                        .description("깊은 수면 비율이 낮습니다. 수면 환경 개선을 고려해보세요.")
                        .confidence(0.7)
                        .recommendations(List.of("수면 환경 개선", "운동 시간 조절", "스트레스 관리"))
                        .build());
            }
        }
        
        return insights;
    }

    private List<PatternAnalysisDto> detectSamsungSpecificPatterns(PlatformSleepDataDto platformData) {
        List<PatternAnalysisDto> patterns = new ArrayList<>();
        
        // Samsung Health 특화 패턴 감지 로직
        if (platformData.getWakeupCount() != null && platformData.getWakeupCount() > 3) {
            patterns.add(PatternAnalysisDto.builder()
                    .patternType("FREQUENT_AWAKENINGS")
                    .title("잦은 수면 중 각성")
                    .description("수면 중 3회 이상의 각성이 감지되었습니다")
                    .confidence(0.8)
                    .severity("MEDIUM")
                    .detectedAt(LocalDateTime.now())
                    .recommendation("수면 환경 점검, 스트레스 관리, 카페인 섭취 시간 조절을 권장합니다")
                    .build());
        }
        
        return patterns;
    }

    private List<HealthAlertDto> generateSamsungHealthAlerts(PlatformSleepDataDto platformData) {
        List<HealthAlertDto> alerts = new ArrayList<>();
        
        // Samsung Health 특화 건강 알림 생성
        if (platformData.getTotalSleepMinutes() != null && platformData.getTotalSleepMinutes() < 360) { // 6시간 미만
            alerts.add(HealthAlertDto.builder()
                    .alertType("INSUFFICIENT_SLEEP")
                    .severity("HIGH")
                    .title("수면 부족 경고")
                    .description("수면 시간이 권장 기준보다 부족합니다 (6시간 미만)")
                    .detectedAt(LocalDateTime.now())
                    .confidence(0.9)
                    .requiresMedicalConsultation(true)
                    .recommendedAction("수면 시간 늘리기, 수면 스케줄 개선, 의료진 상담 고려")
                    .build());
        }
        
        return alerts;
    }

    // 헬퍼 메서드들
    private Double extractDouble(Map<String, Object> data, String key, Double defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private Integer extractInteger(Map<String, Object> data, String key, Integer defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
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
        return "일반적인 수면 위생 권장사항을 따라주세요";
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
} 