package com.sleepwell.sleepwell_backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminConsultationDetailDto {
    private Long sessionId;
    
    // 사용자 정보
    private Long userId;
    private String userEmail;
    private String userName;
    private String userRole;
    
    // 세션 정보
    private String topic;
    private String consultationType;
    private String aiModel;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    
    // 전체 대화 내용
    private List<ChatMessage> messages;
    
    // AI 사용 통계
    private Integer totalTokens;
    private Integer promptTokens;
    private Integer completionTokens;
    private Double estimatedCost;
    
    // 분석 정보
    private List<String> keyTopics;
    private String sentiment;
    private Double satisfactionScore;
    private String consultationSummary;
    
    // 관련 수면 데이터
    private SleepContext sleepContext;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private Long id;
        private String role; // USER, ASSISTANT, SYSTEM
        private String content;
        private LocalDateTime timestamp;
        private Integer tokenCount;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SleepContext {
        private Double recentAverageSleepQuality;
        private Double recentAverageSleepDuration;
        private String primarySleepIssue;
        private LocalDateTime lastSleepRecordDate;
    }
}