package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.WearableSource;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 분석 작업 스케줄링 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleAnalysisRequestDto {
    
    @NotNull(message = "분석 타입은 필수입니다")
    private String analysisType;
    
    @NotNull(message = "우선순위는 필수입니다")
    private String priority;
    
    @NotNull(message = "대상 날짜는 필수입니다")
    private LocalDate targetDate;
    
    @NotNull(message = "플랫폼 소스는 필수입니다")
    private WearableSource platformSource;
}