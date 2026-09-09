package com.sleepwell.sleepwell_backend.dto.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CreateSleepFeedbackRequestDto {

    @NotNull
    private Long sleepRecordId;

    @NotNull
    private LocalDate feedbackDate;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer overallSatisfaction;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer fatigueRecovery;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer morningFreshness;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer morningCondition;

    private Integer perceivedSleepDepth;
    private Integer perceivedWakeupCount;
    private String sleepNotes;
} 