package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.entity.SISResponse;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SIS 설문 응답 DTO
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SISResponseDto {

    private Long id;
    private Long userId;

    // Domain 1: Daily Activities (일상 활동)
    private Integer q1a;
    private Integer q1b;
    private Integer q1c;
    private Integer q1d;
    private Integer q1e;
    private Integer q1f;
    private Integer q1g;
    private Integer q1h;

    // Domain 2: Daytime Emotional Impact (주간 정서 영향)
    private Integer q2a;
    private Integer q2b;
    private Integer q2c;
    private Integer q2d;

    // Domain 3: Nighttime Emotional Impact (야간 정서 영향)
    private Integer q3a;
    private Integer q3b;
    private Integer q3c;
    private Integer q3d;
    private Integer q3e;

    // Domain 4: Fatigue (피로)
    private Integer q4a;
    private Integer q4b;
    private Integer q4c;
    private Integer q4d;
    private Integer q4e;
    private Integer q4f;
    private Integer q4g;

    // Domain 5: Social Impact (사회적 영향)
    private Integer q5a;
    private Integer q5b;
    private Integer q5c;
    private Integer q5d;

    // Domain 6: Mental Fatigue (정신적 피로)
    private Integer q6a;
    private Integer q6b;
    private Integer q6c;
    private Integer q6d;

    // Domain 7: Sleep Satisfaction (수면 만족도)
    private Integer q7a;
    private Integer q7b;
    private Integer q7c;

    private Integer totalScore;
    private String interpretation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static SISResponseDto from(SISResponse entity) {
        return SISResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .q1a(entity.getQ1a())
                .q1b(entity.getQ1b())
                .q1c(entity.getQ1c())
                .q1d(entity.getQ1d())
                .q1e(entity.getQ1e())
                .q1f(entity.getQ1f())
                .q1g(entity.getQ1g())
                .q1h(entity.getQ1h())
                .q2a(entity.getQ2a())
                .q2b(entity.getQ2b())
                .q2c(entity.getQ2c())
                .q2d(entity.getQ2d())
                .q3a(entity.getQ3a())
                .q3b(entity.getQ3b())
                .q3c(entity.getQ3c())
                .q3d(entity.getQ3d())
                .q3e(entity.getQ3e())
                .q4a(entity.getQ4a())
                .q4b(entity.getQ4b())
                .q4c(entity.getQ4c())
                .q4d(entity.getQ4d())
                .q4e(entity.getQ4e())
                .q4f(entity.getQ4f())
                .q4g(entity.getQ4g())
                .q5a(entity.getQ5a())
                .q5b(entity.getQ5b())
                .q5c(entity.getQ5c())
                .q5d(entity.getQ5d())
                .q6a(entity.getQ6a())
                .q6b(entity.getQ6b())
                .q6c(entity.getQ6c())
                .q6d(entity.getQ6d())
                .q7a(entity.getQ7a())
                .q7b(entity.getQ7b())
                .q7c(entity.getQ7c())
                .totalScore(entity.getTotalScore())
                .interpretation(entity.getInterpretation())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
