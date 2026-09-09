package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.MessageStatus;
import com.sleepwell.sleepwell_backend.enums.MessageType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대화 메시지 엔티티
 * 음성 상담 세션 중의 개별 메시지를 관리합니다.
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "conversation_messages", indexes = {
    // 핵심 세션별 메시지 조회 (실시간 채팅)
    @Index(name = "IDX_CONVERSATION_MESSAGE_SESSION_TIME", columnList = "consultation_session_id, createdAt"),
    @Index(name = "IDX_CONVERSATION_MESSAGE_SESSION_TYPE", columnList = "consultation_session_id, message_type, createdAt"),
    
    // 실시간 메시지 처리 큐 최적화
    @Index(name = "IDX_CONVERSATION_MESSAGE_STATUS_TIME", columnList = "status, createdAt"),
    @Index(name = "IDX_CONVERSATION_MESSAGE_PENDING", columnList = "status, createdAt") 
        // PENDING 메시지 우선순위 큐용
        ,
    @Index(name = "IDX_CONVERSATION_MESSAGE_PROCESSING", columnList = "status, processedAt"),
    
    // 사용자별 메시지 이력
    @Index(name = "IDX_CONVERSATION_MESSAGE_USER_TIME", columnList = "user_id, createdAt"),
    
    // STT/TTS 처리 결과 분석
    @Index(name = "IDX_CONVERSATION_MESSAGE_STT_CONFIDENCE", columnList = "stt_confidence_score, createdAt"),
    @Index(name = "IDX_CONVERSATION_MESSAGE_VOICE_FILES", columnList = "voice_file_path, status"),
    
    // 감정 분석 및 의도 인식 조회
    @Index(name = "IDX_CONVERSATION_MESSAGE_INTENT", columnList = "intent_recognition, createdAt"),
    // JSON 컬럼은 MySQL에서 직접 인덱싱 불가능하므로 제거
    // @Index(name = "IDX_CONVERSATION_MESSAGE_SENTIMENT", columnList = "sentiment_analysis, createdAt"),
    
    // 성능 모니터링 및 분석
    @Index(name = "IDX_CONVERSATION_MESSAGE_RESPONSE_TIME", columnList = "response_time_ms, message_type"),
    @Index(name = "IDX_CONVERSATION_MESSAGE_AI_RESPONSE", columnList = "message_type, response_time_ms"),
    
    // 실패 메시지 재처리 - TEXT 컬럼 error_message는 인덱스에서 제외
    @Index(name = "IDX_CONVERSATION_MESSAGE_FAILED", columnList = "status, createdAt"),
    
    // 일별 통계 최적화
    @Index(name = "IDX_CONVERSATION_MESSAGE_DAILY_STATS", columnList = "createdAt, status, message_type")
})
public class ConversationMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 상담 세션
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_session_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_CONVERSATION_MESSAGE_SESSION"))
    private ConsultationSession consultationSession;

    /**
     * 메시지 발신자 (사용자인 경우)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", 
                foreignKey = @ForeignKey(name = "FK_CONVERSATION_MESSAGE_USER"))
    private User user;

    /**
     * 메시지 타입
     * USER_VOICE, USER_TEXT, AI_RESPONSE, SYSTEM_MESSAGE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    /**
     * 텍스트 콘텐츠 (STT 결과 또는 AI 응답)
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 음성 파일 S3 경로 (사용자 음성 또는 TTS 결과)
     */
    @Column(name = "voice_file_path", length = 500)
    private String voiceFilePath;

    /**
     * STT(Speech-to-Text) 처리 결과
     */
    @Column(name = "stt_result", columnDefinition = "TEXT")
    private String sttResult;

    /**
     * STT 신뢰도 점수 (0.0 ~ 1.0)
     */
    @Column(name = "stt_confidence_score")
    private Double sttConfidenceScore;

    /**
     * AI 응답 내용 (원문)
     */
    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    /**
     * 사용된 AI 모델명
     */
    @Column(name = "ai_model", length = 50)
    private String aiModel;

    /**
     * AI 응답의 TTS 파일 경로
     */
    @Column(name = "ai_voice_file_path", length = 500)
    private String aiVoiceFilePath;

    /**
     * 메시지 처리 상태
     * PENDING, PROCESSING, COMPLETED, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status;

    /**
     * 처리 오류 메시지
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 메시지 발송 시간
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * 메시지 처리 완료 시간
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * 응답 생성에 걸린 시간 (밀리초)
     */
    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    /**
     * 감정 분석 결과 (JSON)
     * 기쁨, 슬픔, 분노, 불안 등의 감정 점수
     */
    @Column(name = "sentiment_analysis", columnDefinition = "JSON")
    private String sentimentAnalysis;

    /**
     * 의도 인식 결과
     * 사용자의 질문 의도나 요청 분류
     */
    @Column(name = "intent_recognition", length = 100)
    private String intentRecognition;

    /**
     * 메타데이터 (JSON)
     * 기술적 정보, 음성 품질 등
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /**
     * 이 메시지와 관련된 음성 처리 작업들
     */
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<VoiceProcessingJob> voiceProcessingJobs;
} 