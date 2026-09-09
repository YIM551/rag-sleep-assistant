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
 * 분석 작업 목록용 간소화된 DTO
 * 목록 조회 시 필요한 최소한의 정보만 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisExecutionJobListDto {
    
    private Long id;
    private String jobId;
    private String analysisType;
    private JobStatus status;
    private JobPriority priority;
    private LocalDateTime targetDate;
    private LocalDateTime scheduledAt;
    private LocalDateTime completedAt;
    
    public static AnalysisExecutionJobListDto from(AnalysisExecutionJob job) {
        return AnalysisExecutionJobListDto.builder()
                .id(job.getId())
                .jobId(job.getJobId())
                .analysisType(job.getAnalysisType())
                .status(job.getStatus())
                .priority(job.getPriority())
                .targetDate(job.getTargetDate())
                .scheduledAt(job.getScheduledAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}