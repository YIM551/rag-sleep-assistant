package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.*;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

/**
 * AI 기반 수면 상담 서비스 인터페이스
 * 
 * Spring Boot 베스트 프랙티스를 따라 명확한 책임 분리와 
 * 확장 가능한 구조로 설계된 상담 서비스입니다.
 */
public interface ConsultationService {

    /**
     * 새로운 상담 세션을 시작합니다.
     * 
     * @param user 인증된 사용자
     * @param request 상담 시작 요청
     * @return 생성된 상담 세션 정보
     * @throws IllegalArgumentException 잘못된 요청 데이터
     * @throws BusinessException 비즈니스 로직 오류
     */
    ConsultationSessionResponseDto startSession(User user, StartConsultationRequestDto request);

    /**
     * 상담 세션에 메시지를 전송합니다.
     *
     * @param user 인증된 사용자
     * @param sessionId 세션 ID
     * @param request 메시지 전송 요청
     * @return 메시지 응답 (AI 응답 포함)
     * @throws IllegalArgumentException 잘못된 요청 데이터
     * @throws BusinessException 세션을 찾을 수 없거나 권한 없음
     */
    MessageResponseDto sendMessage(User user, Long sessionId, SendMessageRequestDto request);

    /**
     * 상담 세션에 메시지를 전송하고 AI 응답을 스트리밍으로 받습니다 (SSE).
     *
     * ✅ 세션 격리: conversationId로 사용자별 대화 완전 분리
     * ✅ 동일 모델: OpenAI GPT-4o (consultationChatClient)
     *
     * @param user 인증된 사용자
     * @param sessionId 세션 ID
     * @param request 메시지 전송 요청
     * @return AI 응답 스트림 (Flux)
     * @throws BusinessException 세션을 찾을 수 없거나 권한 없음
     */
    Flux<String> sendMessageStream(User user, Long sessionId, SendMessageRequestDto request);

    /**
     * 사용자의 상담 세션 목록을 조회합니다.
     * 
     * @param user 인증된 사용자
     * @param pageable 페이징 정보
     * @return 상담 세션 목록
     */
    Page<ConsultationSessionResponseDto> getUserSessions(User user, Pageable pageable);

    /**
     * 상담 세션의 상세 정보를 조회합니다.
     * 
     * @param user 인증된 사용자
     * @param sessionId 세션 ID
     * @return 세션 상세 정보
     * @throws BusinessException 세션을 찾을 수 없거나 권한 없음
     */
    ConsultationSessionDetailDto getSessionDetail(User user, Long sessionId);

    /**
     * 상담 세션을 종료합니다.
     * 
     * @param user 인증된 사용자
     * @param sessionId 세션 ID
     * @param request 세션 종료 요청
     * @return 세션 종료 응답
     * @throws BusinessException 세션을 찾을 수 없거나 권한 없음
     */
    SessionEndResponseDto endSession(User user, Long sessionId, EndSessionRequestDto request);

    /**
     * 상담 세션의 요약을 조회합니다.
     * 
     * @param user 인증된 사용자
     * @param sessionId 세션 ID
     * @return 상담 요약
     * @throws BusinessException 세션을 찾을 수 없거나 권한 없음
     */
    ConsultationSummaryDto getSessionSummary(User user, Long sessionId);

    /**
     * 상담에 대한 피드백을 제출합니다.
     * 
     * @param user 인증된 사용자
     * @param sessionId 세션 ID
     * @param request 피드백 요청
     * @throws BusinessException 세션을 찾을 수 없거나 권한 없음
     */
    void submitFeedback(User user, Long sessionId, FeedbackRequestDto request);

    /**
     * 사용 가능한 AI 모델 정보를 조회합니다.
     * 
     * @return AI 모델 정보
     */
    AvailableAiModelsDto getAvailableModels();

    /**
     * 시스템에 의해 자동으로 상담 세션을 생성합니다.
     * (수면 분석 결과 기반 자동 트리거)
     * 
     * @param user 사용자
     * @param sleepAnalysisData 분석 데이터
     * @return 생성된 상담 세션 정보
     */
    ConsultationSessionResponseDto createAutomaticSession(User user, String sleepAnalysisData);
} 