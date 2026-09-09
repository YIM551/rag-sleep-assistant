package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.ConsultationTopic;
import com.sleepwell.sleepwell_backend.enums.SessionStatus;
import com.sleepwell.sleepwell_backend.enums.SessionType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 음성 상담 세션 엔티티
 * LLM 기반 음성 상담의 세션 정보를 관리합니다.
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "consultation_sessions", indexes = {
    // 핵심 비즈니스 쿼리 최적화
    @Index(name = "IDX_CONSULTATION_SESSION_USER_STATUS", columnList = "user_id, status"),
    @Index(name = "IDX_CONSULTATION_SESSION_USER_DATE", columnList = "user_id, consultationTime"),
    
    // 실시간 모니터링 및 WebSocket 지원
    @Index(name = "IDX_CONSULTATION_SESSION_STATUS_TIME", columnList = "status, consultationTime"),
    @Index(name = "IDX_CONSULTATION_SESSION_IN_PROGRESS", columnList = "status, consultationTime"),
    
    // 상담 주제 및 유형별 분석
    @Index(name = "IDX_CONSULTATION_SESSION_TOPIC_STATUS", columnList = "topic, status"),
    @Index(name = "IDX_CONSULTATION_SESSION_TYPE_TIME", columnList = "session_type, consultationTime"),
    
    // 응급 상담 우선 처리
    @Index(name = "IDX_CONSULTATION_SESSION_EMERGENCY", columnList = "session_type, status, consultationTime"),
    
    // 만족도 및 품질 분석
    @Index(name = "IDX_CONSULTATION_SESSION_SATISFACTION", columnList = "user_satisfaction_score, endTime"),
    @Index(name = "IDX_CONSULTATION_SESSION_AI_QUALITY", columnList = "ai_quality_score, endTime"),
    
    // 통계 및 리포팅 최적화
    @Index(name = "IDX_CONSULTATION_SESSION_COMPLETED_TIME", columnList = "status, endTime"),
    @Index(name = "IDX_CONSULTATION_SESSION_DURATION", columnList = "total_duration_minutes, status"),
    
    // 월별/일별 통계 최적화 (FUNCTION 사용 쿼리 지원)
    @Index(name = "IDX_CONSULTATION_SESSION_DATE_STATS", columnList = "consultationTime, status")
})
public class ConsultationSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 상담을 요청한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, 
                foreignKey = @ForeignKey(name = "FK_CONSULTATION_SESSION_USER"))
    private User user;

    /**
     * 상담 시작 시간
     */
    @Column(name = "consultation_time", nullable = false)
    private LocalDateTime consultationTime;

    /**
     * 상담 종료 시간
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 상담 세션 상태
     * SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    /**
     * 상담 주제/분야
     * INSOMNIA, SLEEP_APNEA, SNORING, SLEEP_SCHEDULE 등
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "topic", nullable = false, length = 50)
    private ConsultationTopic topic;

    /**
     * 세션 유형
     * SLEEP_ANALYSIS, SLEEP_IMPROVEMENT, GENERAL_CONSULTATION, EMERGENCY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 30)
    private SessionType sessionType;

    /**
     * 상담 제목
     */
    @Column(name = "title", length = 200)
    private String title;

    /**
     * 사용자가 제공한 초기 설명/질문
     */
    @Column(name = "initial_description", columnDefinition = "TEXT")
    private String initialDescription;

    /**
     * 음성 상담 활성화 여부
     */
    @Column(name = "voice_enabled", nullable = false)
    @Builder.Default
    private Boolean voiceEnabled = false;

    /**
     * 사용된 AI 모델
     * GPT-4, Claude-3, 등 AI 모델 식별자
     */
    @Column(name = "ai_model", length = 50)
    private String aiModel;

    /**
     * 예상 상담 시간 (분)
     */
    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    /**
     * 총 대화 시간 (분)
     */
    @Column(name = "total_duration_minutes")
    private Integer totalDurationMinutes;

    /**
     * AI 모델 응답 품질 평가 점수 (1-100)
     */
    @Column(name = "ai_quality_score")
    private Integer aiQualityScore;

    /**
     * 사용자 만족도 점수 (1-5)
     */
    @Column(name = "user_satisfaction_score")
    private Integer userSatisfactionScore;

    /**
     * 사용자 피드백
     */
    @Column(name = "user_feedback", columnDefinition = "TEXT")
    private String userFeedback;

    /**
     * 세션 메타데이터 (JSON 형태)
     * 기술적 세부정보, 연결 품질 등
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /**
     * 이 세션의 대화 메시지들
     */
    @OneToMany(mappedBy = "consultationSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ConversationMessage> conversationMessages;

    /**
     * 이 세션의 요약 정보 (1:1 관계)
     */
    @OneToOne(mappedBy = "consultationSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private ConsultationSummary consultationSummary;

    // === 비즈니스 메서드 ===

    /**
     * 세션 상태 업데이트
     */
    public void updateStatus(SessionStatus newStatus) {
        this.status = newStatus;
        // updatedAt은 JPA Auditing에 의해 자동 설정됨
    }

    /**
     * 세션 종료 처리
     */
    public void endSession() {
        this.status = SessionStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        
        // 총 대화 시간 계산
        if (this.consultationTime != null) {
            long durationMinutes = java.time.Duration.between(
                this.consultationTime, this.endTime).toMinutes();
            this.totalDurationMinutes = (int) durationMinutes;
        }
    }

    /**
     * 사용자 피드백 및 만족도 설정
     */
    public void setUserFeedback(String feedback, Integer satisfactionScore) {
        this.userFeedback = feedback;
        this.userSatisfactionScore = satisfactionScore;
    }

    /**
     * AI 품질 점수 설정
     */
    public void setAiQualityScore(Integer qualityScore) {
        this.aiQualityScore = qualityScore;
    }

    /**
     * 세션 진행 중 업데이트 (메시지 전송 시)
     * JPA dirty checking을 통해 자동으로 updatedAt이 갱신됨
     */
    public void updateProgress() {
        // 빈 메서드 - JPA가 자동으로 updatedAt 갱신
    }

    /**
     * 세션 취소
     */
    public void cancelSession() {
        this.status = SessionStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
    }

    /**
     * 세션이 활성 상태인지 확인
     */
    public boolean isActive() {
        return this.status == SessionStatus.IN_PROGRESS || 
               this.status == SessionStatus.SCHEDULED;
    }

    /**
     * 세션이 완료된 상태인지 확인
     */
    public boolean isCompleted() {
        return this.status == SessionStatus.COMPLETED;
    }

    /**
     * 메시지 개수 조회 (연관 엔티티 접근)
     */
    public int getMessageCount() {
        return this.conversationMessages != null ? this.conversationMessages.size() : 0;
    }

    /**
     * 세션 시작 처리
     */
    public void startSession() {
        this.status = SessionStatus.IN_PROGRESS;
        if (this.consultationTime == null) {
            this.consultationTime = LocalDateTime.now();
        }
    }

    /**
     * 메타데이터 업데이트
     */
    public void updateMetadata(String metadata) {
        this.metadata = metadata;
    }
} 