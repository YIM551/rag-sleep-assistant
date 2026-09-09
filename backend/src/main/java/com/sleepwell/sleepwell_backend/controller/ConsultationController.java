package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.*;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.ActivityEventType;
import com.sleepwell.sleepwell_backend.enums.ConsultationTopic;
import com.sleepwell.sleepwell_backend.enums.RecommendedFeature;
import com.sleepwell.sleepwell_backend.event.AIFeatureClickEvent;
import com.sleepwell.sleepwell_backend.service.FollowUpQuestionGenerator;
import com.sleepwell.sleepwell_backend.service.RecommendedFeatureDetector;
import com.sleepwell.sleepwell_backend.service.SpringAIChatService;
import com.sleepwell.sleepwell_backend.service.ConsultationService;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 기반 수면 상담 컨트롤러
 * 개인화된 수면 코칭과 대화형 상담 서비스를 제공합니다.
 */
@RestController
@RequestMapping("/api/consultation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI 수면 상담", description = "AI 기반 개인화 수면 상담 및 코칭 서비스")
@SecurityRequirement(name = "bearerAuth")
public class ConsultationController {

    private final SpringAIChatService springAIChatService;
    private final ConsultationService consultationService;
    private final com.sleepwell.sleepwell_backend.service.RagEnhancedConsultationService ragEnhancedConsultationService;
    private final ApplicationEventPublisher eventPublisher;
    private final FollowUpQuestionGenerator followUpQuestionGenerator;
    private final RecommendedFeatureDetector recommendedFeatureDetector;
    private final ObjectMapper objectMapper;

    @Operation(
        summary = "상담 세션 시작",
        description = "새로운 AI 수면 상담 세션을 시작합니다. 사용자의 수면 데이터를 기반으로 개인화된 상담을 제공합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "상담 세션 생성 성공"),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.sleepwell.sleepwell_backend.exception.ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error Example",
                    summary = "상담 시작 유효성 검증 실패 예시",
                    value = """
                    {
                        "timestamp": "2025-07-11T11:00:00.123456",
                        "status": 400,
                        "error": "Validation Failed",
                        "message": "입력 값 유효성 검사에 실패했습니다.",
                        "path": "/api/v1/consultation/sessions",
                        "details": {
                            "topic": [
                                "상담 주제는 필수입니다"
                            ],
                            "initialMessage": [
                                "초기 메시지는 1000자를 초과할 수 없습니다"
                            ]
                        }
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "429", description = "요청 한도 초과 (Rate Limit)"),
        @ApiResponse(responseCode = "500", description = "내부 서버 오류"),
        @ApiResponse(responseCode = "502", description = "AI 서비스 호출 실패 (OpenAI/Claude API 오류)"),
        @ApiResponse(responseCode = "503", description = "AI 서비스 일시적 사용 불가")
    })
    @PostMapping("/sessions")
    public ResponseEntity<ConsultationSessionResponseDto> startConsultationSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StartConsultationRequestDto request) {
        
        log.info("Authentication received: {}", userDetails != null ? userDetails.getUsername() : "null");
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        
        User user = userDetails.getUser();

        log.info("Starting consultation session for user: {}, topic: {}",
                user.getId(), request.getTopic());

        // AI 기능 클릭 이벤트 발행 (사용자 활동 추적)
        try {
            String category = mapTopicToCategory(request.getTopic());
            ActivityEventType eventType = AIFeatureClickEvent.mapCategoryToEventType(category);

            AIFeatureClickEvent clickEvent = new AIFeatureClickEvent(
                user.getId(),
                user.getEmail(),
                eventType,
                category,
                java.util.Map.of(
                    "initialMessage", request.getInitialMessage() != null ? request.getInitialMessage() : "",
                    "topic", request.getTopic() != null ? request.getTopic() : ""
                )
            );

            eventPublisher.publishEvent(clickEvent);
            log.debug("AIFeatureClickEvent 발행 완료 - userId: {}, category: {}", user.getId(), category);
        } catch (Exception e) {
            // 이벤트 발행 실패는 메인 상담 프로세스에 영향을 주지 않음
            log.warn("AIFeatureClickEvent 발행 실패: {}", e.getMessage());
        }

        ConsultationSessionResponseDto response = consultationService.startSession(user, request);

        return ResponseEntity.status(201).body(response);
    }

    @Operation(
        summary = "메시지 전송",
        description = "상담 세션에 메시지를 전송합니다. 텍스트 또는 음성 메시지를 지원합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 전송 성공"),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.sleepwell.sleepwell_backend.exception.ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error Example",
                    summary = "메시지 전송 유효성 검증 실패 예시",
                    value = """
                    {
                        "timestamp": "2025-07-11T11:05:00.123456",
                        "status": 400,
                        "error": "Validation Failed",
                        "message": "입력 값 유효성 검사에 실패했습니다.",
                        "path": "/api/v1/consultation/sessions/123/messages",
                        "details": {
                            "messageType": [
                                "메시지 타입은 필수입니다"
                            ],
                            "content": [
                                "메시지 내용은 2000자를 초과할 수 없습니다"
                            ]
                        }
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "429", description = "요청 한도 초과 (Rate Limit)"),
        @ApiResponse(responseCode = "502", description = "AI 서비스 호출 실패 (OpenAI/Claude API 오류)"),
        @ApiResponse(responseCode = "503", description = "AI 서비스 일시적 사용 불가")
    })
    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<MessageResponseDto> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "상담 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequestDto request) {

        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        log.info("Sending message to session: {}, user: {}, type: {}",
                sessionId, user.getId(), request.getMessageType());

        MessageResponseDto response = consultationService.sendMessage(user, sessionId, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "메시지 전송 (SSE 스트리밍)",
        description = """
            상담 세션에 메시지를 전송하고 AI 응답을 실시간 스트리밍으로 받습니다.

            ✅ ChatGPT와 같은 타이핑 효과 제공
            ✅ 세션 격리: conversationId로 사용자별 대화 완전 분리
            ✅ 동일 모델: OpenAI GPT-4o (일관된 응답 품질)

            클라이언트는 Server-Sent Events (SSE)를 사용하여 실시간으로 응답을 수신합니다.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "스트리밍 시작 성공 (text/event-stream)"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @PostMapping(value = "/sessions/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sendMessageStream(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "상담 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequestDto request) {

        if (userDetails == null) {
            return Flux.just(
                    ServerSentEvent.<String>builder()
                            .event("error")
                            .data("인증 정보가 없습니다.")
                            .build()
            );
        }

        User user = userDetails.getUser();
        log.info("Starting streaming message to session: {}, user: {}", sessionId, user.getId());

        ServerSentEvent<String> metaEvent = buildMetaEvent(request.getContent());
        ServerSentEvent<String> doneEvent = ServerSentEvent.<String>builder()
                .event("done")
                .data("[DONE]")
                .build();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Flux<ServerSentEvent<String>> messageEvents = consultationService.sendMessageStream(user, sessionId, request)
                .map(content -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(content)
                        .build())
                .timeout(Duration.ofSeconds(60))
                .doOnError(errorRef::set)
                .onErrorResume(error -> Flux.empty());

        Flux<ServerSentEvent<String>> errorEvents = Flux.defer(() -> {
            if (errorRef.get() == null) {
                return Flux.empty();
            }
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("스트리밍 중 오류가 발생했습니다.")
                    .build());
        });

        return Flux.concat(messageEvents, errorEvents, Flux.just(metaEvent), Flux.just(doneEvent))
                .doOnComplete(() -> log.info("Streaming completed for session: {}", sessionId))
                .doOnError(error -> log.error("Streaming error for session {}: {}", sessionId, error.getMessage()));
    }

    @Operation(
        summary = "상담 세션 목록 조회",
        description = "사용자의 상담 세션 목록을 조회합니다. 페이징을 지원합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "세션 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/sessions")
    public ResponseEntity<Page<ConsultationSessionResponseDto>> getConsultationSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        log.info("Getting consultation sessions for user: {}", user.getId());
        
        Page<ConsultationSessionResponseDto> sessions = consultationService.getUserSessions(user, pageable);
        
        return ResponseEntity.ok(sessions);
    }

    @Operation(
        summary = "상담 세션 상세 조회",
        description = "특정 상담 세션의 상세 정보와 대화 내역을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "세션 상세 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ConsultationSessionDetailDto> getConsultationSessionDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "상담 세션 ID") @PathVariable Long sessionId) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        log.info("Getting consultation session detail: {}, user: {}", sessionId, user.getId());
        
        ConsultationSessionDetailDto detail = consultationService.getSessionDetail(user, sessionId);
        
        return ResponseEntity.ok(detail);
    }

    @Operation(
        summary = "상담 세션 종료",
        description = "진행 중인 상담 세션을 종료하고 요약을 생성합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "세션 종료 성공"),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.sleepwell.sleepwell_backend.exception.ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error Example",
                    summary = "세션 종료 유효성 검증 실패 예시",
                    value = """
                    {
                        "timestamp": "2025-07-11T11:10:00.123456",
                        "status": 400,
                        "error": "Validation Failed",
                        "message": "입력 값 유효성 검사에 실패했습니다.",
                        "path": "/api/v1/consultation/sessions/123/end",
                        "details": {
                            "userSatisfactionScore": [
                                "만족도는 1 이상이어야 합니다"
                            ],
                            "userFeedback": [
                                "피드백은 1000자를 초과할 수 없습니다"
                            ]
                        }
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<SessionEndResponseDto> endConsultationSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "상담 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody EndSessionRequestDto request) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        log.info("Ending consultation session: {}, user: {}", sessionId, user.getId());
        
        SessionEndResponseDto response = consultationService.endSession(user, sessionId, request);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "상담 요약 조회",
        description = "완료된 상담 세션의 AI 생성 요약과 권장사항을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "상담 요약 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "요약을 찾을 수 없음")
    })
    @GetMapping("/sessions/{sessionId}/summary")
    public ResponseEntity<ConsultationSummaryDto> getConsultationSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "상담 세션 ID") @PathVariable Long sessionId) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        log.info("Getting consultation summary: {}, user: {}", sessionId, user.getId());
        
        ConsultationSummaryDto summary = consultationService.getSessionSummary(user, sessionId);
        
        return ResponseEntity.ok(summary);
    }

    @Operation(
        summary = "상담 피드백 제출",
        description = "완료된 상담에 대한 사용자 피드백을 제출합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "피드백 제출 성공"),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 데이터",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.sleepwell.sleepwell_backend.exception.ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error Example",
                    summary = "피드백 제출 유효성 검증 실패 예시",
                    value = """
                    {
                        "timestamp": "2025-07-11T11:15:00.123456",
                        "status": 400,
                        "error": "Validation Failed",
                        "message": "입력 값 유효성 검사에 실패했습니다.",
                        "path": "/api/v1/consultation/sessions/123/feedback",
                        "details": {
                            "rating": [
                                "평점은 1 이상이어야 합니다"
                            ],
                            "feedback": [
                                "피드백은 1000자를 초과할 수 없습니다"
                            ]
                        }
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @PostMapping("/sessions/{sessionId}/feedback")
    public ResponseEntity<Void> submitFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "상담 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody FeedbackRequestDto request) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        log.info("Submitting feedback for session: {}, user: {}, rating: {}", 
                sessionId, user.getId(), request.getRating());
        
        consultationService.submitFeedback(user, sessionId, request);
        
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "사용 가능한 AI 모델 정보",
        description = "현재 사용 가능한 AI 상담 모델들의 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "AI 모델 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/ai-models")
    public ResponseEntity<AvailableAiModelsDto> getAvailableAiModels() {
        
        log.info("Getting available AI models");
        
        AvailableAiModelsDto models = consultationService.getAvailableModels();
        
        return ResponseEntity.ok(models);
    }

    @Operation(
        summary = "Spring AI 테스트",
        description = "Spring AI ChatClient 동작을 테스트합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "테스트 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "Spring AI 테스트 중 오류 발생")
    })
    @GetMapping("/test-spring-ai")
    public ResponseEntity<String> testSpringAI(
            @Parameter(description = "테스트 메시지") @RequestParam(defaultValue = "안녕하세요, 수면에 대해 상담받고 싶습니다.") String message) {
        
        log.info("Testing Spring AI with message: {}", message);
        
        try {
            String conversationId = "test_" + System.currentTimeMillis();
            String response = springAIChatService.generateConsultationResponse(message, conversationId, Optional.empty());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error testing Spring AI", e);
            return ResponseEntity.status(500).body("Spring AI 테스트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "RAG 강화 상담 (논문 근거 + 개인 데이터)",
        description = """
            수면 의학 논문 검색과 개인 수면 데이터를 결합한 고품질 상담을 제공합니다.

            ## 기능
            - 논문 기반 의학적 근거 제공
            - 개인 수면 패턴 분석
            - 맞춤형 개선 권장사항

            ## 응답 포함 내용
            - AI 답변 (논문 근거 + 개인화)
            - 개인 수면 데이터 요약
            - 논문 인용 목록
            - 실행 가능한 권장사항
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "상담 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RagEnhancedConsultationResponseDto.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/rag-enhanced")
    public ResponseEntity<RagEnhancedConsultationResponseDto> ragEnhancedConsultation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RagEnhancedConsultationRequestDto request) {

        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userDetails.getUser();
        log.info("RAG-enhanced consultation request from user: {}", user.getId());

        RagEnhancedConsultationResponseDto response =
            ragEnhancedConsultationService.generateConsultation(user, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "RAG 강화 상담 스트리밍 (SSE)",
        description = "논문 근거 + 개인 데이터 기반 상담을 스트리밍으로 받습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "스트리밍 시작"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping(value = "/rag-enhanced/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> ragEnhancedConsultationStream(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RagEnhancedConsultationRequestDto request) {

        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userDetails.getUser();
        log.info("RAG-enhanced streaming consultation from user: {}", user.getId());

        ServerSentEvent<String> metaEvent = buildMetaEvent(request.getQuery());
        ServerSentEvent<String> doneEvent = ServerSentEvent.<String>builder()
                .event("done")
                .data("[DONE]")
                .build();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Flux<ServerSentEvent<String>> messageEvents =
                ragEnhancedConsultationService.generateConsultationStream(user, request)
                        .map(content -> ServerSentEvent.<String>builder()
                                .event("message")
                                .data(content)
                                .build())
                        .timeout(Duration.ofSeconds(60))
                        .doOnError(errorRef::set)
                        .onErrorResume(error -> Flux.empty());

        Flux<ServerSentEvent<String>> errorEvents = Flux.defer(() -> {
            if (errorRef.get() == null) {
                return Flux.empty();
            }
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("스트리밍 중 오류가 발생했습니다.")
                    .build());
        });

        return Flux.concat(messageEvents, errorEvents, Flux.just(metaEvent), Flux.just(doneEvent))
                .doOnComplete(() -> log.info("RAG-enhanced streaming completed"))
                .doOnError(error -> log.error("RAG-enhanced streaming error: {}", error.getMessage()));
    }

    // ========== 새로운 세션 기반 RAG 상담 API ==========

    @Operation(
            summary = "RAG 상담 세션 시작 (신규)",
            description = "연속 대화를 위한 RAG 상담 세션을 생성합니다. ChatGPT처럼 대화 맥락이 유지됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "세션 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/rag-sessions")
    public ResponseEntity<RagSessionResponseDto> startRagSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StartRagSessionRequestDto request) {

        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userDetails.getUser();
        log.info("RAG 세션 시작 - userId: {}", user.getId());

        RagSessionResponseDto response = ragEnhancedConsultationService.startRagSession(user, request);

        return ResponseEntity.ok()
                .header("X-Session-Id", String.valueOf(response.getSessionId()))
                .body(response);
    }

    @Operation(
            summary = "RAG 상담 메시지 전송 (연속 대화)",
            description = "생성된 세션에 질문을 전송하고 RAG 기반 답변을 받습니다. 이전 대화 맥락이 유지됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "답변 생성 성공"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "세션 접근 권한 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/rag-sessions/{sessionId}/messages")
    public ResponseEntity<RagEnhancedConsultationResponseDto> sendRagMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @Valid @RequestBody SendRagMessageRequestDto request) {

        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userDetails.getUser();
        log.info("RAG 메시지 전송 - userId: {}, sessionId: {}", user.getId(), sessionId);

        // AI 메시지 전송 추적 (실제 AI 사용량 = 사용자별 질문 횟수)
        try {
            AIFeatureClickEvent clickEvent = new AIFeatureClickEvent(
                    user.getId(),
                    user.getEmail(),
                    ActivityEventType.AI_GENERAL,
                    "RAG_MESSAGE",
                    java.util.Map.of(
                            "sessionId", sessionId,
                            "queryLength", request.getQuery() != null ? request.getQuery().length() : 0
                    )
            );
            eventPublisher.publishEvent(clickEvent);
        } catch (Exception e) {
            log.warn("AI 메시지 추적 이벤트 발행 실패 (메인 기능은 정상 진행): {}", e.getMessage());
        }

        RagEnhancedConsultationResponseDto response =
                ragEnhancedConsultationService.sendRagMessage(user, sessionId, request);

        return ResponseEntity.ok()
                .header("X-Session-Id", String.valueOf(sessionId))
                .header("X-Processing-Time-Ms", String.valueOf(response.getProcessingTimeMs()))
                .body(response);
    }

    @Operation(
            summary = "RAG 상담 세션 종료",
            description = "RAG 상담 세션을 명시적으로 종료합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "세션 종료 성공"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "세션 접근 권한 없음")
    })
    @PostMapping("/rag-sessions/{sessionId}/end")
    public ResponseEntity<SessionEndResponseDto> endRagSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {

        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userDetails.getUser();
        log.info("RAG 세션 종료 - userId: {}, sessionId: {}", user.getId(), sessionId);

        SessionEndResponseDto response = ragEnhancedConsultationService.endRagSession(user, sessionId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "RAG 상담 세션 목록 조회",
            description = "사용자의 모든 RAG 상담 세션 목록을 조회합니다."
    )
    @GetMapping("/rag-sessions")
    public ResponseEntity<Page<RagSessionResponseDto>> getRagSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userDetails.getUser();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        Page<RagSessionResponseDto> sessions = ragEnhancedConsultationService.getRagSessions(user, pageable);

        return ResponseEntity.ok(sessions);
    }

    @Operation(
            summary = "RAG 상담 세션 상세 조회",
            description = "특정 RAG 상담 세션의 전체 대화 내역을 조회합니다."
    )
    @GetMapping("/rag-sessions/{sessionId}")
    public ResponseEntity<ConsultationSessionDetailDto> getRagSessionDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {

        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userDetails.getUser();
        ConsultationSessionDetailDto detail = ragEnhancedConsultationService.getRagSessionDetail(user, sessionId);

        return ResponseEntity.ok(detail);
    }

    /**
     * 상담 주제를 활동 추적 카테고리로 매핑
     *
     * PM 요구사항에 따라 5가지 AI 기능 카테고리로 변환:
     * - SLEEPY (졸림)
     * - INSOMNIA (잠안옴/불면)
     * - SLEEP_TEST (수면검사)
     * - STRESS (스트레스)
     * - ACUPRESSURE (경혈)
     *
     * @param topic 상담 주제 (ConsultationTopic enum)
     * @return 활동 추적 카테고리
     */
    private String mapTopicToCategory(ConsultationTopic topic) {
        if (topic == null) {
            return "GENERAL";
        }

        // ConsultationTopic enum을 AI 활동 카테고리로 매핑
        switch (topic) {
            case INSOMNIA:
            case SLEEP_DISORDERS:
                return "INSOMNIA";

            case SLEEP_QUALITY:
            case SLEEP_APNEA:
            case SNORING:
                return "SLEEP_TEST";

            case STRESS_SLEEP:
                return "STRESS";

            case SLEEP_SCHEDULE:
            case SLEEP_HYGIENE:
            case MEDICATION_SLEEP:
            case GENERAL:
            default:
                // 기본값: 일반 상담으로 분류
                // 현재 enum에는 SLEEPY나 ACUPRESSURE가 없으므로
                // 필요시 추가적인 매핑 로직 구현 필요
                return "GENERAL";
        }
    }

    private ServerSentEvent<String> buildMetaEvent(String message) {
        try {
            var followUpQuestions = followUpQuestionGenerator.generate(message);
            RecommendedFeature feature = recommendedFeatureDetector.detect(message);
            Map<String, Object> payload = Map.of(
                    "followUpQuestions", followUpQuestions,
                    "recommendedFeature", feature
            );
            return ServerSentEvent.<String>builder()
                    .event("meta")
                    .data(objectMapper.writeValueAsString(payload))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to build meta event: {}", e.getMessage());
            return ServerSentEvent.<String>builder()
                    .event("meta")
                    .data("{\"followUpQuestions\":[],\"recommendedFeature\":null}")
                    .build();
        }
    }
} 
