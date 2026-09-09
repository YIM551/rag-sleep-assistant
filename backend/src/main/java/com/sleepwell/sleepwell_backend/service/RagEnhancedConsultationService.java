package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.*;
import com.sleepwell.sleepwell_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

/**
 * RAG 강화 상담 서비스
 *
 * 수면 의학 논문 검색(RAG)과 개인 수면 데이터를 결합하여
 * 근거 기반의 개인화된 상담을 제공합니다.
 */
public interface RagEnhancedConsultationService {

    /**
     * RAG 강화 상담 응답 생성 (동기) - 레거시 API, 호환성 유지용
     *
     * @param user 인증된 사용자
     * @param request 상담 요청
     * @return 논문 근거 + 개인 데이터 기반 응답
     * @deprecated 세션 기반 API 사용 권장 (startRagSession + sendRagMessage)
     */
    @Deprecated
    RagEnhancedConsultationResponseDto generateConsultation(User user, RagEnhancedConsultationRequestDto request);

    /**
     * RAG 강화 상담 응답 스트리밍 (SSE) - 레거시 API
     *
     * @param user 인증된 사용자
     * @param request 상담 요청
     * @return AI 응답 스트림
     * @deprecated 세션 기반 API 사용 권장
     */
    @Deprecated
    Flux<String> generateConsultationStream(User user, RagEnhancedConsultationRequestDto request);

    /**
     * RAG 상담 세션 시작 (신규 API)
     *
     * @param user 인증된 사용자
     * @param request RAG 세션 설정
     * @return 생성된 세션 정보
     */
    RagSessionResponseDto startRagSession(User user, StartRagSessionRequestDto request);

    /**
     * RAG 상담 메시지 전송 (연속 대화 지원)
     *
     * @param user 인증된 사용자
     * @param sessionId 세션 ID
     * @param request 질문 내용
     * @return RAG 기반 답변 (논문 인용 + 개인 데이터)
     */
    RagEnhancedConsultationResponseDto sendRagMessage(User user, Long sessionId, SendRagMessageRequestDto request);

    /**
     * RAG 상담 세션 종료
     *
     * @param user 인증된 사용자
     * @param sessionId 세션 ID
     * @return 세션 종료 응답
     */
    SessionEndResponseDto endRagSession(User user, Long sessionId);

    /**
     * RAG 상담 세션 목록 조회
     *
     * @param user 인증된 사용자
     * @param pageable 페이징 정보
     * @return 세션 목록
     */
    Page<RagSessionResponseDto> getRagSessions(User user, Pageable pageable);

    /**
     * RAG 상담 세션 상세 정보 조회 (메시지 포함)
     *
     * @param user 인증된 사용자
     * @param sessionId 세션 ID
     * @return 세션 상세 + 대화 메시지
     */
    ConsultationSessionDetailDto getRagSessionDetail(User user, Long sessionId);
}
