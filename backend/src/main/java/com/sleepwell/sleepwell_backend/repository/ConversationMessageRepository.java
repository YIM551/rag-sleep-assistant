package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.ConversationMessage;
import com.sleepwell.sleepwell_backend.entity.ConsultationSession;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.MessageStatus;
import com.sleepwell.sleepwell_backend.enums.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 대화 메시지 Repository
 * 실시간 메시지 처리 및 WebSocket 지원을 위한 최적화된 쿼리 제공
 */
@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    /**
     * 세션별 메시지 조회 (시간순) - Spring Data 네이밍 규칙
     */
    List<ConversationMessage> findByConsultationSessionOrderBySentAtAsc(ConsultationSession consultationSession);

    /**
     * 세션별 메시지 개수 조회 - Spring Data 네이밍 규칙
     */
    int countByConsultationSession(ConsultationSession consultationSession);

    /**
     * 세션별 메시지 조회 (페이징)
     */
    Page<ConversationMessage> findByConsultationSessionOrderBySentAtAsc(
            ConsultationSession session, Pageable pageable);

    /**
     * 특정 메시지 타입별 조회
     */
    List<ConversationMessage> findByConsultationSessionAndMessageTypeOrderBySentAtAsc(
            ConsultationSession session, MessageType messageType);

    /**
     * 특정 상태의 메시지 조회
     */
    List<ConversationMessage> findByConsultationSessionAndStatusOrderBySentAtAsc(
            ConsultationSession session, MessageStatus status);

    /**
     * 최근 메시지 조회
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.consultationSession = :session " +
           "ORDER BY cm.sentAt DESC")
    List<ConversationMessage> findRecentBySession(@Param("session") ConsultationSession session, Pageable pageable);

    /**
     * 사용자 메시지만 조회
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.consultationSession = :session " +
           "AND cm.messageType = 'USER_TEXT' ORDER BY cm.sentAt ASC")
    List<ConversationMessage> findUserMessagesBySession(@Param("session") ConsultationSession session);

    /**
     * AI 응답 메시지만 조회
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.consultationSession = :session " +
           "AND cm.messageType = 'AI_RESPONSE' ORDER BY cm.sentAt ASC")
    List<ConversationMessage> findAiResponsesBySession(@Param("session") ConsultationSession session);

    /**
     * 특정 기간 내 메시지 조회
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.consultationSession = :session " +
           "AND cm.sentAt BETWEEN :startTime AND :endTime ORDER BY cm.sentAt ASC")
    List<ConversationMessage> findBySessionAndTimeRange(
            @Param("session") ConsultationSession session,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 세션의 첫 번째 메시지 조회
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.consultationSession = :session " +
           "ORDER BY cm.sentAt ASC")
    List<ConversationMessage> findFirstBySession(@Param("session") ConsultationSession session, Pageable pageable);

    /**
     * 세션의 마지막 메시지 조회
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.consultationSession = :session " +
           "ORDER BY cm.sentAt DESC")
    List<ConversationMessage> findLastBySession(@Param("session") ConsultationSession session, Pageable pageable);

    /**
     * 에러 상태 메시지 조회
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.consultationSession = :session " +
           "AND cm.status = 'FAILED' ORDER BY cm.sentAt DESC")
    List<ConversationMessage> findErrorMessagesBySession(@Param("session") ConsultationSession session);

    /**
     * 메시지 통계 조회
     */
    @Query("SELECT cm.messageType, COUNT(cm) FROM ConversationMessage cm " +
           "WHERE cm.consultationSession = :session GROUP BY cm.messageType")
    List<Object[]> getMessageStatisticsBySession(@Param("session") ConsultationSession session);

    /**
     * 사용자의 모든 메시지 조회
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.user = :user ORDER BY cm.sentAt DESC")
    Page<ConversationMessage> findByUser(@Param("user") User user, Pageable pageable);

    /**
     * 메시지 타입별 조회
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.messageType = :messageType " +
           "ORDER BY cm.sentAt DESC")
    Page<ConversationMessage> findByMessageType(@Param("messageType") MessageType messageType, Pageable pageable);

    /**
     * 처리 상태별 메시지 조회
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.status = :status ORDER BY cm.sentAt ASC")
    List<ConversationMessage> findByStatus(@Param("status") MessageStatus status);

    /**
     * 처리 대기 중인 메시지 조회 (우선순위 큐용)
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.status = 'PENDING' " +
           "ORDER BY cm.sentAt ASC")
    List<ConversationMessage> findPendingMessages();

    /**
     * 처리 중인 메시지 조회 (모니터링용)
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.status = 'PROCESSING' " +
           "ORDER BY cm.sentAt ASC")
    List<ConversationMessage> findProcessingMessages();

    /**
     * 실패한 메시지 조회 (재처리 대상)
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.status = 'FAILED' " +
           "ORDER BY cm.sentAt DESC")
    List<ConversationMessage> findFailedMessages();

    /**
     * 세션의 최신 메시지 조회
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.consultationSession = :session " +
           "ORDER BY cm.sentAt DESC")
    Optional<ConversationMessage> findTopByConsultationSessionOrderBySentAtDesc(@Param("session") ConsultationSession session);

    /**
     * 메시지 상태 일괄 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationMessage cm SET cm.status = :newStatus WHERE cm.status = :oldStatus")
    int updateStatusBatch(@Param("oldStatus") MessageStatus oldStatus, @Param("newStatus") MessageStatus newStatus);

    /**
     * 처리 시간 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationMessage cm SET cm.processedAt = :processedAt, cm.responseTimeMs = :responseTime " +
           "WHERE cm.id = :messageId")
    int updateProcessingTime(@Param("messageId") Long messageId, 
                           @Param("processedAt") LocalDateTime processedAt,
                           @Param("responseTime") Long responseTime);

    /**
     * 평균 응답 시간 계산
     */
    @Query("SELECT AVG(cm.responseTimeMs) FROM ConversationMessage cm " +
           "WHERE cm.responseTimeMs IS NOT NULL AND cm.messageType = 'AI_RESPONSE'")
    Double getAverageResponseTime();

    /**
     * 평균 STT 신뢰도 점수
     */
    @Query("SELECT AVG(cm.sttConfidenceScore) FROM ConversationMessage cm " +
           "WHERE cm.sttConfidenceScore IS NOT NULL")
    Double getAverageSTTConfidence();

    /**
     * 특정 기간 내 메시지 조회
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.sentAt BETWEEN :startDate AND :endDate " +
           "ORDER BY cm.sentAt DESC")
    List<ConversationMessage> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * 메시지 타입별 통계
     */
    @Query("SELECT cm.messageType, COUNT(cm) FROM ConversationMessage cm " +
           "GROUP BY cm.messageType ORDER BY COUNT(cm) DESC")
    List<Object[]> getMessageTypeStatistics();

    /**
     * 처리 상태별 통계
     */
    @Query("SELECT cm.status, COUNT(cm) FROM ConversationMessage cm " +
           "GROUP BY cm.status ORDER BY COUNT(cm) DESC")
    List<Object[]> getStatusStatistics();

    List<ConversationMessage> findByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ConversationMessage cm WHERE cm.user = :user")
    void deleteAllByUser(@Param("user") User user);
    
    // Admin 기능을 위한 추가 메서드
    @Query("SELECT COUNT(cm) FROM ConversationMessage cm WHERE cm.createdAt BETWEEN :startDate AND :endDate")
    Long countByCreatedAtBetween(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);
    
    /**
     * 사용자별 메시지 개수 조회
     * AdminServiceImpl에서 사용
     */
    @Query("SELECT COUNT(cm) FROM ConversationMessage cm WHERE cm.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    /**
     * 세션 ID로 메시지 개수 조회
     */
    @Query("SELECT COUNT(cm) FROM ConversationMessage cm WHERE cm.consultationSession.id = :sessionId")
    int countByConsultationSessionId(@Param("sessionId") Long sessionId);

    /**
     * 세션 ID로 메시지 조회 (시간 역순)
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.consultationSession.id = :sessionId ORDER BY cm.createdAt DESC")
    List<ConversationMessage> findByConsultationSessionIdOrderByCreatedAtDesc(@Param("sessionId") Long sessionId);

    /**
     * 세션 ID로 메시지 조회 (시간 순)
     */
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.consultationSession.id = :sessionId ORDER BY cm.createdAt ASC")
    List<ConversationMessage> findByConsultationSessionIdOrderByCreatedAtAsc(@Param("sessionId") Long sessionId);
} 