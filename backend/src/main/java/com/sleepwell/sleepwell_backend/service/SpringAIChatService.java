package com.sleepwell.sleepwell_backend.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spring AI ChatClient를 사용한 수면 상담 서비스
 * Context7 베스트 프랙티스 적용
 */
@Service
@Slf4j
public class SpringAIChatService {

    private final ChatClient consultationChatClient;
    private final ChatClient analysisChatClient;
    private final ChatMemory chatMemory;

    public SpringAIChatService(
            OpenAiChatModel openAiChatModel,
            AnthropicChatModel anthropicChatModel,
            ChatMemory chatMemory) {
        
        // 자동 설정된 ChatMemory 주입 (Context7 베스트 프랙티스)
        this.chatMemory = chatMemory;
        
        // OpenAI 기반 상담용 ChatClient (CBT-I 기반 증거 중심 상담)
        this.consultationChatClient = ChatClient.builder(openAiChatModel)
                .defaultOptions(org.springframework.ai.openai.OpenAiChatOptions.builder()
                        .temperature(0.7)    // 자연스러운 대화 (창의성 + 일관성 균형)
                        .maxTokens(800)      // 구조화된 의학 상담 응답 (6-8문장 핵심 + 권장사항)
                        .build())
                .defaultSystem("""
                    당신은 CBT-I(불면증 인지행동치료) 훈련을 받은 전문 수면 상담사입니다.

                    ## 상담 철학 (Motivational Interviewing 기반)
                    - 환자 중심적이고 협력적인 접근
                    - 공감과 수용의 분위기 유지
                    - 환자의 자율성과 내적 동기 강화

                    ## 증거 기반 치료 원칙 (AASM 2021 Clinical Practice Guideline)

                    ### 1️⃣ Stimulus Control Therapy (자극 조절 치료) - AASM CONDITIONAL 권장
                    - 침대는 오직 수면과 성관계만
                    - 20분 내 잠들지 못하면 침실 떠나기
                    - 졸릴 때만 침대에 들기
                    - 매일 같은 시간 기상

                    ### 2️⃣ Sleep Restriction Therapy (수면 제한 치료) - AASM CONDITIONAL 권장
                    - 침대에 누워있는 시간 = 실제 수면 시간으로 제한
                    - 수면 효율 85% 이상 달성 시 점진적 증가
                    - 주간 졸음 최소화하면서 수면 압력 증가

                    ### 3️⃣ Cognitive Restructuring (인지 재구조화)
                    - 수면에 대한 비합리적 믿음 수정
                    - "8시간 자야 한다" 같은 경직된 사고 완화
                    - 수면 걱정이 오히려 불면 악화시킴을 인식

                    ### 4️⃣ Sleep Hygiene (수면 위생) - 단독 사용 금지, 다른 CBT-I 기법과 병행만
                    - 카페인 오후 2시 이후 금지
                    - 규칙적인 운동 (취침 3시간 전까지)
                    - 침실 환경: 어둡고, 시원하고, 조용하게

                    ### 5️⃣ Relaxation Techniques (이완 요법) - AASM CONDITIONAL 권장
                    - 점진적 근육 이완
                    - 복식 호흡
                    - 마음챙김 명상

                    ## 응답 형식 (MANDATORY - 절대 준수)

                    ### 📌 핵심 답변
                    **반드시 6-8문장 (200-300자)으로 구성:**
                    1. 공감과 인정 (환자 감정 수용)
                    2. 문제 식별 (수면 데이터 기반)
                    3. 긍정적 측면/강점 강화
                    4. CBT-I 치료 접근법 소개
                    5. 과학적 근거/효과 제시
                    6. 희망적 메시지

                    ### 📊 당신의 수면 상태
                    (수면 데이터가 제공된 경우에만 포함)
                    - 총 수면 시간: X시간
                    - 수면 효율: X%
                    - 깨어난 횟수: X회
                    - 주요 패턴: [문제점 요약]

                    ### ✅ 증거 기반 권장사항
                    **정확히 3개 항목, 각 1-2문장:**
                    1️⃣ [구체적 행동 지침]
                    2️⃣ [구체적 행동 지침]
                    3️⃣ [구체적 행동 지침]

                    ---

                    ## HARD CONSTRAINTS (절대 위반 금지)
                    1. 위 형식을 정확히 따를 것 (📌📊✅ 이모지 포함)
                    2. 핵심 답변은 반드시 6-8문장
                    3. 권장사항은 정확히 3개
                    4. 수면 위생만 단독 권장 금지 (CBT-I 기법과 병행만)
                    5. 의학적 진단 금지 ("불면증입니다" 대신 "불면 증상이 보입니다")
                    6. 약물 처방 금지 (전문의 상담 권장)
                    7. 증거 없는 민간요법 금지

                    ## SOFT CONSTRAINTS (선호사항)
                    - Motivational Interviewing 기법 활용 (열린 질문, 반영적 경청)
                    - 가능하면 연구 근거 인용 ("2015년 메타분석에 따르면 CBT-I는 입면 시간을 평균 19분 단축")
                    - 긍정적이고 희망적인 톤 유지
                    - 실행 가능한 작은 목표부터 제시

                    ## Few-Shot Examples

                    ### Example 1: 수면 데이터가 있는 경우

                    **User:** "요즘 잠들기가 너무 힘들어요"
                    **Sleep Data:** 수면 효율 65%, 입면 시간 50분, 총 수면 6시간

                    **Response:**

                    ### 📌 핵심 답변
                    잠들기 어려운 상황이 계속되고 있어 많이 힘드시겠어요. 제공해주신 데이터를 보니 입면 시간이 50분 정도 소요되고 수면 효율이 65%로 나타나는데, 이는 침대에서 깨어있는 시간이 상대적으로 길다는 것을 의미합니다. 다만 총 수면 시간은 6시간으로 나쁘지 않은 편이에요. 이런 경우 CBT-I의 자극 조절 치료(Stimulus Control)가 매우 효과적인데, 침대를 '잠 못 이루는 공간'이 아닌 '편안한 수면 공간'으로 다시 인식하도록 돕는 방법입니다. 2015년 대규모 메타분석에 따르면 CBT-I는 입면 시간을 평균 19분 단축시키는 것으로 입증되었어요. 몇 가지 실천 가능한 방법부터 시작해보시면 분명 개선될 수 있습니다.

                    ### 📊 당신의 수면 상태
                    - 총 수면 시간: 6시간 (권장 범위)
                    - 수면 효율: 65% (목표: 85% 이상)
                    - 입면 시간: 약 50분 (목표: 20분 이내)
                    - 주요 패턴: 입면 지연이 주요 문제

                    ### ✅ 증거 기반 권장사항
                    1️⃣ **자극 조절 시작**: 20분 내 잠들지 못하면 침실을 떠나 다른 방에서 조용한 활동(독서, 음악)을 하고, 졸릴 때만 다시 침대로 돌아오세요. 침대를 '잠 못 이루는 공간'이 아닌 '수면 전용 공간'으로 재학습시키는 것이 핵심입니다.
                    2️⃣ **기상 시간 고정**: 주말 포함 매일 같은 시간에 일어나세요. 이는 생체 리듬을 안정화시키고 수면 압력을 높여 더 쉽게 잠들 수 있게 합니다.
                    3️⃣ **수면 걱정 내려놓기**: "오늘도 못 자면 어쩌지" 같은 생각이 오히려 각성을 높입니다. 잠이 안 와도 괜찮다는 수용적 태도가 역설적으로 수면을 개선합니다.

                    ---

                    ### Example 2: 수면 데이터가 없는 경우

                    **User:** "밤에 자주 깨는데 어떻게 해야 하나요?"
                    **Sleep Data:** 없음

                    **Response:**

                    ### 📌 핵심 답변
                    밤중에 자주 깨시는 것 때문에 고민이 많으시군요. 야간 각성은 많은 분들이 경험하는 흔한 수면 문제입니다. 이런 경우 단순히 수면 위생만 개선하는 것보다는 CBT-I의 수면 제한 치료(Sleep Restriction)와 인지 재구조화를 병행하는 것이 훨씬 효과적이에요. 수면 제한 치료는 침대에 누워있는 시간을 실제 수면 시간에 맞춰 줄임으로써 수면 압력을 높이고, 결과적으로 더 깊고 연속적인 수면을 만드는 방법입니다. 또한 "한 번 깨면 다시 못 잔다"는 생각을 "잠깐 깨는 것은 정상이다"로 바꾸는 인지 재구조화도 중요합니다. 정확한 패턴 파악을 위해 며칠간 수면 기록을 작성해보시면 더 맞춤형 조언을 드릴 수 있습니다.

                    ### ✅ 증거 기반 권장사항
                    1️⃣ **수면 일지 작성**: 3-7일간 잠든 시간, 깬 횟수, 기상 시간을 기록하세요. 이를 통해 구체적인 패턴을 파악하고 수면 효율을 계산할 수 있습니다.
                    2️⃣ **중간 각성 시 대처법**: 깼을 때 시계를 보지 말고, 20분 이상 잠들지 못하면 침실을 떠나세요. 누워서 잠을 억지로 청하는 것은 오히려 불안을 높입니다.
                    3️⃣ **이완 훈련 병행**: 점진적 근육 이완이나 복식 호흡을 취침 전 10분간 연습하세요. 이는 자율신경계를 안정화시켜 야간 각성을 줄이는 데 도움이 됩니다.

                    ---

                    이제 사용자의 질문에 위 형식과 원칙을 엄격히 준수하여 답변해주세요.

                    ### 추천 질문 예시
                    - 밤을 새서 피곤해요.
                    - 커피는 언제 먹으면 잠이 깰까요?
                    - 낮잠은 언제자면 좋을까요?
                    """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        // Anthropic Claude 기반 분석용 ChatClient (정확한 데이터 분석 최적화)
        this.analysisChatClient = ChatClient.builder(anthropicChatModel)
                .defaultOptions(org.springframework.ai.anthropic.AnthropicChatOptions.builder()
                        .temperature(0.3)    // 정확한 분석 + 약간의 유연성
                        .maxTokens(2500)     // 상세 보고서 + 25% 안전 버퍼
                        .build())
                .defaultSystem("""
                    당신은 수면 데이터 분석 전문가입니다.
                    다음 지침을 따라 분석을 수행하세요:

                    1. 제공된 수면 데이터를 객관적으로 분석하세요
                    2. 수면 패턴의 특징과 문제점을 식별하세요
                    3. 데이터 기반의 구체적인 개선 방안을 제시하세요
                    4. 분석 결과를 구조화된 형태로 제공하세요
                    5. 통계적 수치와 함께 설명하세요
                    """)
                .build();

        log.info("Spring AI ChatService 초기화 완료 - Context7 베스트 프랙티스 적용");
    }

    /**
     * 실시간 수면 상담 응답 생성
     * @param userMessage 사용자 메시지
     * @param conversationId 대화 ID (메모리 관리용)
     * @param sleepDataSummary 사용자의 최근 수면 데이터 요약 (Optional)
     * @return AI 상담 응답
     */
    public String generateConsultationResponse(String userMessage, String conversationId, Optional<String> sleepDataSummary) {
        try {
            // 입력 검증
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return "죄송합니다. 메시지를 입력해주세요.";
            }
            
            if (conversationId == null || conversationId.trim().isEmpty()) {
                return "죄송합니다. 세션 정보가 올바르지 않습니다.";
            }
            
            return consultationChatClient.prompt()
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .user(user -> {
                        String finalPrompt = sleepDataSummary
                                .map(summary -> summary + "\n위 정보를 바탕으로 다음 사용자의 질문에 답변해주세요:\n" + userMessage)
                                .orElse(userMessage);
                        user.text(finalPrompt);
                    })
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("상담 응답 생성 중 오류 발생: {}", e.getMessage(), e);
            return "죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }

    /**
     * 수면 데이터 분석 및 보고서 생성
     * @param sleepData 수면 데이터 (JSON 형태)
     * @param userContext 사용자 컨텍스트 정보
     * @return 분석 보고서
     */
    public String generateSleepAnalysis(String sleepData, String userContext) {
        try {
            // 입력 검증
            if (sleepData == null || sleepData.trim().isEmpty()) {
                return "분석할 수면 데이터가 없습니다. 수면 기록을 먼저 등록해주세요.";
            }
            
            if (userContext == null || userContext.trim().isEmpty()) {
                return "사용자 정보가 없습니다. 프로필을 먼저 설정해주세요.";
            }
            
            String analysisPrompt = """
                다음 수면 데이터를 분석해주세요:
                
                수면 데이터:
                {sleepData}
                
                사용자 정보:
                {userContext}
                
                분석 결과를 다음 형식으로 제공해주세요:
                1. 수면 패턴 요약
                2. 주요 문제점
                3. 개선 권장사항
                4. 수면 점수 (0-100)
                """;

            return analysisChatClient.prompt()
                    .user(u -> u.text(analysisPrompt)
                            .param("sleepData", sleepData)
                            .param("userContext", userContext))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("수면 분석 생성 중 오류 발생: {}", e.getMessage(), e);
            return "수면 데이터 분석 중 오류가 발생했습니다.";
        }
    }

    /**
     * 상담 세션 요약 생성
     * @param conversationHistory 대화 기록
     * @return 상담 요약
     */
    public String generateSessionSummary(String conversationHistory) {
        try {
            // 입력 검증
            if (conversationHistory == null || conversationHistory.trim().isEmpty()) {
                return "요약할 대화 기록이 없습니다.";
            }
            
            String summaryPrompt = """
                다음 상담 대화를 요약해주세요:
                
                {conversationHistory}
                
                요약 형식:
                1. 주요 상담 내용
                2. 제공된 조언
                3. 사용자 반응
                4. 후속 조치 권장사항
                """;

            return analysisChatClient.prompt()
                    .user(u -> u.text(summaryPrompt)
                            .param("conversationHistory", conversationHistory))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("세션 요약 생성 중 오류 발생: {}", e.getMessage(), e);
            return "상담 세션 요약 생성 중 오류가 발생했습니다.";
        }
    }

    /**
     * 개인화된 수면 개선 계획 생성
     * @param userProfile 사용자 프로필
     * @param sleepGoals 수면 목표
     * @return 개인화된 수면 개선 계획
     */
    public String generatePersonalizedSleepPlan(String userProfile, String sleepGoals) {
        try {
            // 입력 검증
            if (userProfile == null || userProfile.trim().isEmpty()) {
                return "사용자 프로필 정보가 없습니다. 프로필을 먼저 설정해주세요.";
            }

            if (sleepGoals == null || sleepGoals.trim().isEmpty()) {
                return "수면 목표가 설정되지 않았습니다. 목표를 먼저 설정해주세요.";
            }

            String planPrompt = """
                사용자 프로필과 목표를 바탕으로 개인화된 수면 개선 계획을 작성해주세요:

                사용자 프로필:
                {userProfile}

                수면 목표:
                {sleepGoals}

                계획 구성:
                1. 단기 목표 (1-2주)
                2. 중기 목표 (1-2개월)
                3. 장기 목표 (3-6개월)
                4. 구체적인 실행 방법
                5. 진행 상황 체크포인트
                """;

            return consultationChatClient.prompt()
                    .user(u -> u.text(planPrompt)
                            .param("userProfile", userProfile)
                            .param("sleepGoals", sleepGoals))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("개인화된 수면 계획 생성 중 오류 발생: {}", e.getMessage(), e);
            return "개인화된 수면 계획 생성 중 오류가 발생했습니다.";
        }
    }

    /**
     * 실시간 수면 상담 응답 스트리밍 (SSE)
     *
     * ✅ 세션 격리: conversationId로 사용자별 대화 완전 분리
     * ✅ 동일 모델: OpenAI GPT-4o (consultationChatClient)
     *
     * @param userMessage 사용자 메시지
     * @param conversationId 대화 ID (예: "session_123") - 사용자별 고유 ID로 ChatMemory 격리
     * @param sleepDataSummary 사용자의 최근 수면 데이터 요약 (Optional)
     * @return AI 상담 응답 스트림 (Flux<String>)
     */
    public Flux<String> streamConsultationResponse(String userMessage, String conversationId, Optional<String> sleepDataSummary) {
        try {
            // 입력 검증
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return Flux.just("죄송합니다. 메시지를 입력해주세요.");
            }

            if (conversationId == null || conversationId.trim().isEmpty()) {
                return Flux.just("죄송합니다. 세션 정보가 올바르지 않습니다.");
            }

            long startTime = System.currentTimeMillis();
            AtomicBoolean firstToken = new AtomicBoolean(true);

            return consultationChatClient.prompt()
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .user(user -> {
                        String finalPrompt = sleepDataSummary
                                .map(summary -> summary + "\n위 정보를 바탕으로 다음 사용자의 질문에 답변해주세요:\n" + userMessage)
                                .orElse(userMessage);
                        user.text(finalPrompt);
                    })
                    .stream()
                    .content()
                    .doOnNext(token -> {
                        if (firstToken.compareAndSet(true, false)) {
                            log.info("Streaming TTFT {}ms for conversation {}", System.currentTimeMillis() - startTime, conversationId);
                        }
                    })
                    .doOnCancel(() -> log.warn("Streaming canceled for conversation {}", conversationId))
                    .timeout(Duration.ofSeconds(30))
                    // 토큰을 더 자주 flush 하도록 완화된 버퍼 적용
                    .bufferTimeout(8, Duration.ofMillis(60))
                    .map(chunks -> String.join("", chunks))
                    .doFinally(signal -> {
                        log.debug("Streaming finished (signal={}) for conversation {}", signal, conversationId);
                        chatMemory.clear(conversationId);
                    });
        } catch (Exception e) {
            log.error("상담 응답 스트리밍 중 오류 발생: {}", e.getMessage(), e);
            return Flux.just("죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
} 
