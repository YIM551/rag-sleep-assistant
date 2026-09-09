package com.sleepwell.sleepwell_backend.dto.analysis;

import com.sleepwell.sleepwell_backend.entity.AnalysisExecutionJob;
import com.sleepwell.sleepwell_backend.enums.JobStatus;
import com.sleepwell.sleepwell_backend.enums.JobPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 분석 작업 응답 DTO
 * 순환 참조 방지를 위해 엔티티 대신 DTO 사용 (김영한님 권장 방식)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisExecutionJobResponseDto {
    
    private Long id;
    private String jobId;
    
    // User 엔티티 대신 필요한 정보만 포함
    private Long userId;
    private String userEmail;
    private String userName;
    
    // SleepRecord 엔티티 대신 ID만 포함
    private Long sleepRecordId;
    private LocalDateTime targetDate;
    
    private String analysisType;
    private JobStatus status;
    private JobPriority priority;
    
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    private String resultSummary;
    private String errorMessage;
    private Integer retryCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static AnalysisExecutionJobResponseDto from(AnalysisExecutionJob job) {
        return AnalysisExecutionJobResponseDto.builder()
                .id(job.getId())
                .jobId(job.getJobId())
                .userId(job.getUser() != null ? job.getUser().getId() : null)
                .userEmail(job.getUser() != null ? job.getUser().getEmail() : null)
                .userName(job.getUser() != null ? job.getUser().getName() : null)
                .sleepRecordId(job.getSleepRecord() != null ? job.getSleepRecord().getId() : null)
                .targetDate(job.getTargetDate())
                .analysisType(job.getAnalysisType())
                .status(job.getStatus())
                .priority(job.getPriority())
                .scheduledAt(job.getScheduledAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .resultSummary(job.getResultSummary())
                .errorMessage(job.getErrorMessage())
                .retryCount(job.getRetryCount())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}