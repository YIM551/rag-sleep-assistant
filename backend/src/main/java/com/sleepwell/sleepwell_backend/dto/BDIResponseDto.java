package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.entity.BDIResponse;
import lombok.*;

import java.time.LocalDateTime;

/**
 * BDI-II 설문 응답 DTO
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BDIResponseDto {

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
    private Integer q9;
    private Integer q10;
    private Integer q11;
    private Integer q12;
    private Integer q13;
    private Integer q14;
    private Integer q15;
    private Integer q16;
    private Integer q17;
    private Integer q18;
    private Integer q19;
    private Integer q20;
    private Integer q21;
    private Integer totalScore;
    private String interpretation;
    private Boolean suicideRisk;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static BDIResponseDto from(BDIResponse entity) {
        return BDIResponseDto.builder()
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
                .q9(entity.getQ9())
                .q10(entity.getQ10())
                .q11(entity.getQ11())
                .q12(entity.getQ12())
                .q13(entity.getQ13())
                .q14(entity.getQ14())
                .q15(entity.getQ15())
                .q16(entity.getQ16())
                .q17(entity.getQ17())
                .q18(entity.getQ18())
                .q19(entity.getQ19())
                .q20(entity.getQ20())
                .q21(entity.getQ21())
                .totalScore(entity.getTotalScore())
                .interpretation(entity.getInterpretation())
                .suicideRisk(entity.getSuicideRisk())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
