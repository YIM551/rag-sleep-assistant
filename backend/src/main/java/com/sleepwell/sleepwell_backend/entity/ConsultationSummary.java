package com.sleepwell.sleepwell_backend.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상담 요약 엔티티
 * 음성 상담 세션 종료 후 AI가 생성한 요약과 권장사항을 관리합니다.
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "consultation_summaries", indexes = {
    // 핵심 관계 조회
    @Index(name = "IDX_CONSULTATION_SUMMARY_SESSION", columnList = "consultation_session_id"),
    @Index(name = "IDX_CONSULTATION_SUMMARY_USER_TIME", columnList = "user_id, createdAt"),
    
    // 요약 품질 및 평가 분석
    @Index(name = "IDX_CONSULTATION_SUMMARY_RATING", columnList = "summary_rating, createdAt"),
    @Index(name = "IDX_CONSULTATION_SUMMARY_HIGH_RATING", columnList = "summary_rating, generatedAt"),
    
    // 위험도 평가 및 의료진 의뢰 시스템
    @Index(name = "IDX_CONSULTATION_SUMMARY_RISK", columnList = "risk_assessment, createdAt"),
    @Index(name = "IDX_CONSULTATION_SUMMARY_HIGH_RISK", columnList = "risk_assessment, medical_referral, createdAt"),
    @Index(name = "IDX_CONSULTATION_SUMMARY_MEDICAL_REFERRAL", columnList = "medical_referral, risk_assessment"),
    
    // 상담 효과성 및 참여도 분석
    @Index(name = "IDX_CONSULTATION_SUMMARY_EFFECTIVENESS", columnList = "effectiveness_score, createdAt"),
    @Index(name = "IDX_CONSULTATION_SUMMARY_ENGAGEMENT", columnList = "engagement_score, createdAt"),
    @Index(name = "IDX_CONSULTATION_SUMMARY_PERFORMANCE", columnList = "effectiveness_score, engagement_score"),
    
    // AI 모델 성능 분석
    @Index(name = "IDX_CONSULTATION_SUMMARY_AI_MODEL", columnList = "ai_model, confidence_score, generatedAt"),
    @Index(name = "IDX_CONSULTATION_SUMMARY_CONFIDENCE", columnList = "confidence_score, ai_model"),
    
    // 후속 조치 관리
    @Index(name = "IDX_CONSULTATION_SUMMARY_FOLLOW_UP", columnList = "next_consultation_recommended, risk_assessment"),
    
    // 통계 및 리포팅 최적화
    @Index(name = "IDX_CONSULTATION_SUMMARY_MONTHLY_STATS", columnList = "generatedAt, summary_rating, effectiveness_score"),
    @Index(name = "IDX_CONSULTATION_SUMMARY_DAILY_STATS", columnList = "createdAt, risk_assessment, medical_referral"),
    
    // 사용자별 요약 트렌드 분석
    @Index(name = "IDX_CONSULTATION_SUMMARY_USER_TREND", columnList = "user_id, generatedAt, effectiveness_score")
})
public class ConsultationSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연관된 상담 세션 (1:1 관계)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_session_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "FK_CONSULTATION_SUMMARY_SESSION"))
    private ConsultationSession consultationSession;

    /**
     * 상담을 받은 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_CONSULTATION_SUMMARY_USER"))
    private User user;

    /**
     * AI가 생성한 상담 요약 텍스트
     */
    @Column(name = "summary_text", columnDefinition = "TEXT", nullable = false)
    private String summaryText;

    /**
     * 주요 요약 내용 (별칭)
     */
    @Column(name = "main_summary", columnDefinition = "TEXT")
    private String mainSummary;

    /**
     * 주요 논의 사항들 (JSON 배열)
     */
    @Column(name = "key_topics", columnDefinition = "JSON")
    private String keyTopics;

    /**
     * AI 권장사항들 (JSON 배열)
     */
    @Column(name = "recommendations", columnDefinition = "JSON")
    private String recommendations;

    /**
     * 식별된 수면 문제들 (JSON 배열)
     */
    @Column(name = "identified_issues", columnDefinition = "JSON")
    private String identifiedIssues;

    /**
     * 제안된 해결책들 (JSON 배열)
     */
    @Column(name = "suggested_solutions", columnDefinition = "JSON")
    private String suggestedSolutions;

    /**
     * 후속 조치 계획 (JSON)
     */
    @Column(name = "follow_up_plan", columnDefinition = "JSON")
    private String followUpPlan;

    /**
     * 다음 상담 권장 일정
     */
    @Column(name = "next_consultation_recommended")
    private LocalDateTime nextConsultationRecommended;

    /**
     * 위험도 평가 (1=낮음, 5=높음)
     */
    @Column(name = "risk_assessment")
    private Integer riskAssessment;

    /**
     * 요약의 신뢰도 점수 (0.0 ~ 1.0)
     */
    @Column(name = "confidence_score")
    private Double confidenceScore;

    /**
     * 감정 분석 종합 결과 (JSON)
     */
    @Column(name = "emotion_summary", columnDefinition = "JSON")
    private String emotionSummary;

    /**
     * 상담 효과성 점수 (1-100)
     */
    @Column(name = "effectiveness_score")
    private Integer effectivenessScore;

    /**
     * 사용자 참여도 점수 (1-100)
     */
    @Column(name = "engagement_score")
    private Integer engagementScore;

    /**
     * 요약 생성에 사용된 AI 모델
     */
    @Column(name = "ai_model", length = 100)
    private String aiModel;

    /**
     * 요약 생성 시간
     */
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    /**
     * 요약 품질 평가 (1-5)
     */
    @Column(name = "summary_rating")
    private Integer summaryRating;

    /**
     * 관련 진료과 추천
     */
    @Column(name = "medical_referral", length = 200)
    private String medicalReferral;

    /**
     * 추가 메모
     */
    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    /**
     * 요약 메타데이터 (JSON)
     * 생성 조건, 사용된 파라미터 등
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
} 