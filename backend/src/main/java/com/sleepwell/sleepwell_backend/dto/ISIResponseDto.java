package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.entity.ISIResponse;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ISI 설문 응답 DTO
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ISIResponseDto {

    private Long id;
    private Long userId;
    private Integer q1a;
    private Integer q1b;
    private Integer q1c;
    private Integer q2;
    private Integer q3;
    private Integer q4;
    private Integer q5;
    private Integer totalScore;
    private String interpretation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static ISIResponseDto from(ISIResponse entity) {
        return ISIResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .q1a(entity.getQ1a())
                .q1b(entity.getQ1b())
                .q1c(entity.getQ1c())
                .q2(entity.getQ2())
                .q3(entity.getQ3())
                .q4(entity.getQ4())
                .q5(entity.getQ5())
                .totalScore(entity.getTotalScore())
                .interpretation(entity.getInterpretation())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
