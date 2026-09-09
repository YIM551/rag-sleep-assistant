package com.sleepwell.sleepwell_backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminConsultationDto {
    private Long sessionId;
    private Long userId;
    private String userEmail;
    private String userName;
    
    private String topic;
    private String consultationType;
    private String aiModel;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer messageCount;
    
    private String initialQuestion;
    private String lastMessage;
    
    // AI 사용 통계
    private Integer totalTokens;
    private Double estimatedCost;
    
    private String status;
    private Double satisfactionScore;
}