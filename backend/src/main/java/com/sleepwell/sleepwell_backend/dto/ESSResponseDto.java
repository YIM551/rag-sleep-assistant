package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.entity.ESSResponse;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ESS 설문 응답 DTO
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ESSResponseDto {

    private Long id;
    private Long userId;
    private Integer q1;
    private Integer q2;
    private Integer q3;
    private Integer q4;
    private Integer q5;
    private Integer q6;
    private Integer q7;
    private Integer q8;
    private Integer totalScore;
    private String interpretation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static ESSResponseDto from(ESSResponse entity) {
        return ESSResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .q1(entity.getQ1())
                .q2(entity.getQ2())
                .q3(entity.getQ3())
                .q4(entity.getQ4())
                .q5(entity.getQ5())
                .q6(entity.getQ6())
                .q7(entity.getQ7())
                .q8(entity.getQ8())
                .totalScore(entity.getTotalScore())
                .interpretation(entity.getInterpretation())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
