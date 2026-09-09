package com.sleepwell.sleepwell_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.SleepAnalysis;
import com.sleepwell.sleepwell_backend.entity.SleepDiary;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.repository.SleepDiaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * AI 기반 수면 분석 서비스
 * 
 * 최신 AI 모델(OpenAI GPT, Anthropic Claude)을 활용하여 
 * 사용자의 수면 데이터를 심층 분석하고 개인화된 인사이트를 제공합니다.
 * 
 * 주요 분석 기능:
 * - 수면 패턴 및 품질 종합 분석
 * - 개인화된 수면 개선 권장사항 생성
 * - 수면 장애 위험 요소 식별
 * - 생활 습관과 수면의 상관관계 분석
 * - 장기 수면 트렌드 예측
 * 
 * AI 모델 활용:
 * - GPT-4: 복합적 패턴 분석 및 권장사항 생성
 * - Claude: 의료진 수준의 수면 해석
 * - 프롬프트 엔지니어링을 통한 정확도 향상
 * 
 * 성능 최적화:
 * - Spring Cache를 통한 분석 결과 캐싱
 * - 비동기 처리로 응답 속도 개선
 * - 배치 분석을 통한 리소스 효율성
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepAnalysis
 * @see SpringAIChatService
 */
@Service
@Slf4j
public class AISleepAnalysisService {

    private final ChatClient sleepAnalysisChatClient;
    private final ObjectMapper objectMapper;
    private final SleepAnalysisPromptTemplate promptTemplate;
    private final SleepRecordRepository sleepRecordRepository;
    private final SleepDiaryRepository sleepDiaryRepository;

    @Value("${ai.analysis.enabled:false}")
    private boolean aiAnalysisEnabled;

    /**
     * 생성자 기반 의존성 주입 (Spring AI 베스트 프랙티스)
     * ChatClient.builder()를 사용하여 수면 분석용 ChatClient 생성
     */
    public AISleepAnalysisService(
            OpenAiChatModel openAiChatModel,
            ObjectMapper objectMapper,
            SleepAnalysisPromptTemplate promptTemplate,
            SleepRecordRepository sleepRecordRepository,
            SleepDiaryRepository sleepDiaryRepository) {

        this.objectMapper = objectMapper;
        this.promptTemplate = promptTemplate;
        this.sleepRecordRepository = sleepRecordRepository;
        this.sleepDiaryRepository = sleepDiaryRepository;

        // Spring AI ChatClient 패턴: yml 기본 설정 + 용도별 최적화
        // 수면 분석: JSON 정확도 우선 (temperature 0.2, max-tokens 700)
        this.sleepAnalysisChatClient = ChatClient.builder(openAiChatModel)
                .defaultOptions(org.springframework.ai.openai.OpenAiChatOptions.builder()
                        .temperature(0.2)    // 의료 정확도 (2025 연구: 0.2 권장)
                        .maxTokens(700)      // JSON 구조 + 40% 안전 버퍼
                        .build())
                .defaultSystem(SLEEP_ANALYSIS_SYSTEM_PROMPT)
                .build();

        log.info("AISleepAnalysisService 초기화 완료 - Spring AI ChatClient 패턴 적용");
    }

    /**
     * AI 기반 종합 수면 분석 수행
     * AI 팀원이 개발한 프롬프트 템플릿을 활용
     */
    @Async
    public CompletableFuture<SleepAnalysis> performAIAnalysis(SleepRecord sleepRecord) {
        if (!aiAnalysisEnabled) {
            log.info("AI analysis is disabled, skipping for sleep record: {}", sleepRecord.getId());
            return CompletableFuture.completedFuture(createBasicAnalysis(sleepRecord));
        }

        try {
            // 1. 수면 데이터 전처리
            Map<String, Object> sleepData = prepareSleepDataForAI(sleepRecord);
            
            // 2. AI 프롬프트 생성 (AI 팀원과 협업하여 최적화)
            String prompt = buildSleepAnalysisPrompt(sleepData);
            
            // 3. AI 모델 호출 (선호하는 모델 선택)
            String aiResponse = callAIModel(prompt);
            
            // 4. AI 응답 파싱 및 분석 결과 생성
            SleepAnalysis analysis = parseAIResponse(sleepRecord, aiResponse);
            
            log.info("AI analysis completed for sleep record: {}", sleepRecord.getId());
            return CompletableFuture.completedFuture(analysis);
            
        } catch (Exception e) {
            log.error("AI analysis failed for sleep record: {}, error: {}", sleepRecord.getId(), e.getMessage());
            // AI 분석 실패 시 기본 분석으로 폴백
            return CompletableFuture.completedFuture(createBasicAnalysis(sleepRecord));
        }
    }

    /**
     * 수면 데이터를 AI 분석에 적합한 형태로 전처리
     */
    private Map<String, Object> prepareSleepDataForAI(SleepRecord sleepRecord) {
        Map<String, Object> data = new HashMap<>();
        
        // 기본 수면 메트릭
        data.put("totalSleepMinutes", sleepRecord.getTotalSleepMinutes());
        data.put("deepSleepMinutes", sleepRecord.getDeepSleepMinutes());
        data.put("lightSleepMinutes", sleepRecord.getLightSleepMinutes());
        data.put("remSleepMinutes", sleepRecord.getRemSleepMinutes());
        data.put("sleepInBedMinutes", sleepRecord.getSleepInBedMinutes());
        data.put("sleepAwakeMinutes", sleepRecord.getSleepAwakeMinutes());
        data.put("wakeupCount", sleepRecord.getWakeupCount());
        
        // 계산된 비율
        data.put("sleepEfficiency", sleepRecord.calculateSleepEfficiency());
        data.put("deepSleepRatio", sleepRecord.calculateDeepSleepRatio());
        data.put("lightSleepRatio", sleepRecord.calculateLightSleepRatio());
        data.put("remSleepRatio", sleepRecord.calculateRemSleepRatio());
        
        // 환경 요인
        data.put("temperature", sleepRecord.getTemperature());
        data.put("humidity", sleepRecord.getHumidity());
        data.put("noiseLevel", sleepRecord.getNoiseLevel());
        data.put("lightLevel", sleepRecord.getLightLevel());
        
        // 건강 데이터 (JSON 파싱)
        data.put("heartRateData", sleepRecord.getHeartRateData());
        data.put("respiratoryRateData", sleepRecord.getRespiratoryRateData());
        
        return data;
    }

    /**
     * 최적화된 수면 분석 프롬프트 생성
     * SleepAnalysisPromptTemplate를 사용하여 일관된 고품질 프롬프트 생성
     */
    private String buildSleepAnalysisPrompt(Map<String, Object> sleepData) {
        // 최적화된 프롬프트 템플릿 사용
        return promptTemplate.buildComprehensiveAnalysisPrompt(sleepData);
    }
    
    /**
     * 기존 프롬프트 (호환성을 위해 유지)
     * @deprecated Use promptTemplate.buildComprehensiveAnalysisPrompt instead
     */
    @Deprecated
    private String buildLegacySleepAnalysisPrompt(Map<String, Object> sleepData) {
        return String.format("""
            당신은 전문 수면 분석가입니다. 다음 수면 데이터를 종합적으로 분석하여 정확하고 개인화된 분석 결과를 제공해주세요.
            
            📊 수면 데이터:
            - 총 수면시간: %d분 (%.1f시간)
            - 깊은 수면: %d분 (%.1f%%)
            - 얕은 수면: %d분 (%.1f%%)
            - REM 수면: %d분 (%.1f%%)
            - 수면 효율성: %.1f%%
            - 중간 깨는 횟수: %d회
            - 침대에 있던 시간: %d분
            
            🌡️ 환경 요인:
            - 온도: %.1f°C
            - 습도: %.1f%%
            - 소음 수준: %.1f dB
            - 조도: %.1f lux
            
            다음 JSON 형식으로 분석 결과를 제공해주세요:
            {
                "overallScore": <0-100 점수>,
                "sleepQualityLevel": "<excellent|good|fair|poor>",
                "keyInsights": [
                    "<주요 발견사항 1>",
                    "<주요 발견사항 2>",
                    "<주요 발견사항 3>"
                ],
                "recommendations": [
                    "<개선 권장사항 1>",
                    "<개선 권장사항 2>",
                    "<개선 권장사항 3>"
                ],
                "sleepStageAnalysis": {
                    "deepSleepAssessment": "<깊은 수면 평가>",
                    "remSleepAssessment": "<REM 수면 평가>",
                    "lightSleepAssessment": "<얕은 수면 평가>"
                },
                "environmentalFactors": "<환경 요인 영향 분석>",
                "confidenceScore": <0.0-1.0 신뢰도>
            }
            """, 
            sleepData.get("totalSleepMinutes"), 
            (Integer) sleepData.get("totalSleepMinutes") / 60.0,
            sleepData.get("deepSleepMinutes"), 
            sleepData.get("deepSleepRatio"),
            sleepData.get("lightSleepMinutes"), 
            sleepData.get("lightSleepRatio"),
            sleepData.get("remSleepMinutes"), 
            sleepData.get("remSleepRatio"),
            sleepData.get("sleepEfficiency"),
            sleepData.get("wakeupCount"),
            sleepData.get("sleepInBedMinutes"),
            sleepData.get("temperature"),
            sleepData.get("humidity"),
            sleepData.get("noiseLevel"),
            sleepData.get("lightLevel")
        );
    }

    /**
     * AI 모델 호출 (Spring AI ChatClient 패턴)
     * yml 설정이 자동으로 적용됨 (model, temperature, max-tokens, top-p)
     */
    @Cacheable(value = "aiAnalysis", key = "#prompt.hashCode()")
    private String callAIModel(String prompt) throws Exception {
        try {
            return sleepAnalysisChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI model call failed: {}", e.getMessage(), e);
            throw new IllegalStateException("AI 분석 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 수면 분석 시스템 프롬프트 (Prompt Caching용)
     * OpenAI가 자동으로 캐싱하여 80% 속도 향상 및 비용 절감
     */
    private static final String SLEEP_ANALYSIS_SYSTEM_PROMPT = """
        당신은 전문 수면 분석가입니다. 다음 수면 데이터를 종합적으로 분석하여 정확하고 개인화된 분석 결과를 제공해주세요.

        [분석 지침]
        1. 수면 패턴 종합 평가
           - 총 수면시간, 수면 효율성, 수면 단계별 분포를 종합적으로 평가
           - 연령대별 정상 범위와 비교하여 평가

        2. 수면 단계별 상세 분석
           - 깊은 수면: 신체 회복과 면역력 향상에 중요 (15-20%가 이상적)
           - REM 수면: 기억 정리와 학습 능력 향상 (20-25%가 이상적)
           - 얕은 수면: 전체 수면의 안정성 평가 (50-60%가 정상)

        3. 수면 방해 요인 분석
           - 중간 깨는 횟수와 영향
           - 환경 요인(온도, 습도, 소음, 조도)의 영향
           - 생활 습관 관련 요인

        4. 개인화된 권장사항
           - 가장 효과적인 개선 방안부터 우선순위 부여
           - 실천 가능하고 구체적인 조언 제공
           - 단계별 실행 계획 포함

        [출력 형식]
        반드시 다음 JSON 형식으로만 응답하세요:

        {
            "overallScore": <0-100 사이의 종합 점수>,
            "sleepQualityLevel": "<excellent|good|fair|poor 중 하나>",
            "keyInsights": [
                "<주요 발견사항 1: 가장 중요한 발견>",
                "<주요 발견사항 2: 두 번째로 중요한 발견>",
                "<주요 발견사항 3: 세 번째로 중요한 발견>"
            ],
            "recommendations": [
                "<개선 권장사항 1: 가장 효과적인 방법>",
                "<개선 권장사항 2: 두 번째로 효과적인 방법>",
                "<개선 권장사항 3: 세 번째로 효과적인 방법>"
            ],
            "sleepStageAnalysis": {
                "deepSleepAssessment": "<깊은 수면 평가 및 영향>",
                "remSleepAssessment": "<REM 수면 평가 및 영향>",
                "lightSleepAssessment": "<얕은 수면 평가 및 영향>"
            },
            "environmentalFactors": "<환경 요인이 수면에 미친 영향 분석>",
            "confidenceScore": <0.0-1.0 사이의 분석 신뢰도>
        }

        [주의사항]
        - 의학적 진단은 하지 말고, 일반적인 수면 패턴 분석에 집중
        - 심각한 수면 장애가 의심되면 전문의 상담 권유
        - 긍정적이면서도 현실적인 조언 제공
        - 모든 응답은 한국어로 작성
        """;


    /**
     * AI 응답을 SleepAnalysis 엔티티로 파싱
     */
    private SleepAnalysis parseAIResponse(SleepRecord sleepRecord, String aiResponse) throws Exception {
        JsonNode analysisJson = objectMapper.readTree(aiResponse);
        
        return SleepAnalysis.builder()
            .sleepRecord(sleepRecord)
            .analysisDate(LocalDate.now())
            .overallScore(analysisJson.path("overallScore").asInt())
            .sleepQuality(analysisJson.path("sleepQualityLevel").asText())
            .insights(objectMapper.writeValueAsString(analysisJson.path("keyInsights")))
            .recommendations(objectMapper.writeValueAsString(analysisJson.path("recommendations")))
            .details(aiResponse) // 전체 AI 응답 저장
            .aiGenerated(true)
                            .confidenceScore(BigDecimal.valueOf(analysisJson.path("confidenceScore").asDouble()))
            .build();
    }

    /**
     * AI 분석 실패 시 기본 분석 생성
     */
    private SleepAnalysis createBasicAnalysis(SleepRecord sleepRecord) {
        return SleepAnalysis.builder()
            .sleepRecord(sleepRecord)
            .analysisDate(LocalDate.now())
            .overallScore(calculateBasicScore(sleepRecord))
            .sleepQuality(determineBasicQuality(sleepRecord))
            .insights("[]") // 빈 배열
            .recommendations("[]") // 빈 배열
            .details("기본 분석 결과")
            .aiGenerated(false)
                            .confidenceScore(BigDecimal.valueOf(0.7))
            .build();
    }

    private Integer calculateBasicScore(SleepRecord sleepRecord) {
        // 종합적인 수면 점수 계산 로직
        double efficiency = sleepRecord.calculateSleepEfficiency();
        double deepSleepRatio = sleepRecord.calculateDeepSleepRatio();
        int wakeupCount = sleepRecord.getWakeupCount() != null ? sleepRecord.getWakeupCount() : 0;
        
        // 기본 점수 (효율성 기반)
        double baseScore = efficiency;
        
        // 깊은 수면 비율 보너스/페널티 (이상적: 15-20%)
        if (deepSleepRatio >= 15 && deepSleepRatio <= 25) {
            baseScore += 5; // 보너스
        } else if (deepSleepRatio < 10) {
            baseScore -= 10; // 페널티
        }
        
        // 각성 횟수 페널티
        if (wakeupCount > 3) {
            baseScore -= (wakeupCount - 3) * 5; // 3회 초과시 회당 5점 감점
        }
        
        // 총 수면시간 고려 (7-9시간이 이상적)
        int totalHours = sleepRecord.getTotalSleepMinutes() / 60;
        if (totalHours < 6) {
            baseScore -= 15; // 수면 부족 페널티
        } else if (totalHours > 10) {
            baseScore -= 10; // 과수면 페널티
        }
        
        return (int) Math.max(0, Math.min(100, baseScore));
    }

    private String determineBasicQuality(SleepRecord sleepRecord) {
        double efficiency = sleepRecord.calculateSleepEfficiency();
        if (efficiency >= 85) return "good";
        else if (efficiency >= 75) return "fair";
        else return "poor";
    }

    /**
     * AI 기반 상담 응답 생성
     * 사용자 메시지에 대한 개인화된 수면 상담 응답을 생성합니다.
     */
    public String generateConsultationResponse(String userMessage, Object sessionContext) {
        if (!aiAnalysisEnabled) {
            return "현재 AI 상담 서비스를 준비 중입니다. 잠시 후 다시 시도해주세요.";
        }

        try {
            String prompt = buildConsultationPrompt(userMessage, sessionContext);
            return callAIModel(prompt);
        } catch (Exception e) {
            log.error("Failed to generate consultation response", e);
            return generateFallbackResponse(userMessage);
        }
    }

    /**
     * 상담 세션 요약 생성
     */
    public String generateSessionSummary(Object messages) {
        if (!aiAnalysisEnabled) {
            return "AI 요약 서비스를 준비 중입니다.";
        }

        try {
            String prompt = buildSummaryPrompt(messages);
            return callAIModel(prompt);
        } catch (Exception e) {
            log.error("Failed to generate session summary", e);
            return "상담 세션이 완료되었습니다. 요약 생성 중 오류가 발생했습니다.";
        }
    }

    /**
     * 상담 프롬프트 생성
     */
    private String buildConsultationPrompt(String userMessage, Object sessionContext) {
        return String.format("""
            당신은 전문 수면 상담사입니다. 사용자의 메시지에 대해 따뜻하고 전문적인 상담 응답을 제공해주세요.
            
            사용자 메시지: "%s"
            
            다음 가이드라인을 따라 응답해주세요:
            1. 공감하고 이해하는 어조 사용
            2. 구체적이고 실행 가능한 조언 제공
            3. 필요시 추가 질문으로 상담 진행
            4. 의료적 조언이 필요한 경우 전문의 상담 권유
            5. 응답은 2-3문단, 200자 내외로 작성
            
            전문적이면서도 친근한 상담 응답을 작성해주세요.
            """, userMessage);
    }

    /**
     * 요약 프롬프트 생성
     */
    private String buildSummaryPrompt(Object messages) {
        return """
            다음 상담 세션의 대화 내용을 요약해주세요:
            
            주요 논의 사항, 제공된 조언, 후속 조치 사항을 포함하여 
            간결하고 유용한 요약을 작성해주세요.
            """;
    }

    /**
     * 폴백 응답 생성
     */
    private String generateFallbackResponse(String userMessage) {
        if (userMessage.contains("잠") || userMessage.contains("수면")) {
            return "수면과 관련된 고민을 말씀해주셔서 감사합니다. 규칙적인 수면 패턴과 좋은 수면 환경 조성이 중요합니다. 더 구체적인 상황을 알려주시면 더 도움이 되는 조언을 드릴 수 있습니다.";
        }
        
        return "말씀해주신 내용을 잘 이해했습니다. 수면 건강과 관련하여 어떤 부분이 가장 궁금하신지 더 자세히 알려주시겠어요?";
    }

    // ========== 수면일지 통합 분석 기능 (2025-01 추가) ==========

    /**
     * 수면일지와 수면기록을 통합한 AI 분석 수행
     *
     * 객관적 데이터(SleepRecord)와 주관적 데이터(SleepDiary)를 결합하여
     * 더욱 정확하고 개인화된 수면 분석을 제공합니다.
     *
     * @param sleepDiary 사용자의 주관적 수면일지
     * @return AI 통합 분석 결과 (비동기)
     */
    @Async
    public CompletableFuture<SleepAnalysis> performIntegratedAnalysis(SleepDiary sleepDiary) {
        if (!aiAnalysisEnabled) {
            log.info("AI analysis is disabled, skipping integrated analysis for sleep diary: {}", sleepDiary.getId());
            return CompletableFuture.completedFuture(createBasicDiaryAnalysis(sleepDiary));
        }

        try {
            // 1. 해당 날짜의 SleepRecord 조회 (Optional 패턴)
            Optional<SleepRecord> sleepRecord = sleepRecordRepository
                    .findByUserAndRecordDate(sleepDiary.getUser(), sleepDiary.getDiaryDate());

            // 2. 통합 분석 데이터 준비
            Map<String, Object> analysisData = prepareIntegratedAnalysisData(sleepDiary, sleepRecord);

            // 3. AI 프롬프트 생성
            String prompt = buildIntegratedAnalysisPrompt(analysisData);

            // 4. AI 모델 호출 (기존 메서드 재사용)
            String aiResponse = callAIModel(prompt);

            // 5. AI 응답 파싱 및 분석 결과 생성
            SleepAnalysis analysis = parseIntegratedAIResponse(sleepDiary, sleepRecord.orElse(null), aiResponse);

            log.info("Integrated AI analysis completed for sleep diary: {}", sleepDiary.getId());
            return CompletableFuture.completedFuture(analysis);

        } catch (Exception e) {
            log.error("Integrated AI analysis failed for sleep diary: {}, error: {}",
                    sleepDiary.getId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(createBasicDiaryAnalysis(sleepDiary));
        }
    }

    /**
     * 수면일지 기반 개인화된 상담 응답 생성
     *
     * 사용자의 최근 수면일지 데이터를 맥락으로 활용하여
     * 더욱 개인화된 상담 응답을 제공합니다.
     *
     * @param userMessage 사용자 질문
     * @param recentDiary 최근 수면일지
     * @param weeklyDiaries 주간 수면일지 목록 (선택사항)
     * @return 개인화된 상담 응답
     */
    public String generateDiaryBasedConsultation(String userMessage, SleepDiary recentDiary,
                                               List<SleepDiary> weeklyDiaries) {
        if (!aiAnalysisEnabled) {
            return "현재 AI 상담 서비스를 준비 중입니다. 잠시 후 다시 시도해주세요.";
        }

        try {
            Map<String, Object> contextData = prepareDiaryConsultationContext(recentDiary, weeklyDiaries);
            String prompt = buildDiaryConsultationPrompt(userMessage, contextData);
            return callAIModel(prompt);

        } catch (Exception e) {
            log.error("Failed to generate diary-based consultation response", e);
            return generateFallbackDiaryConsultationResponse(userMessage, recentDiary);
        }
    }

    /**
     * 여러 수면일지의 생활습관 트렌드 분석
     *
     * 일정 기간의 수면일지 데이터를 분석하여
     * 생활습관 변화와 수면 품질의 상관관계를 파악합니다.
     *
     * @param sleepDiaries 분석할 수면일지 목록
     * @return 트렌드 분석 결과 (JSON 형태의 Map)
     */
    @Async
    public CompletableFuture<Map<String, Object>> analyzeLifestyleTrends(List<SleepDiary> sleepDiaries) {
        if (!aiAnalysisEnabled || sleepDiaries.isEmpty()) {
            return CompletableFuture.completedFuture(createBasicTrendAnalysis());
        }

        try {
            Map<String, Object> trendData = prepareLifestyleTrendData(sleepDiaries);
            String prompt = buildLifestyleTrendPrompt(trendData);
            String aiResponse = callAIModel(prompt);

            JsonNode responseJson = objectMapper.readTree(aiResponse);
            @SuppressWarnings("unchecked")
            Map<String, Object> analysisResult = objectMapper.convertValue(responseJson, Map.class);

            log.info("Lifestyle trend analysis completed for {} diaries", sleepDiaries.size());
            return CompletableFuture.completedFuture(analysisResult);

        } catch (Exception e) {
            log.error("Lifestyle trend analysis failed: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(createBasicTrendAnalysis());
        }
    }

    // ========== 데이터 준비 메서드들 ==========

    /**
     * 통합 분석을 위한 데이터 준비
     */
    private Map<String, Object> prepareIntegratedAnalysisData(SleepDiary sleepDiary,
                                                             Optional<SleepRecord> sleepRecord) {
        Map<String, Object> data = new HashMap<>();

        // 수면일지 데이터 (주관적)
        data.put("diary", prepareDiaryDataForAI(sleepDiary));

        // 수면기록 데이터 (객관적, 있는 경우)
        if (sleepRecord.isPresent()) {
            data.put("objectiveSleep", prepareSleepDataForAI(sleepRecord.get()));
            data.put("hasObjectiveData", true);
        } else {
            data.put("hasObjectiveData", false);
        }

        // 분석 메타데이터
        data.put("analysisDate", sleepDiary.getDiaryDate());
        data.put("analysisType", "integrated");

        return data;
    }

    /**
     * 수면일지 데이터를 AI 분석용으로 구조화
     */
    private Map<String, Object> prepareDiaryDataForAI(SleepDiary sleepDiary) {
        Map<String, Object> diaryData = new HashMap<>();

        // 기본 수면 정보
        diaryData.put("diaryDate", sleepDiary.getDiaryDate());
        diaryData.put("bedTime", sleepDiary.getBedTime());
        diaryData.put("wakeUpTime", sleepDiary.getWakeUpTime());
        diaryData.put("perceivedSleepMinutes", sleepDiary.getPerceivedSleepMinutes());

        // 계산된 메트릭 (Optional 패턴 활용)
        sleepDiary.calculateTotalBedTime().ifPresent(bedTime ->
            diaryData.put("totalBedTime", bedTime));
        sleepDiary.calculateSubjectiveSleepEfficiency().ifPresent(efficiency ->
            diaryData.put("subjectiveSleepEfficiency", efficiency));

        // 생활습관 요인
        diaryData.put("caffeineMg", sleepDiary.getCaffeineMg());
        diaryData.put("alcoholMl", sleepDiary.getAlcoholMl());
        diaryData.put("didExercise", sleepDiary.getDidExercise());
        diaryData.put("caffeineLevel", sleepDiary.getCaffeineIntakeLevel().name());
        diaryData.put("alcoholLevel", sleepDiary.getAlcoholIntakeLevel().name());

        // 주관적 평가
        diaryData.put("subjectiveSleepQuality", sleepDiary.getSubjectiveSleepQuality());
        diaryData.put("morningConditionScore", sleepDiary.getMorningConditionScore());
        diaryData.put("stressLevel", sleepDiary.getStressLevel());

        // 계산된 점수
        diaryData.put("lifestyleScore", sleepDiary.calculateLifestyleScore());

        return diaryData;
    }

    /**
     * 생활습관 트렌드 분석을 위한 데이터 준비
     */
    private Map<String, Object> prepareLifestyleTrendData(List<SleepDiary> sleepDiaries) {
        Map<String, Object> trendData = new HashMap<>();

        // 기본 통계
        trendData.put("totalDays", sleepDiaries.size());
        trendData.put("startDate", sleepDiaries.get(0).getDiaryDate());
        trendData.put("endDate", sleepDiaries.get(sleepDiaries.size() - 1).getDiaryDate());

        // 평균값 계산
        double avgSleepQuality = sleepDiaries.stream()
            .filter(d -> d.getSubjectiveSleepQuality() != null)
            .mapToInt(SleepDiary::getSubjectiveSleepQuality)
            .average().orElse(0.0);

        double avgLifestyleScore = sleepDiaries.stream()
            .mapToDouble(SleepDiary::calculateLifestyleScore)
            .average().orElse(0.0);

        trendData.put("avgSleepQuality", avgSleepQuality);
        trendData.put("avgLifestyleScore", avgLifestyleScore);

        // 생활습관 패턴
        long exerciseDays = sleepDiaries.stream()
            .filter(d -> Boolean.TRUE.equals(d.getDidExercise()))
            .count();

        double avgCaffeine = sleepDiaries.stream()
            .filter(d -> d.getCaffeineMg() != null)
            .mapToInt(SleepDiary::getCaffeineMg)
            .average().orElse(0.0);

        trendData.put("exerciseFrequency", (double) exerciseDays / sleepDiaries.size() * 100);
        trendData.put("avgCaffeineIntake", avgCaffeine);

        return trendData;
    }

    /**
     * 수면일지 기반 상담 컨텍스트 준비
     */
    private Map<String, Object> prepareDiaryConsultationContext(SleepDiary recentDiary,
                                                               List<SleepDiary> weeklyDiaries) {
        Map<String, Object> context = new HashMap<>();

        context.put("recentDiary", prepareDiaryDataForAI(recentDiary));

        if (weeklyDiaries != null && !weeklyDiaries.isEmpty()) {
            double weeklyAvgQuality = weeklyDiaries.stream()
                .filter(d -> d.getSubjectiveSleepQuality() != null)
                .mapToInt(SleepDiary::getSubjectiveSleepQuality)
                .average().orElse(0.0);

            context.put("weeklyAvgSleepQuality", weeklyAvgQuality);
            context.put("weeklyDiariesCount", weeklyDiaries.size());
        }

        return context;
    }

    // ========== 프롬프트 생성 메서드들 ==========

    /**
     * 통합 분석을 위한 AI 프롬프트 생성
     */
    private String buildIntegratedAnalysisPrompt(Map<String, Object> analysisData) {
        // SleepAnalysisPromptTemplate에 새 메서드 추가 필요
        // 일단 기본적인 프롬프트로 구현
        @SuppressWarnings("unchecked")
        Map<String, Object> diaryData = (Map<String, Object>) analysisData.get("diary");
        boolean hasObjectiveData = (Boolean) analysisData.get("hasObjectiveData");

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("""
            당신은 수면 전문의이자 행동 수면의학 전문가입니다.
            사용자의 주관적 수면일지와 객관적 수면 데이터를 통합 분석하여 포괄적인 수면 인사이트를 제공해주세요.

            ## 📔 수면일지 데이터 (주관적)

            ### 수면 시간 정보
            - 잠자리에 든 시간: %s
            - 일어난 시간: %s
            - 주관적 수면시간: %d분 (%.1f시간)
            - 침상에서 보낸 시간: %s분
            - 주관적 수면효율성: %s%%

            ### 생활습관 요인
            - 카페인 섭취: %smg (%s)
            - 알코올 섭취: %sml (%s)
            - 운동 여부: %s
            - 생활습관 점수: %.1f/100

            ### 주관적 평가
            - 수면 품질 점수: %s/10
            - 아침 컨디션 점수: %s/10
            - 스트레스 수준: %s/10
            """.formatted(
            diaryData.get("bedTime"),
            diaryData.get("wakeUpTime"),
            diaryData.get("perceivedSleepMinutes"),
            (Integer) diaryData.get("perceivedSleepMinutes") / 60.0,
            diaryData.get("totalBedTime"),
            diaryData.get("subjectiveSleepEfficiency"),
            diaryData.get("caffeineMg"),
            diaryData.get("caffeineLevel"),
            diaryData.get("alcoholMl"),
            diaryData.get("alcoholLevel"),
            Boolean.TRUE.equals(diaryData.get("didExercise")) ? "함" : "안함",
            diaryData.get("lifestyleScore"),
            diaryData.get("subjectiveSleepQuality"),
            diaryData.get("morningConditionScore"),
            diaryData.get("stressLevel")
        ));

        if (hasObjectiveData) {
            @SuppressWarnings("unchecked")
            Map<String, Object> objectiveData = (Map<String, Object>) analysisData.get("objectiveSleep");
            promptBuilder.append("""

                ## 📊 객관적 수면 데이터 (웨어러블 기기)

                ### 실제 수면 구조
                - 총 수면시간: %d분 (%.1f시간)
                - 깊은 수면: %d분 (%.1f%%)
                - 얕은 수면: %d분 (%.1f%%)
                - REM 수면: %d분 (%.1f%%)
                - 객관적 수면효율성: %.1f%%
                - 중간 깨는 횟수: %d회
                """.formatted(
                objectiveData.get("totalSleepMinutes"),
                (Integer) objectiveData.get("totalSleepMinutes") / 60.0,
                objectiveData.get("deepSleepMinutes"),
                objectiveData.get("deepSleepRatio"),
                objectiveData.get("lightSleepMinutes"),
                objectiveData.get("lightSleepRatio"),
                objectiveData.get("remSleepMinutes"),
                objectiveData.get("remSleepRatio"),
                objectiveData.get("sleepEfficiency"),
                objectiveData.get("wakeupCount")
            ));
        }

        promptBuilder.append("""

            ## 분석 요구사항

            다음 JSON 형식으로 통합 분석 결과를 제공해주세요:

            {
                "overallScore": <0-100 점수>,
                "sleepQualityLevel": "<excellent|good|fair|poor>",
                "keyInsights": [
                    {
                        "category": "<subjective_vs_objective|lifestyle_impact|sleep_architecture>",
                        "finding": "<구체적 발견사항>",
                        "importance": "<high|medium|low>"
                    }
                ],
                "recommendations": [
                    {
                        "priority": "<high|medium|low>",
                        "category": "<lifestyle|environment|sleep_hygiene>",
                        "action": "<구체적 실행방안>",
                        "rationale": "<권장 이유>"
                    }
                ],
                "lifestyleAnalysis": {
                    "caffeineImpact": "<카페인이 수면에 미친 영향 분석>",
                    "alcoholImpact": "<알코올이 수면에 미친 영향 분석>",
                    "exerciseImpact": "<운동이 수면에 미친 영향 분석>",
                    "stressImpact": "<스트레스가 수면에 미친 영향 분석>"
                },
                "confidenceScore": <0.0-1.0 신뢰도>
            }

            특히 주관적 인식과 객관적 데이터 간의 차이점과 생활습관이 실제 수면에 미친 영향을 중점 분석해주세요.
            """);

        return promptBuilder.toString();
    }

    /**
     * 수면일지 기반 상담 프롬프트 생성
     */
    private String buildDiaryConsultationPrompt(String userMessage, Map<String, Object> contextData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> recentDiary = (Map<String, Object>) contextData.get("recentDiary");

        return String.format("""
            당신은 친근하고 전문적인 수면 상담사입니다. 사용자의 최근 수면일지 데이터를 바탕으로 개인화된 상담을 제공해주세요.

            ## 사용자 메시지
            "%s"

            ## 최근 수면일지 정보
            - 수면 품질: %s/10
            - 아침 컨디션: %s/10
            - 생활습관 점수: %.1f/100
            - 카페인 섭취: %smg
            - 운동 여부: %s
            - 스트레스 수준: %s/10

            다음 가이드라인을 따라 상담 응답을 작성해주세요:
            1. 사용자의 수면일지 데이터를 구체적으로 언급
            2. 공감하고 이해하는 어조로 응답
            3. 개인화된 실용적 조언 제공
            4. 필요시 추가 질문으로 상담 심화
            5. 응답은 3-4문장, 250자 내외

            개인의 수면 패턴과 생활습관을 고려한 맞춤형 조언을 제공해주세요.
            """,
            userMessage,
            recentDiary.get("subjectiveSleepQuality"),
            recentDiary.get("morningConditionScore"),
            recentDiary.get("lifestyleScore"),
            recentDiary.get("caffeineMg"),
            Boolean.TRUE.equals(recentDiary.get("didExercise")) ? "함" : "안함",
            recentDiary.get("stressLevel")
        );
    }

    /**
     * 생활습관 트렌드 분석을 위한 프롬프트 생성
     */
    private String buildLifestyleTrendPrompt(Map<String, Object> trendData) {
        return String.format("""
            %d일간의 수면일지 데이터를 바탕으로 생활습관과 수면 품질의 상관관계를 분석해주세요.

            ## 📈 트렌드 데이터 요약
            - 분석 기간: %s ~ %s (%d일)
            - 평균 수면 품질: %.1f/10
            - 평균 생활습관 점수: %.1f/100
            - 운동 실시율: %.1f%%
            - 평균 카페인 섭취량: %.1fmg

            다음 JSON 형식으로 트렌드 분석 결과를 제공해주세요:

            {
                "trendSummary": {
                    "overallTrend": "<improving|stable|declining>",
                    "keyPatterns": ["<패턴 1>", "<패턴 2>", "<패턴 3>"]
                },
                "lifestyleCorrelations": {
                    "exerciseCorrelation": {
                        "strength": "<strong|moderate|weak|none>",
                        "direction": "<positive|negative>",
                        "insight": "<분석 결과>"
                    },
                    "caffeineCorrelation": {
                        "strength": "<strong|moderate|weak|none>",
                        "direction": "<positive|negative>",
                        "insight": "<분석 결과>"
                    }
                },
                "recommendations": [
                    {
                        "category": "<consistency|lifestyle|environment>",
                        "priority": "<high|medium|low>",
                        "suggestion": "<구체적 개선방안>"
                    }
                ]
            }
            """,
            trendData.get("totalDays"),
            trendData.get("startDate"),
            trendData.get("endDate"),
            trendData.get("totalDays"),
            trendData.get("avgSleepQuality"),
            trendData.get("avgLifestyleScore"),
            trendData.get("exerciseFrequency"),
            trendData.get("avgCaffeineIntake")
        );
    }

    // ========== 응답 파싱 및 기본 분석 메서드들 ==========

    /**
     * AI 응답을 SleepAnalysis 엔티티로 파싱 (통합 분석용)
     */
    private SleepAnalysis parseIntegratedAIResponse(SleepDiary sleepDiary, SleepRecord sleepRecord,
                                                   String aiResponse) throws Exception {
        JsonNode analysisJson = objectMapper.readTree(aiResponse);

        return SleepAnalysis.builder()
            .sleepRecord(sleepRecord) // null일 수 있음 (일지만 있는 경우)
            .analysisDate(sleepDiary.getDiaryDate())
            .overallScore(analysisJson.path("overallScore").asInt())
            .sleepQuality(analysisJson.path("sleepQualityLevel").asText())
            .insights(objectMapper.writeValueAsString(analysisJson.path("keyInsights")))
            .recommendations(objectMapper.writeValueAsString(analysisJson.path("recommendations")))
            .details(aiResponse)
            .aiGenerated(true)
            .confidenceScore(BigDecimal.valueOf(analysisJson.path("confidenceScore").asDouble()))
            .build();
    }

    /**
     * AI 분석 실패 시 기본 수면일지 분석 생성
     */
    private SleepAnalysis createBasicDiaryAnalysis(SleepDiary sleepDiary) {
        return SleepAnalysis.builder()
            .sleepRecord(null)
            .analysisDate(sleepDiary.getDiaryDate())
            .overallScore(calculateBasicDiaryScore(sleepDiary))
            .sleepQuality(determineBasicDiaryQuality(sleepDiary))
            .insights("[]")
            .recommendations("[]")
            .details("수면일지 기반 기본 분석 결과")
            .aiGenerated(false)
            .confidenceScore(BigDecimal.valueOf(0.6))
            .build();
    }

    private Integer calculateBasicDiaryScore(SleepDiary sleepDiary) {
        double score = 50.0; // 기본 점수

        // 주관적 수면 품질 반영 (40점 만점)
        if (sleepDiary.getSubjectiveSleepQuality() != null) {
            score += sleepDiary.getSubjectiveSleepQuality() * 4.0;
        }

        // 생활습관 점수 반영 (30% 가중치)
        score += sleepDiary.calculateLifestyleScore() * 0.3;

        // 아침 컨디션 반영 (10점 만점)
        if (sleepDiary.getMorningConditionScore() != null) {
            score += sleepDiary.getMorningConditionScore();
        }

        return (int) Math.max(0, Math.min(100, score));
    }

    private String determineBasicDiaryQuality(SleepDiary sleepDiary) {
        Integer sleepQuality = sleepDiary.getSubjectiveSleepQuality();
        if (sleepQuality == null) return "fair";

        if (sleepQuality >= 8) return "excellent";
        else if (sleepQuality >= 6) return "good";
        else if (sleepQuality >= 4) return "fair";
        else return "poor";
    }

    private Map<String, Object> createBasicTrendAnalysis() {
        return Map.of(
            "message", "AI 트렌드 분석 서비스를 준비 중입니다.",
            "basicRecommendation", "규칙적인 수면 패턴과 일관된 생활습관을 유지해주세요."
        );
    }

    private String generateFallbackDiaryConsultationResponse(String userMessage, SleepDiary recentDiary) {
        if (recentDiary.getSubjectiveSleepQuality() != null &&
            recentDiary.getSubjectiveSleepQuality() < 5) {
            return "최근 수면 품질이 좋지 않으셨군요. 규칙적인 수면 시간과 편안한 수면 환경 조성이 도움이 될 것 같습니다. 더 구체적인 상황을 알려주시면 더 나은 조언을 드릴 수 있어요.";
        }

        return "수면일지를 꾸준히 작성해주셔서 감사합니다. 생활습관과 수면의 연관성을 파악하는 데 도움이 됩니다. 어떤 부분이 가장 궁금하신지 더 자세히 알려주시겠어요?";
    }

    /**
     * 수면일지와 수면기록을 통합하여 AI 분석 수행
     * 객관적 데이터와 주관적 경험을 결합하여 더 정확한 분석 제공
     *
     * @param sleepRecord 객관적 수면 데이터 (웨어러블 기기)
     * @param sleepDiary 주관적 수면일지 (사용자 작성)
     * @return AI 분석 결과
     */
    @Async
    public CompletableFuture<SleepAnalysis> performIntegratedAnalysis(SleepRecord sleepRecord, SleepDiary sleepDiary) {
        if (!aiAnalysisEnabled) {
            log.info("AI analysis is disabled, skipping integrated analysis for diary: {}", sleepDiary.getId());
            return CompletableFuture.completedFuture(createBasicDiaryAnalysis(sleepDiary));
        }

        try {
            log.info("Starting integrated AI analysis for sleep record: {} and diary: {}",
                sleepRecord.getId(), sleepDiary.getId());

            // 1. 객관적 수면 데이터 준비
            Map<String, Object> sleepData = prepareSleepDataForAI(sleepRecord);

            // 2. 주관적 수면일지 데이터 준비
            Map<String, Object> diaryData = prepareDiaryData(sleepDiary);

            // 3. 통합 AI 프롬프트 생성
            String prompt = buildIntegratedPrompt(sleepData, diaryData);

            // 4. AI 모델 호출
            String aiResponse = callAIModel(prompt);

            // 5. AI 응답 파싱 및 분석 결과 생성
            SleepAnalysis analysis = parseAIResponse(sleepRecord, aiResponse);

            log.info("Integrated AI analysis completed for sleep record: {} and diary: {}",
                sleepRecord.getId(), sleepDiary.getId());
            return CompletableFuture.completedFuture(analysis);

        } catch (Exception e) {
            log.error("Integrated AI analysis failed for sleep record: {} and diary: {}, error: {}",
                sleepRecord.getId(), sleepDiary.getId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(createBasicAnalysis(sleepRecord));
        }
    }

    /**
     * 수면일지만으로 AI 분석 수행
     * 객관적 수면 데이터가 없을 때 주관적 경험만으로 분석
     *
     * @param sleepDiary 주관적 수면일지
     * @return AI 분석 결과
     */
    @Async
    public CompletableFuture<SleepAnalysis> performDiaryOnlyAnalysis(SleepDiary sleepDiary) {
        if (!aiAnalysisEnabled) {
            log.info("AI analysis is disabled, skipping diary-only analysis for diary: {}", sleepDiary.getId());
            return CompletableFuture.completedFuture(createBasicDiaryAnalysis(sleepDiary));
        }

        try {
            log.info("Starting diary-only AI analysis for diary: {}", sleepDiary.getId());

            // 1. 수면일지 데이터 준비
            Map<String, Object> diaryData = prepareDiaryData(sleepDiary);

            // 2. 일지 전용 AI 프롬프트 생성
            String prompt = buildDiaryOnlyPrompt(diaryData);

            // 3. AI 모델 호출
            String aiResponse = callAIModel(prompt);

            // 4. AI 응답 파싱 및 분석 결과 생성
            SleepAnalysis analysis = parseDiaryOnlyAIResponse(sleepDiary, aiResponse);

            log.info("Diary-only AI analysis completed for diary: {}", sleepDiary.getId());
            return CompletableFuture.completedFuture(analysis);

        } catch (Exception e) {
            log.error("Diary-only AI analysis failed for diary: {}, error: {}",
                sleepDiary.getId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(createBasicDiaryAnalysis(sleepDiary));
        }
    }

    /**
     * 수면일지 데이터를 AI 분석에 적합한 형태로 전처리
     */
    private Map<String, Object> prepareDiaryData(SleepDiary sleepDiary) {
        Map<String, Object> data = new HashMap<>();

        // 기본 정보
        data.put("diaryDate", sleepDiary.getDiaryDate());
        data.put("bedTime", sleepDiary.getBedTime());
        data.put("wakeUpTime", sleepDiary.getWakeUpTime());
        data.put("perceivedSleepMinutes", sleepDiary.getPerceivedSleepMinutes());

        // 생활습관 요인
        data.put("caffeineMg", sleepDiary.getCaffeineMg() != null ? sleepDiary.getCaffeineMg() : 0);
        data.put("alcoholMl", sleepDiary.getAlcoholMl() != null ? sleepDiary.getAlcoholMl() : 0);
        data.put("didExercise", sleepDiary.getDidExercise() != null ? sleepDiary.getDidExercise() : false);
        data.put("caffeineLevel", sleepDiary.getCaffeineIntakeLevel().name());
        data.put("alcoholLevel", sleepDiary.getAlcoholIntakeLevel().name());

        // 주관적 평가
        data.put("subjectiveSleepQuality", sleepDiary.getSubjectiveSleepQuality() != null ?
            sleepDiary.getSubjectiveSleepQuality() : 5);
        data.put("morningConditionScore", sleepDiary.getMorningConditionScore() != null ?
            sleepDiary.getMorningConditionScore() : 5);
        data.put("stressLevel", sleepDiary.getStressLevel() != null ? sleepDiary.getStressLevel() : 5);

        // 계산된 메트릭
        data.put("lifestyleScore", sleepDiary.calculateLifestyleScore());
        sleepDiary.calculateSubjectiveSleepEfficiency().ifPresent(eff ->
            data.put("subjectiveSleepEfficiency", eff));
        sleepDiary.calculateTotalBedTime().ifPresent(time ->
            data.put("totalBedTime", time));

        return data;
    }

    /**
     * 통합 분석을 위한 AI 프롬프트 생성
     */
    private String buildIntegratedPrompt(Map<String, Object> sleepData, Map<String, Object> diaryData) {
        return String.format("""
            당신은 수면 전문의입니다. 객관적 수면 데이터와 주관적 수면일지를 통합 분석하세요.

            📊 객관적 수면 데이터 (웨어러블 기기):
            - 총 수면시간: %d분 (%.1f시간)
            - 깊은 수면: %d분 (%.1f%%)
            - 얕은 수면: %d분 (%.1f%%)
            - REM 수면: %d분 (%.1f%%)
            - 수면 효율: %.1f%%
            - 중간 깨는 횟수: %d회

            📔 주관적 수면일지:
            - 주관적 수면 품질: %d/10
            - 아침 컨디션: %d/10
            - 스트레스 수준: %d/10
            - 카페인 섭취: %dmg (%s)
            - 알코올 섭취: %dml (%s)
            - 운동 여부: %s
            - 생활습관 점수: %.1f/100

            다음 JSON 형식으로 통합 분석 결과를 제공하세요:

            ```json
            {
              "overallScore": <0-100 종합 점수>,
              "sleepQualityLevel": "<excellent|good|fair|poor>",
              "keyInsights": [
                "<주요 발견사항 1>",
                "<주요 발견사항 2>",
                "<주요 발견사항 3>"
              ],
              "recommendations": [
                "<개선 권장사항 1>",
                "<개선 권장사항 2>",
                "<개선 권장사항 3>"
              ],
              "sleepStageAnalysis": {
                "deepSleepAssessment": "<깊은 수면 평가>",
                "remSleepAssessment": "<REM 수면 평가>",
                "lightSleepAssessment": "<얕은 수면 평가>"
              },
              "lifestyleImpact": {
                "caffeineEffect": "<카페인이 수면에 미친 영향>",
                "alcoholEffect": "<알코올이 수면에 미친 영향>",
                "exerciseEffect": "<운동이 수면에 미친 영향>",
                "stressEffect": "<스트레스가 수면에 미친 영향>"
              },
              "perceptionGap": {
                "subjectiveVsObjective": "<주관적 인식 vs 객관적 데이터 비교>",
                "accuracyScore": <0.0-1.0 주관적 인식 정확도>
              },
              "confidenceScore": <0.0-1.0 분석 신뢰도>
            }
            ```

            특히 다음을 중점적으로 분석하세요:
            1. 생활습관 요인(카페인, 알코올, 운동, 스트레스)이 실제 수면 구조에 미친 영향
            2. 사용자의 주관적 인식과 객관적 데이터 간의 차이점 및 원인
            3. 우선순위별 개선 권장사항 (가장 효과적인 것부터)
            """,
            // 객관적 데이터
            sleepData.get("totalSleepMinutes"),
            (Integer) sleepData.get("totalSleepMinutes") / 60.0,
            sleepData.get("deepSleepMinutes"),
            sleepData.get("deepSleepRatio"),
            sleepData.get("lightSleepMinutes"),
            sleepData.get("lightSleepRatio"),
            sleepData.get("remSleepMinutes"),
            sleepData.get("remSleepRatio"),
            sleepData.get("sleepEfficiency"),
            sleepData.get("wakeupCount"),
            // 주관적 데이터
            diaryData.get("subjectiveSleepQuality"),
            diaryData.get("morningConditionScore"),
            diaryData.get("stressLevel"),
            diaryData.get("caffeineMg"),
            diaryData.get("caffeineLevel"),
            diaryData.get("alcoholMl"),
            diaryData.get("alcoholLevel"),
            (Boolean) diaryData.get("didExercise") ? "함" : "안함",
            diaryData.get("lifestyleScore")
        );
    }

    /**
     * 일지 전용 분석을 위한 AI 프롬프트 생성
     */
    private String buildDiaryOnlyPrompt(Map<String, Object> diaryData) {
        return String.format("""
            당신은 수면 전문의입니다. 사용자의 주관적 수면일지를 분석하세요.

            📔 수면일지 데이터:
            - 주관적 수면 품질: %d/10
            - 아침 컨디션: %d/10
            - 스트레스 수준: %d/10
            - 카페인 섭취: %dmg (%s)
            - 알코올 섭취: %dml (%s)
            - 운동 여부: %s
            - 생활습관 점수: %.1f/100

            다음 JSON 형식으로 분석 결과를 제공하세요:

            ```json
            {
              "overallScore": <0-100 종합 점수>,
              "sleepQualityLevel": "<excellent|good|fair|poor>",
              "keyInsights": [
                "<주요 발견사항 1>",
                "<주요 발견사항 2>",
                "<주요 발견사항 3>"
              ],
              "recommendations": [
                "<생활습관 개선 권장사항 1>",
                "<생활습관 개선 권장사항 2>",
                "<웨어러블 기기 사용 권장>"
              ],
              "lifestyleAnalysis": {
                "caffeineAssessment": "<카페인 섭취 평가>",
                "alcoholAssessment": "<알코올 섭취 평가>",
                "exerciseAssessment": "<운동 습관 평가>",
                "stressAssessment": "<스트레스 수준 평가>"
              },
              "limitations": "<객관적 수면 데이터 없이 분석한 한계점>",
              "confidenceScore": <0.0-1.0 분석 신뢰도>
            }
            ```

            참고사항:
            - 객관적 수면 데이터가 없으므로 주관적 평가에 의존한 분석입니다
            - 더 정확한 분석을 위해 웨어러블 기기 사용을 권장해주세요
            - 생활습관 개선에 초점을 맞춘 실용적 조언을 제공하세요
            """,
            diaryData.get("subjectiveSleepQuality"),
            diaryData.get("morningConditionScore"),
            diaryData.get("stressLevel"),
            diaryData.get("caffeineMg"),
            diaryData.get("caffeineLevel"),
            diaryData.get("alcoholMl"),
            diaryData.get("alcoholLevel"),
            (Boolean) diaryData.get("didExercise") ? "함" : "안함",
            diaryData.get("lifestyleScore")
        );
    }

    /**
     * 일지 전용 AI 응답 파싱
     */
    private SleepAnalysis parseDiaryOnlyAIResponse(SleepDiary sleepDiary, String aiResponse) throws Exception {
        JsonNode analysisJson = objectMapper.readTree(aiResponse);

        return SleepAnalysis.builder()
            .sleepRecord(null) // 일지만으로 분석이므로 SleepRecord 없음
            .analysisDate(sleepDiary.getDiaryDate())
            .overallScore(analysisJson.path("overallScore").asInt())
            .sleepQuality(analysisJson.path("sleepQualityLevel").asText())
            .insights(objectMapper.writeValueAsString(analysisJson.path("keyInsights")))
            .recommendations(objectMapper.writeValueAsString(analysisJson.path("recommendations")))
            .details(aiResponse)
            .aiGenerated(true)
            .confidenceScore(BigDecimal.valueOf(analysisJson.path("confidenceScore").asDouble()))
            .build();
    }
} 