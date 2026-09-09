package com.sleepwell.sleepwell_backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleepwell.sleepwell_backend.dto.*;
import com.sleepwell.sleepwell_backend.entity.ConsultationSession;
import com.sleepwell.sleepwell_backend.entity.ConversationMessage;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.AiResponseMode;
import com.sleepwell.sleepwell_backend.enums.ConsultationTopic;
import com.sleepwell.sleepwell_backend.enums.MessageType;
import com.sleepwell.sleepwell_backend.enums.RagRouteType;
import com.sleepwell.sleepwell_backend.enums.RecommendedFeature;
import com.sleepwell.sleepwell_backend.enums.SessionStatus;
import com.sleepwell.sleepwell_backend.enums.SessionType;
import com.sleepwell.sleepwell_backend.enums.SleepDataFetchStatus;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.rag.dto.RagQueryRequest;
import com.sleepwell.sleepwell_backend.rag.service.RagQueryService;
import com.sleepwell.sleepwell_backend.repository.ConsultationSessionRepository;
import com.sleepwell.sleepwell_backend.repository.ConversationMessageRepository;
import com.sleepwell.sleepwell_backend.service.AiResponseModeResolver;
import com.sleepwell.sleepwell_backend.service.AnswerAugmentor;
import com.sleepwell.sleepwell_backend.service.FollowUpQuestionGenerator;
import com.sleepwell.sleepwell_backend.service.PeriodRoutingResult;
import com.sleepwell.sleepwell_backend.service.RagEnhancedConsultationService;
import com.sleepwell.sleepwell_backend.service.RagTurnRoutingService;
import com.sleepwell.sleepwell_backend.service.RecommendedFeatureDetector;
import com.sleepwell.sleepwell_backend.service.SleepContextBuilder;
import com.sleepwell.sleepwell_backend.service.SleepPeriodParser;
import com.sleepwell.sleepwell_backend.service.SpringAIChatService;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RAG 강화 상담 서비스 구현체
 *
 * 논문 검색(RAG) + 개인 수면 데이터를 결합하여 최상의 상담 품질 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RagEnhancedConsultationServiceImpl implements RagEnhancedConsultationService {

    private final RagQueryService ragQueryService;
    private final SpringAIChatService springAIChatService;
    private final ConsultationSessionRepository consultationSessionRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final SleepContextBuilder sleepContextBuilder;
    private final AiResponseModeResolver aiResponseModeResolver;
    private final SleepPeriodParser sleepPeriodParser;
    private final FollowUpQuestionGenerator followUpQuestionGenerator;
    private final RecommendedFeatureDetector recommendedFeatureDetector;
    private final AnswerAugmentor answerAugmentor;
    private final RagTurnRoutingService ragTurnRoutingService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional  // DB 저장을 위해 readOnly = false
    public RagEnhancedConsultationResponseDto generateConsultation(
            User user,
            RagEnhancedConsultationRequestDto request) {

        long startTime = System.currentTimeMillis();
        log.info("[RAG상담] 시작 - userId={}, queryLength={}, sleepDataDays={}, responseMode={}, topK={}",
                user.getId(),
                request.getQuery().length(),
                request.getSleepDataDays(),
                request.getResponseMode(),
                request.getTopK());

        try {
            // 1. 상담 세션 생성
            LocalDateTime now = LocalDateTime.now();
            ConsultationSession session = ConsultationSession.builder()
                    .user(user)
                    .sessionType(SessionType.RAG_CONSULTATION)  // RAG 전용 타입
                    .topic(ConsultationTopic.GENERAL)
                    .status(SessionStatus.IN_PROGRESS)
                    .consultationTime(now)
                    .voiceEnabled(false)
                    .build();
            session = consultationSessionRepository.save(session);
            log.debug("[RAG상담] 세션 생성 - sessionId={}", session.getId());

            // 2. 사용자 질문 저장
            ConversationMessage userMessage = ConversationMessage.builder()
                    .consultationSession(session)
                    .user(user)
                    .messageType(MessageType.USER_TEXT)
                    .content(request.getQuery())
                    .status(com.sleepwell.sleepwell_backend.enums.MessageStatus.COMPLETED)
                    .sentAt(now)
                    .processedAt(now)
                    .build();
            conversationMessageRepository.save(userMessage);
            log.debug("[RAG상담] 사용자 질문 저장 - messageId={}", userMessage.getId());

            PeriodRoutingResult routing = resolveRouting(request.getQuery(), request.getSleepDataDays());
            SleepContextBuilder.SleepContextResult sleepContext =
                    sleepContextBuilder.buildSleepContext(user, routing);

            // 4. RAG로 논문 검색
            Map<String, Object> ragResponse = performRagSearch(request);

            AiResponseMode resolvedMode = aiResponseModeResolver.resolve(
                    request.getResponseMode(),
                    request.getQuery(),
                    List.of());

            // 5. 통합 프롬프트 구성 및 AI 답변 생성
            String prompt = buildRagPrompt(
                    request.getQuery(),
                    sleepContext,
                    ragResponse,
                    resolvedMode,
                    null);

            AiParsedResponse parsedResponse = generateParsedAnswer(
                    prompt,
                    resolvedMode,
                    sleepContext.summarySection(),
                    user.getId());

            List<String> followUpQuestions = followUpQuestionGenerator.generate(request.getQuery());
            RecommendedFeature feature = recommendedFeatureDetector.detect(request.getQuery());

            String augmentedAnswer = answerAugmentor.appendFeatureRecommendation(parsedResponse.answer(), feature);
            augmentedAnswer = answerAugmentor.appendFollowUpQuestions(augmentedAnswer, followUpQuestions);

            // 6. AI 답변 저장
            LocalDateTime aiResponseTime = LocalDateTime.now();
            ConversationMessage aiMessage = ConversationMessage.builder()
                    .consultationSession(session)
                    .user(user)
                    .messageType(MessageType.AI_RESPONSE)
                    .content(augmentedAnswer)
                    .status(com.sleepwell.sleepwell_backend.enums.MessageStatus.COMPLETED)
                    .sentAt(aiResponseTime)
                    .processedAt(aiResponseTime)
                    .build();
            conversationMessageRepository.save(aiMessage);
            log.debug("[RAG상담] AI 답변 저장 - messageId={}", aiMessage.getId());

            // 7. 세션 완료 처리
            session.endSession();
            consultationSessionRepository.save(session);

            // 8. 인용 정보 추출
            List<RagEnhancedConsultationResponseDto.Citation> citations = extractCitations(ragResponse);

            // 9. 권장사항 추출
            List<String> recommendations = extractRecommendations(augmentedAnswer);

            long processingTime = System.currentTimeMillis() - startTime;
            log.info(
                    "[RAG상담] 완료 - userId={}, sessionId={}, processingTime={}ms, citations={}, recommendations={}, hasPersonalData={}",
                    user.getId(),
                    session.getId(),
                    processingTime,
                    citations != null ? citations.size() : 0,
                    recommendations != null ? recommendations.size() : 0,
                    sleepContext.status() == SleepDataFetchStatus.SUCCESS);

            return RagEnhancedConsultationResponseDto.builder()
                    .answer(augmentedAnswer)
                    .personalData(sleepContext.summary())
                    .sleepDataStatus(sleepContext.status())
                    .sleepSummary(sleepContext.summarySection())
                    .citations(citations)
                    .recommendations(recommendations)
                    .aiResponseMode(parsedResponse.mode())
                    .followUpQuestion(parsedResponse.followUpQuestion())
                    .followUpQuestions(followUpQuestions)
                    .recommendedFeature(feature)
                    .processingTimeMs(processingTime)
                    .build();

        } catch (Exception e) {
            log.error("[RAG상담] 실패 - userId={}, error={}", user.getId(), e.getMessage(), e);
            throw new BusinessException(
                    "상담 생성 중 오류가 발생했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }

    @Override
    public Flux<String> generateConsultationStream(
            User user,
            RagEnhancedConsultationRequestDto request) {

        log.info("[RAG상담스트리밍] 시작 - userId={}, queryLength={}", user.getId(), request.getQuery().length());

        try {
            PeriodRoutingResult routing = resolveRouting(request.getQuery(), request.getSleepDataDays());
            SleepContextBuilder.SleepContextResult sleepContext =
                    sleepContextBuilder.buildSleepContext(user, routing);

            // 2. RAG 검색
            Map<String, Object> ragResponse = performRagSearch(request);

            AiResponseMode resolvedMode = aiResponseModeResolver.resolve(
                    request.getResponseMode(),
                    request.getQuery(),
                    List.of());

            // 3. 통합 프롬프트 구성
            String enhancedPrompt = buildRagPrompt(
                    request.getQuery(),
                    sleepContext,
                    ragResponse,
                    resolvedMode,
                    null);

            // 4. 스트리밍 응답 생성 (에러 로깅 강화)
            String conversationId = "rag_enhanced_" + user.getId() + "_" + System.currentTimeMillis();
            return springAIChatService.streamConsultationResponse(
                    enhancedPrompt,
                    conversationId,
                    Optional.empty())
                    .doOnCancel(() -> log.warn("[RAG상담스트리밍] 취소 - userId={}", user.getId()))
                    .doOnError(e -> log.error("[RAG상담스트리밍] 에러 - userId={}, error={}", user.getId(), e.getMessage(), e))
                    .doOnComplete(() -> log.info("[RAG상담스트리밍] 완료 - userId={}", user.getId()));

        } catch (Exception e) {
            log.error("[RAG상담스트리밍] 초기화 실패 - userId={}, error={}", user.getId(), e.getMessage(), e);
            return Flux.error(new BusinessException(
                    "스트리밍 상담 생성 중 오류가 발생했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e));
        }
    }

    /**
     * RAG 검색 수행
     */
    private Map<String, Object> performRagSearch(RagEnhancedConsultationRequestDto request) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("[RAG검색] 시작 - topK={}, queryLength={}", request.getTopK(), request.getQuery().length());
        }

        // RagQueryRequest 내부에서 topK null/이상값은 자동 보정됨
        RagQueryRequest ragRequest = RagQueryRequest.simple(
                request.getQuery(),
                request.getTopK());

        Map<String, Object> result = ragQueryService.answer(ragRequest);

        if (log.isDebugEnabled() && result.containsKey("citations")) {
            Object citationsObj = result.get("citations");
            int size = 0;
            if (citationsObj instanceof List<?> list) {
                size = list.size();
            }
            log.debug("[RAG검색] 완료 - citations={}", size);
        }

        return result;
    }

    /**
     * 통합 프롬프트 구성 및 AI 답변 생성
     */
    private AiParsedResponse generateParsedAnswer(
            String prompt,
            AiResponseMode desiredMode,
            String sleepSummarySection,
            Long userId) {

        String conversationId = "rag_enhanced_" + userId + "_" + System.currentTimeMillis();

        if (log.isDebugEnabled()) {
            log.debug("[AI응답생성] 시작 - conversationId={}, promptLength={}",
                    conversationId, prompt.length());
        }

        String answer = springAIChatService.generateConsultationResponse(
                prompt,
                conversationId,
                Optional.empty());

        if (log.isDebugEnabled()) {
            log.debug("[AI응답생성] 완료 - answerLength={}", answer != null ? answer.length() : 0);
        }

        return parseAiResponse(answer, desiredMode, sleepSummarySection);
    }

    /**
     * 강화된 프롬프트 구성
     */
    private String buildRagPrompt(
            String query,
            SleepContextBuilder.SleepContextResult sleepContext,
            Map<String, Object> ragResponse,
            AiResponseMode responseMode,
            String conversationContext) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 수면 의학 전문가입니다. 제공된 개인 수면 데이터와 RAG 근거를 반영해 답변하세요.\n");
        prompt.append("응답 형식은 반드시 JSON으로만 출력하세요.\n\n");

        if (conversationContext != null && !conversationContext.isEmpty()) {
            prompt.append(conversationContext).append("\n\n");
        }

        prompt.append("[수면 데이터 조회 상태]\n");
        prompt.append("- status: ").append(sleepContext.status()).append("\n");
        prompt.append("- 기간: 최근 ").append(sleepContext.periodDays()).append("일\n");
        if (sleepContext.summarySection() != null) {
            prompt.append(sleepContext.summarySection()).append("\n");
        } else {
            prompt.append("### 당신의 수면 상태 요약\n");
            prompt.append("- ").append(buildSleepStatusMessage(sleepContext.status())).append("\n");
        }

        if (ragResponse.containsKey("message_detailed")) {
            prompt.append("\n[수면 의학 연구 근거]\n");
            prompt.append(ragResponse.get("message_detailed")).append("\n\n");
        }

        prompt.append("[사용자 질문]\n").append(query).append("\n\n");

        prompt.append("[응답 모드 지시]\n");
        prompt.append("- desiredMode: ").append(responseMode).append("\n");
        prompt.append("- PINGPONG: 1~2문장 매우 짧게 답변하고 followUpQuestion을 반드시 포함\n");
        prompt.append("- FULL: 핵심 답변 + 증거 기반 권장사항 구조 유지, ");
        prompt.append("수면 데이터가 있으면 요약 섹션을 포함\n");
        prompt.append("- 데이터가 부족하면 없는 값은 '데이터 없음'으로 표시하거나 생략\n\n");

        prompt.append("응답 JSON 스키마:\n");
        prompt.append("{\"mode\":\"FULL|PINGPONG\",\"answer\":\"...\",\"followUpQuestion\":null|\"...\"}\n");

        log.info("[RAG프롬프트] 수면컨텍스트포함={}, length={}, periodDays={}",
                sleepContext.summarySection() != null,
                prompt.length(),
                sleepContext.periodDays());

        return prompt.toString();
    }

    /**
     * 인용 정보 추출
     */
    @SuppressWarnings("unchecked")
    private List<RagEnhancedConsultationResponseDto.Citation> extractCitations(Map<String, Object> ragResponse) {
        List<RagEnhancedConsultationResponseDto.Citation> citations = new ArrayList<>();

        if (ragResponse.containsKey("citations")) {
            Object citationsObj = ragResponse.get("citations");
            if (citationsObj instanceof List) {
                List<?> citationList = (List<?>) citationsObj;
                citations = citationList.stream()
                        .map(c -> {
                            if (c instanceof com.sleepwell.sleepwell_backend.rag.infra.PromptBuilder.Cite cite) {
                                // Cite record 객체 직접 처리
                                return RagEnhancedConsultationResponseDto.Citation.builder()
                                        .title(cite.title())
                                        .quote(cite.quote())
                                        .url(cite.url())
                                        .relevance("관련 연구 근거")
                                        .build();
                            } else if (c instanceof Map) {
                                // Map 형태로 들어온 경우 (하위 호환성)
                                Map<String, Object> citeMap = (Map<String, Object>) c;
                                return RagEnhancedConsultationResponseDto.Citation.builder()
                                        .title((String) citeMap.get("title"))
                                        .quote((String) citeMap.get("quote"))
                                        .url((String) citeMap.get("url"))
                                        .relevance("관련 연구 근거")
                                        .build();
                            }
                            return null;
                        })
                        .filter(c -> c != null)
                        .collect(Collectors.toList());

                if (log.isDebugEnabled()) {
                    log.debug("[인용추출] 완료 - count={}", citations.size());
                }
            }
        } else {
            log.warn("[인용추출] 인용없음 - RAG 응답에 citations 필드 없음");
        }

        return citations;
    }

    /**
     * 권장사항 추출 (답변에서 리스트 형태로 추출)
     */
    private List<String> extractRecommendations(String answer) {
        List<String> recommendations = new ArrayList<>();

        if (answer == null || answer.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("[권장사항추출] 스킵 - 답변 없음");
            }
            return recommendations;
        }

        // 간단한 패턴 매칭으로 권장사항 추출
        String[] lines = answer.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("*")) {
                recommendations.add(trimmed.substring(1).trim());
            } else if (trimmed.matches("^\\d+\\..*")) {
                recommendations.add(trimmed.replaceFirst("^\\d+\\.", "").trim());
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("[권장사항추출] 완료 - count={}", recommendations.size());
        }

        return recommendations.isEmpty() ? null : recommendations;
    }

    private record AiParsedResponse(AiResponseMode mode, String answer, String followUpQuestion) {}

    /**
     * RAG 상담 세션 시작
     */
    @Override
    @Transactional
    public RagSessionResponseDto startRagSession(User user, StartRagSessionRequestDto request) {
        log.info("[RAG세션] 시작 - userId={}", user.getId());

        ConsultationSession session = ConsultationSession.builder()
                .user(user)
                .sessionType(SessionType.RAG_CONSULTATION)
                .topic(ConsultationTopic.GENERAL)
                .status(SessionStatus.IN_PROGRESS)
                .consultationTime(LocalDateTime.now())
                .voiceEnabled(false)
                .build();

        session = consultationSessionRepository.save(session);
        log.info("[RAG세션] 생성 완료 - sessionId={}", session.getId());

        return RagSessionResponseDto.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .createdAt(session.getConsultationTime())
                .includePersonalData(request.getIncludePersonalData())
                .includeCitations(request.getIncludeCitations())
                .sleepDataDays(request.getSleepDataDays())
                .topK(request.getTopK())
                .totalMessages(0)
                .build();
    }

    /**
     * RAG 상담 메시지 전송 (연속 대화 지원)
     */
    @Override
    @Transactional
    public RagEnhancedConsultationResponseDto sendRagMessage(
            User user,
            Long sessionId,
            SendRagMessageRequestDto request) {

        long startTime = System.currentTimeMillis();
        log.info("[RAG메시지] 전송 - userId={}, sessionId={}, queryLength={}",
                user.getId(), sessionId, request.getQuery().length());

        try {
            // 1. 세션 조회 및 권한 확인
            ConsultationSession session = consultationSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new BusinessException(
                            "상담 세션을 찾을 수 없습니다.",
                            HttpStatus.NOT_FOUND));

            if (!session.getUser().getId().equals(user.getId())) {
                throw new BusinessException(
                        "해당 세션에 접근할 권한이 없습니다.",
                        HttpStatus.FORBIDDEN);
            }

            if (session.getStatus() == SessionStatus.COMPLETED) {
                throw new BusinessException(
                        "이미 종료된 세션입니다.",
                        HttpStatus.BAD_REQUEST);
            }

            RagRouteType routeType = ragTurnRoutingService.resolveRouteType(sessionId, request.getQuery());

            // 2. 사용자 질문 저장
            LocalDateTime now = LocalDateTime.now();
            ConversationMessage userMessage = ConversationMessage.builder()
                    .consultationSession(session)
                    .user(user)
                    .messageType(MessageType.USER_TEXT)
                    .content(request.getQuery())
                    .status(com.sleepwell.sleepwell_backend.enums.MessageStatus.COMPLETED)
                    .sentAt(now)
                    .processedAt(now)
                    .build();
            conversationMessageRepository.save(userMessage);
            log.debug("[RAG메시지] 사용자 질문 저장 완료");

            PeriodRoutingResult routing = resolveRouting(request.getQuery(), request.getSleepDataDays());
            SleepContextBuilder.SleepContextResult sleepContext =
                    sleepContextBuilder.buildSleepContext(user, routing);

            if (routeType == RagRouteType.HYBRID) {
                log.info("[RAG라우팅] HYBRID 선택 - RAG 근거를 검색하되, 일반 상담 답변을 함께 혼합합니다.");
            }

            Map<String, Object> ragResponse = new HashMap<>();
            if (routeType != RagRouteType.NON_RAG) {
                Integer topK = 5;
                RagQueryRequest ragRequest = RagQueryRequest.simple(request.getQuery(), topK);
                ragResponse = performRagSearch(ragRequest);
            }

            // 6. 이전 대화 맥락 가져오기 (중요!)
            List<ConversationMessage> recentMessages = fetchRecentMessages(sessionId);
            String conversationContext = buildConversationContext(recentMessages);

            // 7. 통합 프롬프트 구성 및 AI 답변 생성 (대화 맥락 포함)
            AiResponseMode resolvedMode = aiResponseModeResolver.resolve(
                    request.getResponseMode(),
                    request.getQuery(),
                    recentMessages);

            String prompt;
            AiParsedResponse parsedResponse;
            if (routeType == RagRouteType.NON_RAG) {
                String answer = springAIChatService.generateConsultationResponse(
                        request.getQuery(),
                        "rag_session_" + sessionId,
                        Optional.ofNullable(sleepContext.summarySection()));
                parsedResponse = parseAiResponse(answer, resolvedMode, sleepContext.summarySection());
            } else {
                prompt = buildRagPrompt(
                        request.getQuery(),
                        sleepContext,
                        ragResponse,
                        resolvedMode,
                        conversationContext);
                parsedResponse = generateParsedAnswer(
                        prompt,
                        resolvedMode,
                        sleepContext.summarySection(),
                        user.getId());
            }

            List<String> followUpQuestions = followUpQuestionGenerator.generate(request.getQuery());
            RecommendedFeature feature = recommendedFeatureDetector.detect(request.getQuery());

            String augmentedAnswer = answerAugmentor.appendFeatureRecommendation(parsedResponse.answer(), feature);
            augmentedAnswer = answerAugmentor.appendFollowUpQuestions(augmentedAnswer, followUpQuestions);

            // 8. AI 답변 저장
            LocalDateTime aiResponseTime = LocalDateTime.now();
            ConversationMessage aiMessage = ConversationMessage.builder()
                    .consultationSession(session)
                    .user(user)
                    .messageType(MessageType.AI_RESPONSE)
                    .content(augmentedAnswer)
                    .status(com.sleepwell.sleepwell_backend.enums.MessageStatus.COMPLETED)
                    .sentAt(aiResponseTime)
                    .processedAt(aiResponseTime)
                    .build();
            conversationMessageRepository.save(aiMessage);
            log.debug("[RAG메시지] AI 답변 저장 완료");

            // 9. 인용 정보 추출
            List<RagEnhancedConsultationResponseDto.Citation> citations = extractCitations(ragResponse);

            // 10. 권장사항 추출
            List<String> recommendations = extractRecommendations(augmentedAnswer);

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("[RAG메시지] 완료 - sessionId={}, processingTime={}ms, hasPersonalData={}",
                    sessionId, processingTime, sleepContext.status() == SleepDataFetchStatus.SUCCESS);

            return RagEnhancedConsultationResponseDto.builder()
                    .answer(augmentedAnswer)
                    .sessionId(sessionId)
                    .personalData(sleepContext.summary())
                    .sleepDataStatus(sleepContext.status())
                    .sleepSummary(sleepContext.summarySection())
                    .citations(citations)
                    .recommendations(recommendations)
                    .aiResponseMode(parsedResponse.mode())
                    .followUpQuestion(parsedResponse.followUpQuestion())
                    .followUpQuestions(followUpQuestions)
                    .recommendedFeature(feature)
                    .processingTimeMs(processingTime)
                    .build();

        } catch (BusinessException e) {
            log.error("[RAG메시지] 비즈니스 오류 - sessionId={}, error={}", sessionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[RAG메시지] 시스템 오류 - sessionId={}", sessionId, e);
            throw new BusinessException(
                    "RAG 상담 처리 중 오류가 발생했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * 일반 상담 세션에서도 RAG/HYBRID 라우팅을 적용하기 위한 공용 응답 생성 로직.
     */
    public String generateRoutedAnswer(
            User user,
            Long sessionId,
            String query,
            SleepContextBuilder.SleepContextResult sleepContext,
            RagRouteType routeType) {

        if (routeType == RagRouteType.NON_RAG) {
            return springAIChatService.generateConsultationResponse(
                    query,
                    "session_" + sessionId,
                    Optional.ofNullable(sleepContext.summarySection()));
        }

        // HYBRID는 RAG 근거를 검색하되, 일반 상담 맥락을 함께 섞어 답변을 생성합니다.
        if (routeType == RagRouteType.HYBRID) {
            log.info("[라우팅] HYBRID 적용 - RAG 근거 + 일반 상담 맥락 혼합 생성");
        }

        List<ConversationMessage> recentMessages = fetchRecentMessages(sessionId);
        AiResponseMode resolvedMode = aiResponseModeResolver.resolve(
                AiResponseMode.AUTO,
                query,
                recentMessages);

        Map<String, Object> ragResponse = performRagSearch(RagQueryRequest.simple(query, 5));
        String conversationContext = buildConversationContext(recentMessages);
        String prompt = buildRagPrompt(
                query,
                sleepContext,
                ragResponse,
                resolvedMode,
                conversationContext);

        AiParsedResponse parsedResponse = generateParsedAnswer(
                prompt,
                resolvedMode,
                sleepContext.summarySection(),
                user.getId());

        return parsedResponse.answer();
    }

    /**
     * 스트리밍 응답용 RAG 프롬프트를 준비합니다. NON_RAG이면 null을 반환합니다.
     */
    public String prepareRagPromptForRoute(
            Long sessionId,
            String query,
            SleepContextBuilder.SleepContextResult sleepContext,
            RagRouteType routeType) {
        if (routeType == RagRouteType.NON_RAG) {
            return null;
        }

        // HYBRID는 RAG 근거를 제한적으로 반영하면서 일반 상담 어조를 유지합니다.
        if (routeType == RagRouteType.HYBRID) {
            log.info("[라우팅] HYBRID 스트리밍 - RAG 근거 + 일반 상담 어조 혼합");
        }

        List<ConversationMessage> recentMessages = fetchRecentMessages(sessionId);
        AiResponseMode resolvedMode = aiResponseModeResolver.resolve(
                AiResponseMode.AUTO,
                query,
                recentMessages);

        Map<String, Object> ragResponse = performRagSearch(RagQueryRequest.simple(query, 5));
        String conversationContext = buildConversationContext(recentMessages);
        return buildRagPrompt(
                query,
                sleepContext,
                ragResponse,
                resolvedMode,
                conversationContext);
    }

    /**
     * 이전 대화 맥락 구성
     *
     * ChatGPT/Claude 방식: 전체 대화 포함 (컨텍스트 윈도우 한계까지)
     * 우리 방식: 최근 20개 메시지 (10쌍) + AI 답변은 1000자로 요약
     *
     * 실제 데이터 분석 (portfolio@example.com):
     * - AI 답변 평균 길이: 1,716자 (최소 1,678자, 최대 1,773자)
     * - 1000자 요약 = 원본의 58% 보존 (앞 800자: 핵심 내용, 뒤 200자: 결론)
     *
     * 토큰 계산:
     * - GPT-4o: 128K 토큰 (약 384,000자)
     * - 우리 맥락: 사용자 질문 전체 + AI 답변 1000자 × 10쌍 = 약 15,000자
     * - 안전 마진: 3.9% 사용 (충분함!)
     */
    private List<ConversationMessage> fetchRecentMessages(Long sessionId) {
        return conversationMessageRepository
                .findByConsultationSessionIdOrderByCreatedAtDesc(sessionId)
                .stream()
                .limit(20)
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private String buildConversationContext(List<ConversationMessage> recentMessages) {
        if (recentMessages.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder("\n\n[이전 대화 맥락]\n");
        for (ConversationMessage msg : recentMessages) {
            if (msg.getMessageType() == MessageType.USER_TEXT) {
                // 사용자 질문은 전체 포함 (보통 짧음, 100-500자)
                context.append("사용자: ").append(msg.getContent()).append("\n");
            } else if (msg.getMessageType() == MessageType.AI_RESPONSE) {
                // AI 답변은 1000자로 요약 (원본 평균 1,716자의 58% 보존)
                int summaryLength = 1000;
                String aiContent = msg.getContent();
                if (aiContent.length() <= summaryLength) {
                    context.append("AI: ").append(aiContent).append("\n");
                } else {
                    // 앞 800자 (핵심 분석 + 주요 조언) + 뒤 200자 (결론 + 실천 방법)
                    context.append("AI: ")
                           .append(aiContent.substring(0, 800))
                           .append("... [중략] ...")
                           .append(aiContent.substring(aiContent.length() - 200))
                           .append("\n");
                }
            }
        }

        return context.toString();
    }

    private AiParsedResponse parseAiResponse(
            String rawAnswer,
            AiResponseMode resolvedMode,
            String sleepSummarySection) {

        if (rawAnswer == null || rawAnswer.isBlank()) {
            return new AiParsedResponse(resolvedMode, "", null);
        }

        String jsonPayload = extractJson(rawAnswer);
        if (jsonPayload == null) {
            String normalizedAnswer = appendSleepSummaryIfMissing(rawAnswer, sleepSummarySection);
            return new AiParsedResponse(resolvedMode, normalizedAnswer, null);
        }

        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            String modeValue = root.path("mode").asText(null);
            AiResponseMode mode = parseMode(modeValue, resolvedMode);
            String answer = root.path("answer").asText(null);
            String followUp = extractFollowUp(root);

            if (answer == null || answer.isBlank()) {
                answer = rawAnswer;
            }

            answer = appendSleepSummaryIfMissing(answer, sleepSummarySection);

            if (mode == AiResponseMode.PINGPONG && (followUp == null || followUp.isBlank())) {
                followUp = defaultFollowUpQuestion();
            }

            return new AiParsedResponse(mode, answer, followUp);
        } catch (Exception e) {
            log.warn("[AI응답파싱] 실패 - fallback to raw answer, error={}", e.getMessage());
            String normalizedAnswer = appendSleepSummaryIfMissing(rawAnswer, sleepSummarySection);
            return new AiParsedResponse(resolvedMode, normalizedAnswer, null);
        }
    }

    private String extractJson(String rawAnswer) {
        int start = rawAnswer.indexOf('{');
        int end = rawAnswer.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return rawAnswer.substring(start, end + 1);
        }
        return null;
    }

    private AiResponseMode parseMode(String modeValue, AiResponseMode fallback) {
        if (modeValue == null || modeValue.isBlank()) {
            return fallback;
        }
        try {
            return AiResponseMode.valueOf(modeValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private String extractFollowUp(JsonNode root) {
        String followUp = root.path("followUpQuestion").asText(null);
        if (followUp == null || followUp.isBlank()) {
            followUp = root.path("followUp").asText(null);
        }
        if (followUp == null || followUp.isBlank()) {
            followUp = root.path("nextQuestion").asText(null);
        }
        return followUp;
    }

    private String defaultFollowUpQuestion() {
        return "최근 수면에서 가장 불편했던 점이 무엇인가요?";
    }

    private String appendSleepSummaryIfMissing(String answer, String sleepSummarySection) {
        if (sleepSummarySection == null || sleepSummarySection.isBlank()) {
            return answer;
        }
        if (answer.contains("수면 상태 요약")) {
            return answer;
        }
        return answer + "\n\n" + sleepSummarySection;
    }

    private String buildSleepStatusMessage(SleepDataFetchStatus status) {
        return switch (status) {
            case ACCESS_DENIED -> "수면 데이터 조회 권한이 없어 확인할 수 없습니다.";
            case ERROR -> "수면 데이터 조회 중 오류가 발생했습니다.";
            case EMPTY -> "조회 결과 수면 기록이 비어 있습니다.";
            case SUCCESS -> "수면 데이터가 확인되었습니다.";
        };
    }

    private PeriodRoutingResult resolveRouting(String query, Integer sleepDataDays) {
        PeriodRoutingResult routing = sleepPeriodParser.parse(query);
        if (sleepDataDays != null && sleepDataDays > 0 && routing.isDefault()) {
            return PeriodRoutingResult.forRecentDays(sleepDataDays);
        }
        return routing;
    }

    /**
     * RAG 검색 수행 (기존 메서드 재사용)
     */
    private Map<String, Object> performRagSearch(RagQueryRequest request) {
        try {
            Map<String, Object> result = ragQueryService.answer(request);
            log.debug("[RAG검색] 완료 - documentCount={}",
                    ((List<?>) result.getOrDefault("documents", List.of())).size());
            return result;
        } catch (Exception e) {
            log.error("[RAG검색] 실패", e);
            return new HashMap<>();
        }
    }

    /**
     * RAG 상담 세션 종료
     */
    @Override
    @Transactional
    public SessionEndResponseDto endRagSession(User user, Long sessionId) {
        log.info("[RAG세션종료] 시작 - userId={}, sessionId={}", user.getId(), sessionId);

        ConsultationSession session = consultationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(
                        "상담 세션을 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new BusinessException(
                    "해당 세션에 접근할 권한이 없습니다.",
                    HttpStatus.FORBIDDEN);
        }

        session.endSession();
        consultationSessionRepository.save(session);

        int messageCount = conversationMessageRepository.countByConsultationSessionId(sessionId);

        return SessionEndResponseDto.builder()
                .sessionId(sessionId)
                .endedAt(session.getEndTime())
                .totalMessages(messageCount)
                .build();
    }

    /**
     * RAG 상담 세션 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RagSessionResponseDto> getRagSessions(User user, org.springframework.data.domain.Pageable pageable) {
        Page<ConsultationSession> sessions = consultationSessionRepository
                .findByUserIdAndSessionTypeOrderByCreatedAtDesc(
                        user.getId(),
                        SessionType.RAG_CONSULTATION,
                        pageable);

        return sessions.map(session -> {
            int messageCount = conversationMessageRepository.countByConsultationSessionId(session.getId());

            return RagSessionResponseDto.builder()
                    .sessionId(session.getId())
                    .status(session.getStatus())
                    .createdAt(session.getConsultationTime())
                    .includePersonalData(true) // 기본값
                    .includeCitations(true)
                    .sleepDataDays(7)
                    .topK(5)
                    .totalMessages(messageCount)
                    .build();
        });
    }

    /**
     * RAG 상담 세션 상세 정보 조회
     */
    @Override
    @Transactional(readOnly = true)
    public ConsultationSessionDetailDto getRagSessionDetail(User user, Long sessionId) {
        ConsultationSession session = consultationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(
                        "상담 세션을 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new BusinessException(
                    "해당 세션에 접근할 권한이 없습니다.",
                    HttpStatus.FORBIDDEN);
        }

        // 메시지 조회
        List<ConversationMessage> messages = conversationMessageRepository
                .findByConsultationSessionIdOrderByCreatedAtAsc(sessionId);

        List<MessageResponseDto> messageDtos = messages.stream()
                .map(msg -> MessageResponseDto.builder()
                        .messageId(msg.getId())
                        .messageType(msg.getMessageType())
                        .content(msg.getContent())
                        .sentAt(msg.getCreatedAt())
                        .status(msg.getStatus())
                        .isFromAI(msg.getMessageType() == MessageType.AI_RESPONSE)
                        .build())
                .collect(Collectors.toList());

        return ConsultationSessionDetailDto.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .topic(session.getTopic())
                .sessionType(session.getSessionType())
                .startedAt(session.getConsultationTime())
                .endedAt(session.getEndTime())
                .messages(messageDtos)
                .build();
    }
}
