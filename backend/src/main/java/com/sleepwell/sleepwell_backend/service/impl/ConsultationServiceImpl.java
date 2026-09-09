package com.sleepwell.sleepwell_backend.service.impl;

import com.sleepwell.sleepwell_backend.dto.*;
import com.sleepwell.sleepwell_backend.entity.*;
import com.sleepwell.sleepwell_backend.enums.*;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.repository.*;
import com.sleepwell.sleepwell_backend.service.ConsultationService;
import com.sleepwell.sleepwell_backend.service.AnswerAugmentor;
import com.sleepwell.sleepwell_backend.service.FollowUpQuestionGenerator;
import com.sleepwell.sleepwell_backend.service.PeriodRoutingResult;
import com.sleepwell.sleepwell_backend.service.RecommendedFeatureDetector;
import com.sleepwell.sleepwell_backend.service.SleepContextBuilder;
import com.sleepwell.sleepwell_backend.service.SleepPeriodParser;
import com.sleepwell.sleepwell_backend.service.SpringAIChatService;
import com.sleepwell.sleepwell_backend.service.RagTurnRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AI 기반 수면 상담 서비스 구현체
 * 
 * Spring AI를 활용한 상담 세션 관리 및 AI 응답 생성을 담당합니다.
 * Context7 베스트 프랙티스를 준수하여 안정적이고 확장 가능한 구조로 설계되었습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationSessionRepository consultationSessionRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConsultationSummaryRepository consultationSummaryRepository;
    private final UserRepository userRepository;
    private final SpringAIChatService springAIChatService;
    private final SleepRecordRepository sleepRecordRepository;
    private final SleepContextBuilder sleepContextBuilder;
    private final SleepPeriodParser sleepPeriodParser;
    private final FollowUpQuestionGenerator followUpQuestionGenerator;
    private final RecommendedFeatureDetector recommendedFeatureDetector;
    private final AnswerAugmentor answerAugmentor;
    private final RagTurnRoutingService ragTurnRoutingService;
    private final RagEnhancedConsultationServiceImpl ragEnhancedConsultationService;

    // 정규화된 설문 레포지토리들
    private final ISIResponseRepository isiResponseRepository;
    private final ESSResponseRepository essResponseRepository;
    private final PSQIResponseRepository psqiResponseRepository;
    private final BDIResponseRepository bdiResponseRepository;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String defaultAiModel;

    @Override
    @Transactional
    public ConsultationSessionResponseDto startSession(User user, StartConsultationRequestDto request) {
        log.info("Starting consultation session for user: {}, topic: {}", user.getId(), request.getTopic());

        try {
            // 새 세션 생성
            ConsultationSession session = ConsultationSession.builder()
                    .user(user)
                    .consultationTime(LocalDateTime.now())
                    .status(SessionStatus.IN_PROGRESS)
                    .topic(request.getTopic())
                    .sessionType(request.getSessionType())
                    .title("AI 수면 상담 세션") // 기본 제목
                    .initialDescription(request.getInitialMessage())
                    .voiceEnabled(request.getVoiceEnabled())
                    .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                    .build();

            session = consultationSessionRepository.save(session);
            log.info("Created consultation session: {}", session.getId());

            // 응답 DTO 생성
            return ConsultationSessionResponseDto.builder()
                    .sessionId(session.getId())
                    .status(session.getStatus())
                    .topic(session.getTopic())
                    .sessionType(session.getSessionType())
                    .startedAt(session.getConsultationTime())
                    .estimatedDurationMinutes(session.getEstimatedDurationMinutes())
                    .voiceEnabled(session.getVoiceEnabled())
                    .totalMessages(0)
                    .unreadMessages(0)
                    .lastActivityAt(session.getConsultationTime())
                    .initialMessage(session.getInitialDescription())
                    .suggestedPrompts(ConsultationSessionResponseDto.defaultSuggestedPrompts())
                    .build();

        } catch (BusinessException e) {
            log.error("Business error starting session: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error starting session", e);
            throw new BusinessException(
                    "상담 세션 시작 중 오류가 발생했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    @Transactional
    public MessageResponseDto sendMessage(User user, Long sessionId, SendMessageRequestDto request) {
        log.info("Sending message to session: {}, user: {}, type: {}", sessionId, user.getId(),
                request.getMessageType());

        try {
            // 세션 조회 및 권한 확인
            ConsultationSession session = consultationSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new BusinessException(
                            "상담 세션을 찾을 수 없습니다.",
                            HttpStatus.NOT_FOUND));

            if (!session.getUser().getId().equals(user.getId())) {
                throw new BusinessException(
                        "해당 세션에 접근할 권한이 없습니다.",
                        HttpStatus.FORBIDDEN);
            }

            RagRouteType routeType = ragTurnRoutingService.resolveRouteType(sessionId, request.getContent());

            // 사용자 메시지 저장
            ConversationMessage userMessage = ConversationMessage.builder()
                    .consultationSession(session)
                    .user(user)
                    .content(request.getContent())
                    .messageType(request.getMessageType())
                    .status(MessageStatus.COMPLETED)
                    .sentAt(LocalDateTime.now())
                    .build();

            userMessage = conversationMessageRepository.save(userMessage);

            // 사용자의 최근 N일 수면 데이터 조회
            PeriodRoutingResult routing = resolveRouting(request.getContent(), request.getSleepDataDays());
            SleepContextBuilder.SleepContextResult sleepContext =
                    sleepContextBuilder.buildSleepContext(user, routing);
            Optional<String> sleepDataSummary = Optional.ofNullable(sleepContext.summarySection());

            // AI 응답 생성 (라우팅 적용)
            String aiResponse;
            if (routeType == RagRouteType.NON_RAG) {
                String conversationId = "session_" + sessionId;
                aiResponse = springAIChatService.generateConsultationResponse(
                        request.getContent(),
                        conversationId,
                        sleepDataSummary);
            } else {
                aiResponse = ragEnhancedConsultationService.generateRoutedAnswer(
                        user,
                        sessionId,
                        request.getContent(),
                        sleepContext,
                        routeType);
            }

            List<String> followUpQuestions = followUpQuestionGenerator.generate(request.getContent());
            com.sleepwell.sleepwell_backend.enums.RecommendedFeature feature =
                    recommendedFeatureDetector.detect(request.getContent());

            String augmentedResponse = answerAugmentor.appendFeatureRecommendation(aiResponse, feature);
            augmentedResponse = answerAugmentor.appendFollowUpQuestions(augmentedResponse, followUpQuestions);

            // AI 응답 메시지 저장
            ConversationMessage aiMessage = ConversationMessage.builder()
                    .consultationSession(session)
                    .content(augmentedResponse)
                    .messageType(MessageType.AI_RESPONSE)
                    .status(MessageStatus.COMPLETED)
                    .sentAt(LocalDateTime.now())
                    .build();

            aiMessage = conversationMessageRepository.save(aiMessage);

            // 세션 업데이트 (JPA Auditing으로 updatedAt 자동 갱신)
            consultationSessionRepository.save(session);

            log.info("Message exchange completed");

            // 응답 DTO 생성
            return MessageResponseDto.builder()
                    .messageId(aiMessage.getId())
                    .userMessage(request.getContent())
                    .aiResponse(augmentedResponse)
                    .timestamp(aiMessage.getSentAt())
                    .status(MessageStatus.COMPLETED)
                    .followUpQuestions(followUpQuestions)
                    .recommendedFeature(feature)
                    .build();

        } catch (BusinessException e) {
            log.error("Business error sending message: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error sending message", e);
            throw new BusinessException(
                    "메시지 전송 중 오류가 발생했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public Flux<String> sendMessageStream(User user, Long sessionId, SendMessageRequestDto request) {
        log.info("Sending message with streaming to session: {}, user: {}", sessionId, user.getId());

        try {
            // 1. 동기 DB 작업: 세션 조회 및 권한 확인
            ConsultationSession session = consultationSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new BusinessException(
                            "상담 세션을 찾을 수 없습니다.",
                            HttpStatus.NOT_FOUND));

            if (!session.getUser().getId().equals(user.getId())) {
                throw new BusinessException(
                        "해당 세션에 접근할 권한이 없습니다.",
                        HttpStatus.FORBIDDEN);
            }

            RagRouteType routeType = ragTurnRoutingService.resolveRouteType(sessionId, request.getContent());

            // 2. 동기 DB 작업: 사용자 메시지 저장
            ConversationMessage userMessage = ConversationMessage.builder()
                    .consultationSession(session)
                    .user(user)
                    .content(request.getContent())
                    .messageType(request.getMessageType())
                    .status(MessageStatus.COMPLETED)
                    .sentAt(LocalDateTime.now())
                    .build();

            conversationMessageRepository.save(userMessage);

            // 3. 동기 DB 작업: 수면 데이터 조회
            PeriodRoutingResult routing = resolveRouting(request.getContent(), request.getSleepDataDays());
            SleepContextBuilder.SleepContextResult sleepContext =
                    sleepContextBuilder.buildSleepContext(user, routing);
            Optional<String> sleepDataSummary = Optional.ofNullable(sleepContext.summarySection());

            // 4. 비동기 스트리밍: AI 응답 생성
            String conversationId = "session_" + sessionId;
            StringBuilder responseBuilder = new StringBuilder();

            String streamPrompt = request.getContent();
            Optional<String> streamSummary = sleepDataSummary;
            if (routeType != RagRouteType.NON_RAG) {
                streamPrompt = ragEnhancedConsultationService.prepareRagPromptForRoute(
                        sessionId,
                        request.getContent(),
                        sleepContext,
                        routeType);
                streamSummary = Optional.empty();
                if (streamPrompt == null || streamPrompt.isBlank()) {
                    streamPrompt = request.getContent();
                    streamSummary = sleepDataSummary;
                }
            }

            return springAIChatService.streamConsultationResponse(
                    streamPrompt,
                    conversationId,
                    streamSummary)
                    .doOnNext(responseBuilder::append)
                    .doOnComplete(() -> {
                        // 5. 스트리밍 완료 후 DB 저장 (별도 트랜잭션)
                        saveAiMessageTransactional(session, responseBuilder.toString());
                        log.info("Streaming message exchange completed for session: {}", sessionId);
                    })
                    .doOnError(error -> {
                        log.error("Error during streaming for session {}: {}", sessionId, error.getMessage());
                    });

        } catch (BusinessException e) {
            log.error("Business error during streaming setup: {}", e.getMessage());
            return Flux.just("죄송합니다. " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during streaming setup", e);
            return Flux.just("죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * AI 응답 메시지를 별도 트랜잭션으로 저장
     * (Reactive 스트림과 트랜잭션 분리)
     */
    @Transactional
    private void saveAiMessageTransactional(ConsultationSession session, String aiResponse) {
        try {
            ConversationMessage aiMessage = ConversationMessage.builder()
                    .consultationSession(session)
                    .content(aiResponse)
                    .messageType(MessageType.AI_RESPONSE)
                    .status(MessageStatus.COMPLETED)
                    .sentAt(LocalDateTime.now())
                    .build();

            conversationMessageRepository.save(aiMessage);

            // 세션 업데이트 (JPA Auditing으로 updatedAt 자동 갱신)
            consultationSessionRepository.save(session);

            log.debug("AI message saved for session: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to save AI message: {}", e.getMessage(), e);
        }
    }

    @Override
    public Page<ConsultationSessionResponseDto> getUserSessions(User user, Pageable pageable) {
        log.info("Getting sessions for user: {}", user.getId());

        try {
            Page<ConsultationSession> sessions = consultationSessionRepository
                    .findByUserOrderByConsultationTimeDesc(user, pageable);

            return sessions.map(this::mapToResponseDto);

        } catch (BusinessException e) {
            log.error("Business error getting user sessions: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting user sessions", e);
            throw new BusinessException(
                    "사용자 세션 조회 중 오류가 발생했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public ConsultationSessionDetailDto getSessionDetail(User user, Long sessionId) {
        log.info("Getting session detail: {}, user: {}", sessionId, user.getId());

        try {
            ConsultationSession session = consultationSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new BusinessException(
                            "상담 세션을 찾을 수 없습니다.",
                            HttpStatus.NOT_FOUND));

            // 권한 확인
            if (!session.getUser().getId().equals(user.getId())) {
                throw new BusinessException(
                        "해당 세션에 접근할 권한이 없습니다.",
                        HttpStatus.FORBIDDEN);
            }

            // 메시지 목록 조회
            List<ConversationMessage> messages = conversationMessageRepository
                    .findByConsultationSessionOrderBySentAtAsc(session);

            List<MessageResponseDto> messageResponses = messages.stream()
                    .map(this::mapToMessageResponseDto)
                    .collect(Collectors.toList());

            return ConsultationSessionDetailDto.builder()
                    .sessionId(session.getId())
                    .status(session.getStatus())
                    .topic(session.getTopic())
                    .sessionType(session.getSessionType())
                    .title(session.getTitle())
                    .startedAt(session.getConsultationTime())
                    .endedAt(session.getEndTime())
                    .voiceEnabled(session.getVoiceEnabled())
                    .messages(messageResponses)
                    .messageCount(messageResponses.size())
                    .build();

        } catch (BusinessException e) {
            log.error("Business error getting session detail: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting session detail", e);
            throw new BusinessException(
                    "세션 상세 조회 중 오류가 발생했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    @Transactional
    public SessionEndResponseDto endSession(User user, Long sessionId, EndSessionRequestDto request) {
        log.info("Ending consultation session: {}, user: {}", sessionId, user.getId());

        try {
            // 세션 조회 및 권한 확인
            ConsultationSession session = consultationSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new BusinessException(
                            "상담 세션을 찾을 수 없습니다.",
                            HttpStatus.NOT_FOUND));

            if (!session.getUser().getId().equals(user.getId())) {
                throw new BusinessException(
                        "해당 세션에 접근할 권한이 없습니다.",
                        HttpStatus.FORBIDDEN);
            }

            // 세션 종료 처리
            session.endSession(); // 비즈니스 메서드 사용
            session = consultationSessionRepository.save(session);

            // 대화 기록 수집
            List<ConversationMessage> messages = conversationMessageRepository
                    .findByConsultationSessionOrderBySentAtAsc(session);

            String conversationHistory = messages.stream()
                    .map(msg -> msg.getMessageType() + ": " + msg.getContent())
                    .collect(Collectors.joining("\n"));

            // AI 세션 요약 생성
            String sessionSummary = springAIChatService.generateSessionSummary(conversationHistory);

            // 요약 저장
            ConsultationSummary summary = ConsultationSummary.builder()
                    .consultationSession(session)
                    .user(user) // user 필드 추가 (nullable = false 제약조건)
                    .summaryText(sessionSummary)
                    .recommendations("[\"AI 생성 추천사항\"]") // JSON 배열 형식으로 수정
                    .build();
            consultationSummaryRepository.save(summary);

            log.info("Session ended successfully: {}", sessionId);

            return SessionEndResponseDto.builder()
                    .sessionId(sessionId)
                    .endedAt(session.getEndTime())
                    .totalDurationMinutes(session.getTotalDurationMinutes())
                    .summaryGenerated(true)
                    .message(sessionSummary)
                    .build();

        } catch (BusinessException e) {
            log.error("Business error ending session: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error ending session", e);
            throw new BusinessException(
                    "세션 종료 중 오류가 발생했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public ConsultationSummaryDto getSessionSummary(User user, Long sessionId) {
        log.info("Getting session summary: {}, user: {}", sessionId, user.getId());

        try {
            ConsultationSession session = consultationSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new BusinessException(
                            "상담 세션을 찾을 수 없습니다.",
                            HttpStatus.NOT_FOUND));

            // 권한 확인
            if (!session.getUser().getId().equals(user.getId())) {
                throw new BusinessException(
                        "해당 세션에 접근할 권한이 없습니다.",
                        HttpStatus.FORBIDDEN);
            }

            ConsultationSummary summary = consultationSummaryRepository.findByConsultationSession(session)
                    .orElseThrow(() -> new BusinessException(
                            "상담 요약을 찾을 수 없습니다.",
                            HttpStatus.NOT_FOUND));

            return ConsultationSummaryDto.builder()
                    .sessionId(sessionId)
                    .summary(summary.getSummaryText())
                    .keyInsights(summary.getRecommendations())
                    .createdAt(summary.getCreatedAt())
                    .build();

        } catch (BusinessException e) {
            log.error("Business error getting session summary: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting session summary", e);
            throw new BusinessException(
                    "세션 요약 조회 중 오류가 발생했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    @Transactional
    public void submitFeedback(User user, Long sessionId, FeedbackRequestDto request) {
        log.info("Submitting feedback for session: {}, user: {}", sessionId, user.getId());

        try {
            ConsultationSession session = consultationSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new BusinessException(
                            "상담 세션을 찾을 수 없습니다.",
                            HttpStatus.NOT_FOUND));

            // 권한 확인
            if (!session.getUser().getId().equals(user.getId())) {
                throw new BusinessException(
                        "해당 세션에 접근할 권한이 없습니다.",
                        HttpStatus.FORBIDDEN);
            }

            // 피드백 정보 업데이트 (JPA Auditing으로 updatedAt 자동 갱신)
            consultationSessionRepository.save(session);

            log.info("Feedback submitted successfully for session: {}", sessionId);

        } catch (BusinessException e) {
            log.error("Business error submitting feedback: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error submitting feedback", e);
            throw new BusinessException(
                    "피드백 제출 중 오류가 발생했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public AvailableAiModelsDto getAvailableModels() {
        log.info("Getting available AI models");

        try {
            return AvailableAiModelsDto.builder()
                    .defaultModel(defaultAiModel) // yml 설정에서 가져옴
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error getting available models", e);
            throw new BusinessException(
                    "AI 모델 정보 조회 중 오류가 발생했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    @Transactional
    public ConsultationSessionResponseDto createAutomaticSession(User user, String sleepAnalysisData) {
        log.info("Creating automatic session for user: {}", user.getId());

        try {
            // 개인화된 수면 계획 생성
            String personalizedPlan = springAIChatService.generatePersonalizedSleepPlan(
                    sleepAnalysisData,
                    "user_" + user.getId());

            // 자동 세션 생성
            ConsultationSession session = ConsultationSession.builder()
                    .user(user)
                    .consultationTime(LocalDateTime.now())
                    .status(SessionStatus.IN_PROGRESS)
                    .topic(ConsultationTopic.SLEEP_QUALITY)
                    .sessionType(SessionType.AI_CONSULTATION)
                    .title("AI 수면 분석 기반 자동 상담")
                    .initialDescription("수면 분석 결과를 기반으로 생성된 자동 상담 세션입니다.")
                    .voiceEnabled(false)
                    .estimatedDurationMinutes(15)
                    .build();

            session = consultationSessionRepository.save(session);

            // 초기 AI 메시지 생성
            ConversationMessage initialMessage = ConversationMessage.builder()
                    .consultationSession(session)
                    .content(personalizedPlan)
                    .messageType(MessageType.AI_RESPONSE)
                    .status(MessageStatus.COMPLETED)
                    .sentAt(LocalDateTime.now())
                    .build();

            conversationMessageRepository.save(initialMessage);

            log.info("Automatic session created: {}", session.getId());

            return ConsultationSessionResponseDto.builder()
                    .sessionId(session.getId())
                    .status(session.getStatus())
                    .topic(session.getTopic())
                    .sessionType(session.getSessionType())
                    .startedAt(session.getConsultationTime())
                    .estimatedDurationMinutes(session.getEstimatedDurationMinutes())
                    .voiceEnabled(session.getVoiceEnabled())
                    .totalMessages(1)
                    .unreadMessages(1)
                    .lastActivityAt(session.getConsultationTime())
                    .initialMessage(session.getInitialDescription())
                    .suggestedPrompts(ConsultationSessionResponseDto.defaultSuggestedPrompts())
                    .build();

        } catch (BusinessException e) {
            log.error("Business error creating automatic session: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating automatic session", e);
            throw new BusinessException(
                    "자동 상담 세션 생성 중 오류가 발생했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    // === Private Helper Methods ===

    /**
     * ConsultationSession을 ConsultationSessionResponseDto로 변환
     */
    private ConsultationSessionResponseDto mapToResponseDto(ConsultationSession session) {
        return ConsultationSessionResponseDto.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .topic(session.getTopic())
                .sessionType(session.getSessionType())
                .startedAt(session.getConsultationTime())
                .endedAt(session.getEndTime())
                .actualDurationMinutes(session.getTotalDurationMinutes())
                .voiceEnabled(session.getVoiceEnabled())
                .estimatedDurationMinutes(session.getEstimatedDurationMinutes())
                .totalMessages(session.getMessageCount())
                .unreadMessages(0) // 기본값
                .lastActivityAt(session.getUpdatedAt() != null ? session.getUpdatedAt() : session.getConsultationTime())
                .qualityScore(session.getAiQualityScore())
                .initialMessage(session.getInitialDescription())
                .suggestedPrompts(ConsultationSessionResponseDto.defaultSuggestedPrompts())
                .build();
    }

    /**
     * ConversationMessage를 MessageResponseDto로 변환
     */
    private MessageResponseDto mapToMessageResponseDto(ConversationMessage message) {
        return MessageResponseDto.builder()
                .messageId(message.getId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .timestamp(message.getSentAt())
                .status(message.getStatus())
                .build();
    }

    /**
     * 최근 N일 수면 데이터 요약 조회
     */
    private Optional<String> getSleepDataSummary(User user, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<SleepRecord> records = sleepRecordRepository.findByDateRange(user, startDate, endDate);

        if (records.isEmpty()) {
            return Optional.empty();
        }

        // 유효한 데이터만 필터링
        List<SleepRecord> validRecords = records.stream()
                .filter(r -> r.getTotalSleepMinutes() != null)
                .filter(r -> r.getTotalSleepMinutes() > 0 && r.getTotalSleepMinutes() <= 1440)
                .toList();

        if (validRecords.isEmpty()) {
            return Optional.empty();
        }

        // 통계 계산
        double avgDuration = validRecords.stream()
                .mapToDouble(r -> r.getTotalSleepMinutes() / 60.0)
                .average()
                .orElse(0.0);

        double avgEfficiency = validRecords.stream()
                .map(r -> {
                    // 1. sleepInBedMinutes 사용 (가장 정확)
                    if (r.getSleepInBedMinutes() != null && r.getSleepInBedMinutes() > 0) {
                        return (double) r.getTotalSleepMinutes() / r.getSleepInBedMinutes() * 100;
                    }

                    // 2. sleepAwakeMinutes 활용 (totalSleepMinutes + sleepAwakeMinutes = 침대 시간)
                    if (r.getSleepAwakeMinutes() != null && r.getSleepAwakeMinutes() >= 0) {
                        int timeInBed = r.getTotalSleepMinutes() + r.getSleepAwakeMinutes();
                        if (timeInBed > 0) {
                            return (double) r.getTotalSleepMinutes() / timeInBed * 100;
                        }
                    }

                    // 3. 수면 시작/종료 시간으로 계산
                    if (r.getSleepStartTime() != null && r.getSleepEndTime() != null) {
                        long bedTimeMinutes = java.time.Duration.between(
                            r.getSleepStartTime(),
                            r.getSleepEndTime()
                        ).toMinutes();
                        if (bedTimeMinutes > 0) {
                            return (double) r.getTotalSleepMinutes() / bedTimeMinutes * 100;
                        }
                    }

                    // 4. 단계별 수면 합계 기반 추정 (마지막 수단)
                    Integer stageSumMinutes = r.calculateTotalSleepStageMinutes();
                    if (stageSumMinutes != null && stageSumMinutes > 0) {
                        return (double) stageSumMinutes / r.getTotalSleepMinutes() * 100;
                    }

                    return null;
                })
                .filter(eff -> eff != null && eff > 0 && eff <= 100)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double avgWakeupCount = validRecords.stream()
                .filter(r -> r.getWakeupCount() != null)
                .mapToDouble(SleepRecord::getWakeupCount)
                .average()
                .orElse(0.0);

        // 설문 데이터 통합 (정규화된 테이블에서 조회)
        StringBuilder surveyData = new StringBuilder();

        // ISI 설문 - 문항별 상세 데이터 포함
        isiResponseRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
            .ifPresent(isi -> {
                surveyData.append(String.format(
                    "\n\n=== ISI (불면증 심각도 지수) ===\n" +
                    "총점: %d점 / 28점 (높을수록 심각) - %s\n" +
                    "측정일: %s\n" +
                    "문항별 점수 (0-4점, 높을수록 문제 심각):\n" +
                    "  • 잠들기 어려움 (q1a): %d점%s\n" +
                    "  • 잠 유지 어려움 (q1b): %d점%s\n" +
                    "  • 너무 일찍 깸 (q1c): %d점%s\n" +
                    "  • 수면 패턴 만족도 (q2): %d점 (낮을수록 불만족)%s\n" +
                    "  • 타인의 문제 인지 (q3): %d점%s\n" +
                    "  • 수면 문제 걱정 (q4): %d점%s\n" +
                    "  • 일상 생활 방해 (q5): %d점%s",
                    isi.getTotalScore(),
                    isi.getInterpretation(),
                    isi.getCreatedAt().toLocalDate(),
                    isi.getQ1a(), getAlertIfHigh(isi.getQ1a(), 3),
                    isi.getQ1b(), getAlertIfHigh(isi.getQ1b(), 3),
                    isi.getQ1c(), getAlertIfHigh(isi.getQ1c(), 3),
                    isi.getQ2(), getAlertIfHigh(isi.getQ2(), 3),
                    isi.getQ3(), getAlertIfHigh(isi.getQ3(), 3),
                    isi.getQ4(), getAlertIfHigh(isi.getQ4(), 3),
                    isi.getQ5(), getAlertIfHigh(isi.getQ5(), 3)
                ));

                // 주요 문제 패턴 분석
                String pattern = analyzeSleepPattern(isi);
                if (!pattern.isEmpty()) {
                    surveyData.append("\n주요 불면 패턴: ").append(pattern);
                }
            });

        // ESS 설문 - 문항별 상세 데이터 포함
        essResponseRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
            .ifPresent(ess -> {
                surveyData.append(String.format(
                    "\n\n=== ESS (주간 졸림증 척도) ===\n" +
                    "총점: %d점 / 24점 (높을수록 졸림 심함) - %s\n" +
                    "측정일: %s\n" +
                    "상황별 졸림 정도 (0-3점, 높을수록 졸림):\n" +
                    "  • 앉아서 독서: %d점%s\n" +
                    "  • TV 시청: %d점%s\n" +
                    "  • 공공장소 조용히 앉음: %d점%s\n" +
                    "  • 1시간 이상 차량 탑승: %d점%s\n" +
                    "  • 오후 휴식: %d점%s\n" +
                    "  • 앉아서 대화: %d점%s\n" +
                    "  • 점심 후 조용히 앉음: %d점%s\n" +
                    "  • 차 안에서 잠깐 정차: %d점%s",
                    ess.getTotalScore(),
                    ess.getInterpretation(),
                    ess.getCreatedAt().toLocalDate(),
                    ess.getQ1(), getAlertIfHigh(ess.getQ1(), 2),
                    ess.getQ2(), getAlertIfHigh(ess.getQ2(), 2),
                    ess.getQ3(), getAlertIfHigh(ess.getQ3(), 2),
                    ess.getQ4(), getAlertIfHigh(ess.getQ4(), 2),
                    ess.getQ5(), getAlertIfHigh(ess.getQ5(), 2),
                    ess.getQ6(), getAlertIfHigh(ess.getQ6(), 2),
                    ess.getQ7(), getAlertIfHigh(ess.getQ7(), 2),
                    ess.getQ8(), getAlertIfHigh(ess.getQ8(), 2)
                ));
            });

        // PSQI 설문 - Component별 상세 데이터 포함
        psqiResponseRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
            .ifPresent(psqi -> {
                int latencyHours = psqi.getQ2H();
                int latencyMinutes = psqi.getQ2M();
                int durationHours = psqi.getQ4H();
                int durationMinutes = psqi.getQ4M();

                surveyData.append(String.format(
                    "\n\n=== PSQI (피츠버그 수면의 질 지수) ===\n" +
                    "총점: %d점 / 21점 (높을수록 수면의 질 나쁨) - %s\n" +
                    "측정일: %s\n" +
                    "\n수면 스케줄:\n" +
                    "  • 평소 취침 시간: %s%s\n" +
                    "  • 평소 기상 시간: %s%s\n" +
                    "  • 잠들기까지 걸린 시간: %d시간 %d분%s\n" +
                    "  • 실제 수면 시간: %d시간 %d분%s\n" +
                    "\nPSQI 7개 Component 점수 (0-3점, 높을수록 문제):\n" +
                    "  C1. 주관적 수면의 질: %d점%s\n" +
                    "  C2. 수면 잠복기 (입면 시간): %d점%s\n" +
                    "  C3. 수면 지속 시간: %d점%s\n" +
                    "  C4. 수면 효율성: %d점%s [중요 지표]\n" +
                    "  C5. 수면 장애 빈도: %d점%s\n" +
                    "  C6. 수면제 사용: %d점%s\n" +
                    "  C7. 주간 기능 장애: %d점%s",
                    psqi.getTotalScore(),
                    psqi.getInterpretation(),
                    psqi.getCreatedAt().toLocalDate(),
                    psqi.getQ1Bedtime(), isBedtimeLate(psqi.getQ1Bedtime()),
                    psqi.getQ3Waketime(), isWakeTimeLate(psqi.getQ3Waketime()),
                    latencyHours, latencyMinutes, latencyHours > 0 || latencyMinutes > 30 ? " ⚠️" : "",
                    durationHours, durationMinutes, durationHours < 6 ? " ⚠️" : "",
                    psqi.getComponent1Quality(), getAlertIfHigh(psqi.getComponent1Quality(), 2),
                    psqi.getComponent2Latency(), getAlertIfHigh(psqi.getComponent2Latency(), 2),
                    psqi.getComponent3Duration(), getAlertIfHigh(psqi.getComponent3Duration(), 2),
                    psqi.getComponent4Efficiency(), getAlertIfHigh(psqi.getComponent4Efficiency(), 2),
                    psqi.getComponent5Disturbance(), getAlertIfHigh(psqi.getComponent5Disturbance(), 2),
                    psqi.getComponent6Medication(), getAlertIfHigh(psqi.getComponent6Medication(), 1),
                    psqi.getComponent7Dysfunction(), getAlertIfHigh(psqi.getComponent7Dysfunction(), 2)
                ));
            });

        // BDI 설문 - 자살 위험 경고 및 문항별 상세 데이터 포함
        bdiResponseRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
            .ifPresent(bdi -> {
                surveyData.append(String.format(
                    "\n\n=== BDI-II (벡 우울 척도) ===\n" +
                    "총점: %d점 / 63점 (높을수록 우울 심각) - %s\n" +
                    "측정일: %s\n",
                    bdi.getTotalScore(),
                    bdi.getInterpretation(),
                    bdi.getCreatedAt().toLocalDate()
                ));

                if (Boolean.TRUE.equals(bdi.getSuicideRisk())) {
                    surveyData.append("⚠️⚠️⚠️ [CRITICAL ALERT] 자살 위험 감지됨 (q9 >= 2점) ⚠️⚠️⚠️\n")
                              .append("→ 즉시 전문가 개입 필요\n")
                              .append("→ 상담 시 안전 확보 최우선\n")
                              .append("→ 자살예방상담전화 1393 안내 필수\n\n");
                }

                surveyData.append(String.format(
                    "주요 증상 영역 (0-3점, 높을수록 심각):\n" +
                    "  • 슬픔 (q1): %d점%s\n" +
                    "  • 비관 (q2): %d점%s\n" +
                    "  • 과거 실패감 (q3): %d점%s\n" +
                    "  • 즐거움 상실 (q4): %d점%s\n" +
                    "  • 죄책감 (q5): %d점%s\n" +
                    "  • 처벌 감정 (q6): %d점%s\n" +
                    "  • 자기 혐오 (q7): %d점%s\n" +
                    "  • 자기 비난 (q8): %d점%s\n" +
                    "  • 자살 사고 (q9): %d점%s [최우선 모니터링]\n" +
                    "  • 눈물 (q10): %d점%s\n" +
                    "  • 초조 (q11): %d점%s\n" +
                    "  • 흥미 상실 (q12): %d점%s\n" +
                    "  • 우유부단 (q13): %d점%s\n" +
                    "  • 무가치감 (q14): %d점%s\n" +
                    "  • 에너지 상실 (q15): %d점%s\n" +
                    "  • 수면 패턴 변화 (q16): %d점%s [수면 관련]\n" +
                    "  • 짜증 (q17): %d점%s\n" +
                    "  • 식욕 변화 (q18): %d점%s\n" +
                    "  • 집중 곤란 (q19): %d점%s\n" +
                    "  • 피로감 (q20): %d점%s\n" +
                    "  • 성적 흥미 상실 (q21): %d점%s",
                    bdi.getQ1(), getAlertIfHigh(bdi.getQ1(), 2),
                    bdi.getQ2(), getAlertIfHigh(bdi.getQ2(), 2),
                    bdi.getQ3(), getAlertIfHigh(bdi.getQ3(), 2),
                    bdi.getQ4(), getAlertIfHigh(bdi.getQ4(), 2),
                    bdi.getQ5(), getAlertIfHigh(bdi.getQ5(), 2),
                    bdi.getQ6(), getAlertIfHigh(bdi.getQ6(), 2),
                    bdi.getQ7(), getAlertIfHigh(bdi.getQ7(), 2),
                    bdi.getQ8(), getAlertIfHigh(bdi.getQ8(), 2),
                    bdi.getQ9(), bdi.getQ9() >= 1 ? " ⚠️⚠️⚠️" : "",
                    bdi.getQ10(), getAlertIfHigh(bdi.getQ10(), 2),
                    bdi.getQ11(), getAlertIfHigh(bdi.getQ11(), 2),
                    bdi.getQ12(), getAlertIfHigh(bdi.getQ12(), 2),
                    bdi.getQ13(), getAlertIfHigh(bdi.getQ13(), 2),
                    bdi.getQ14(), getAlertIfHigh(bdi.getQ14(), 2),
                    bdi.getQ15(), getAlertIfHigh(bdi.getQ15(), 2),
                    bdi.getQ16(), getAlertIfHigh(bdi.getQ16(), 2),
                    bdi.getQ17(), getAlertIfHigh(bdi.getQ17(), 2),
                    bdi.getQ18(), getAlertIfHigh(bdi.getQ18(), 2),
                    bdi.getQ19(), getAlertIfHigh(bdi.getQ19(), 2),
                    bdi.getQ20(), getAlertIfHigh(bdi.getQ20(), 2),
                    bdi.getQ21(), getAlertIfHigh(bdi.getQ21(), 2)
                ));
            });

        String surveyDataStr = surveyData.length() > 0 ?
            "\n\n사용자의 최근 설문 평가:" + surveyData.toString() : "";

        return Optional.of(String.format(
                "사용자의 최근 %d일 수면 데이터:\n" +
                        "- 평균 수면 시간: %.1f시간\n" +
                        "- 평균 수면 효율: %.1f%%\n" +
                        "- 평균 야간 각성: %.1f회\n" +
                        "- 데이터 기록 일수: %d일%s",
                days,
                avgDuration,
                avgEfficiency,
                avgWakeupCount,
                validRecords.size(),
                surveyDataStr));
    }

    private String createSleepDataSummary(SleepRecord sleepRecord) {
        return String.format(
                "사용자의 최근 수면 데이터 (%s):\n" +
                        "- 수면 점수: %d/100\n" +
                        "- 총 수면 시간: %d분\n" +
                        "- 깊은 수면: %d분\n" +
                        "- 얕은 수면: %d분\n" +
                        "- REM 수면: %d분\n" +
                        "- 깨어난 횟수: %d회",
                sleepRecord.getRecordDate(),
                sleepRecord.getSleepQualityScore(),
                sleepRecord.getTotalSleepMinutes(),
                sleepRecord.getDeepSleepMinutes(),
                sleepRecord.getLightSleepMinutes(),
                sleepRecord.getRemSleepMinutes(),
                sleepRecord.getWakeupCount());
    }

    /**
     * 점수가 임계값 이상일 경우 경고 표시
     */
    private String getAlertIfHigh(Integer score, int threshold) {
        return (score != null && score >= threshold) ? " ⚠️" : "";
    }

    /**
     * ISI 응답에서 주요 불면 패턴 분석
     */
    private String analyzeSleepPattern(ISIResponse isi) {
        List<String> issues = new ArrayList<>();
        if (isi.getQ1a() != null && isi.getQ1a() >= 3) {
            issues.add("입면 장애 (잠들기 어려움)");
        }
        if (isi.getQ1b() != null && isi.getQ1b() >= 3) {
            issues.add("수면 유지 장애 (자주 깸)");
        }
        if (isi.getQ1c() != null && isi.getQ1c() >= 3) {
            issues.add("조기 각성 (너무 일찍 깸)");
        }
        if (isi.getQ5() != null && isi.getQ5() >= 3) {
            issues.add("일상 생활 방해 심각");
        }
        return String.join(", ", issues);
    }

    /**
     * 취침 시간이 늦은지 판단 (23시 이후)
     */
    private String isBedtimeLate(String bedTime) {
        if (bedTime == null || bedTime.isEmpty()) {
            return "";
        }
        try {
            String[] parts = bedTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            // 23시 이후 또는 새벽 2시 이전을 늦은 시간으로 간주
            if (hour >= 23 || hour < 2) {
                return " ⚠️ (늦은 취침)";
            }
        } catch (Exception e) {
            log.warn("Invalid bed time format: {}", bedTime);
        }
        return "";
    }

    /**
     * 기상 시간이 늦은지 판단 (9시 이후)
     */
    private String isWakeTimeLate(String wakeTime) {
        if (wakeTime == null || wakeTime.isEmpty()) {
            return "";
        }
        try {
            String[] parts = wakeTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            // 9시 이후를 늦은 기상으로 간주
            if (hour >= 9) {
                return " ⚠️ (늦은 기상)";
            }
        } catch (Exception e) {
            log.warn("Invalid wake time format: {}", wakeTime);
        }
        return "";
    }

    private PeriodRoutingResult resolveRouting(String content, Integer sleepDataDays) {
        PeriodRoutingResult routing = sleepPeriodParser.parse(content);
        if (sleepDataDays != null && sleepDataDays > 0 && routing.isDefault()) {
            return PeriodRoutingResult.forRecentDays(sleepDataDays);
        }
        return routing;
    }
}
