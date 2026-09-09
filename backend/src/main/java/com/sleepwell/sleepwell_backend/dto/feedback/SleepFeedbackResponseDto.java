package com.sleepwell.sleepwell_backend.dto.feedback;

import com.sleepwell.sleepwell_backend.entity.SleepFeedback;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class SleepFeedbackResponseDto {

    private Long id;
    private Long sleepRecordId;
    private LocalDate feedbackDate;
    private Integer overallSatisfaction;
    private Integer fatigueRecovery;
    private Integer morningFreshness;
    private Integer morningCondition;
    private Integer perceivedSleepDepth;
    private Integer perceivedWakeupCount;
    private String sleepNotes;
    private boolean isGoodSleep;

    public static SleepFeedbackResponseDto from(SleepFeedback entity) {
        return new SleepFeedbackResponseDto(
                entity.getId(),
                entity.getSleepRecord().getId(),
                entity.getFeedbackDate(),
                entity.getOverallSatisfaction(),
                entity.getFatigueRecovery(),
                entity.getMorningFreshness(),
                entity.getMorningCondition(),
                entity.getPerceivedSleepDepth(),
                entity.getPerceivedWakeupCount(),
                entity.getSleepNotes(),
                entity.isGoodSleep()
        );
    }
} 