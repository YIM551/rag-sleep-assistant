package com.sleepwell.sleepwell_backend.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI 수면 분석을 위한 최적화된 프롬프트 템플릿
 * 
 * 이 클래스는 수면 데이터를 분석하기 위한 구조화된 프롬프트를 생성합니다.
 * AI 모델이 일관되고 정확한 분석 결과를 제공할 수 있도록 설계되었습니다.
 */
@Component
public class SleepAnalysisPromptTemplate {

    /**
     * 종합적인 수면 분석을 위한 메인 프롬프트 생성
     */
    public String buildComprehensiveAnalysisPrompt(Map<String, Object> sleepData) {
        return String.format("""
            You are an expert sleep specialist with deep knowledge in sleep medicine and chronobiology.
            Analyze the following sleep data comprehensively and provide personalized insights.
            
            ## Sleep Data Overview
            
            ### Sleep Duration and Efficiency
            - Total Sleep Time: %d minutes (%.1f hours)
            - Time in Bed: %d minutes
            - Sleep Efficiency: %.1f%% (TST/TIB ratio)
            - Sleep Latency: %d minutes
            - Number of Awakenings: %d
            
            ### Sleep Architecture
            - Deep Sleep (N3): %d minutes (%.1f%% of TST) - Target: 15-20%%
            - Light Sleep (N1+N2): %d minutes (%.1f%% of TST) - Target: 50-60%%
            - REM Sleep: %d minutes (%.1f%% of TST) - Target: 20-25%%
            
            ### Physiological Metrics
            - Average Heart Rate: %.1f bpm
            - Heart Rate Variability: %.1f ms
            - Average Respiratory Rate: %.1f breaths/min
            - Movement Intensity: %.1f (scale 0-10)
            
            ### Environmental Factors
            - Room Temperature: %.1f°C (Optimal: 18-21°C)
            - Humidity: %.1f%% (Optimal: 40-60%%)
            - Noise Level: %.1f dB (Optimal: <40dB)
            - Light Exposure: %.1f lux (Optimal: <1 lux)
            
            ### Context
            - Sleep Goal: %d minutes
            - Previous Night's Score: %.1f/100
            - Weekly Average Score: %.1f/100
            
            ## Analysis Requirements
            
            Please provide a comprehensive analysis in the following JSON format:
            
            ```json
            {
                "overallScore": <integer 0-100>,
                "scoreBreakdown": {
                    "duration": <score 0-100>,
                    "efficiency": <score 0-100>,
                    "architecture": <score 0-100>,
                    "continuity": <score 0-100>,
                    "environment": <score 0-100>
                },
                "sleepQualityLevel": "<excellent|good|fair|poor>",
                "keyInsights": [
                    {
                        "category": "<duration|efficiency|architecture|continuity|environment>",
                        "finding": "<specific observation>",
                        "impact": "<positive|negative|neutral>",
                        "severity": "<high|medium|low>"
                    }
                ],
                "recommendations": [
                    {
                        "priority": "<high|medium|low>",
                        "category": "<sleep_hygiene|environment|schedule|lifestyle|medical>",
                        "action": "<specific actionable recommendation>",
                        "expectedBenefit": "<expected improvement>",
                        "timeframe": "<immediate|short_term|long_term>"
                    }
                ],
                "sleepStageAnalysis": {
                    "deepSleep": {
                        "assessment": "<detailed assessment>",
                        "deviation": "<percentage from target>",
                        "implications": "<health implications>"
                    },
                    "lightSleep": {
                        "assessment": "<detailed assessment>",
                        "deviation": "<percentage from target>",
                        "implications": "<health implications>"
                    },
                    "remSleep": {
                        "assessment": "<detailed assessment>",
                        "deviation": "<percentage from target>",
                        "implications": "<health implications>"
                    }
                },
                "physiologicalAnalysis": {
                    "heartRatePattern": "<analysis of HR patterns>",
                    "respiratoryPattern": "<analysis of breathing patterns>",
                    "autonomicBalance": "<sympathetic/parasympathetic balance assessment>"
                },
                "environmentalImpact": {
                    "temperatureEffect": "<impact analysis>",
                    "humidityEffect": "<impact analysis>",
                    "noiseEffect": "<impact analysis>",
                    "lightEffect": "<impact analysis>",
                    "overallSuitability": "<excellent|good|fair|poor>"
                },
                "trendAnalysis": {
                    "comparisonToPrevious": "<better|worse|similar>",
                    "weeklyTrend": "<improving|declining|stable>",
                    "notableChanges": ["<change 1>", "<change 2>"]
                },
                "riskFactors": [
                    {
                        "factor": "<identified risk>",
                        "severity": "<high|medium|low>",
                        "recommendation": "<mitigation strategy>"
                    }
                ],
                "confidenceScore": <float 0.0-1.0>,
                "analysisMetadata": {
                    "dataQuality": "<excellent|good|fair|poor>",
                    "missingDataPoints": ["<missing metric 1>", "<missing metric 2>"],
                    "analysisVersion": "2.0"
                }
            }
            ```
            
            Ensure all numeric scores are integers between 0-100, and all assessments are evidence-based and actionable.
            Focus on practical, implementable recommendations tailored to the user's specific sleep patterns.
            """,
            // Sleep Duration and Efficiency
            sleepData.get("totalSleepMinutes"),
            (Integer) sleepData.get("totalSleepMinutes") / 60.0,
            sleepData.get("sleepInBedMinutes"),
            sleepData.get("sleepEfficiency"),
            sleepData.getOrDefault("sleepLatency", 15),
            sleepData.get("wakeupCount"),
            // Sleep Architecture
            sleepData.get("deepSleepMinutes"),
            sleepData.get("deepSleepRatio"),
            sleepData.get("lightSleepMinutes"),
            sleepData.get("lightSleepRatio"),
            sleepData.get("remSleepMinutes"),
            sleepData.get("remSleepRatio"),
            // Physiological Metrics
            sleepData.getOrDefault("avgHeartRate", 65.0),
            sleepData.getOrDefault("heartRateVariability", 50.0),
            sleepData.getOrDefault("avgRespiratoryRate", 16.0),
            sleepData.getOrDefault("movementIntensity", 2.0),
            // Environmental Factors
            sleepData.get("temperature"),
            sleepData.get("humidity"),
            sleepData.get("noiseLevel"),
            sleepData.get("lightLevel"),
            // Context
            sleepData.getOrDefault("sleepGoal", 480),
            sleepData.getOrDefault("previousScore", 75.0),
            sleepData.getOrDefault("weeklyAverageScore", 72.0)
        );
    }

    /**
     * 빠른 요약 분석을 위한 간단한 프롬프트
     */
    public String buildQuickSummaryPrompt(Map<String, Object> sleepData) {
        return String.format("""
            Provide a quick sleep quality assessment based on:
            - Total sleep: %d minutes
            - Sleep efficiency: %.1f%%
            - Deep sleep: %.1f%%
            - Awakenings: %d
            
            Return a brief JSON with:
            {
                "score": <0-100>,
                "summary": "<one sentence assessment>",
                "topRecommendation": "<most important action>"
            }
            """,
            sleepData.get("totalSleepMinutes"),
            sleepData.get("sleepEfficiency"),
            sleepData.get("deepSleepRatio"),
            sleepData.get("wakeupCount")
        );
    }

    /**
     * 주간 트렌드 분석을 위한 프롬프트
     */
    public String buildWeeklyTrendPrompt(Map<String, Object> weeklyData) {
        return """
            Analyze the weekly sleep trend data and identify patterns.
            Focus on:
            1. Consistency in sleep schedule
            2. Weekend vs weekday differences
            3. Progressive improvement or decline
            4. Correlation with environmental factors
            
            Provide actionable insights for improving sleep consistency.
            """;
    }
}