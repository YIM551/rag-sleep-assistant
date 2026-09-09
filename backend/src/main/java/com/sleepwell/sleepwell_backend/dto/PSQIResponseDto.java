package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.entity.PSQIResponse;
import lombok.*;

import java.time.LocalDateTime;

/**
 * PSQI 설문 응답 DTO
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PSQIResponseDto {

    private Long id;
    private Long userId;
    private String q1Bedtime;
    private Integer q2H;
    private Integer q2M;
    private String q3Waketime;
    private Integer q4H;
    private Integer q4M;
    private Integer q5a;
    private Integer q5b;
    private Integer q5c;
    private Integer q5d;
    private Integer q5e;
    private Integer q5f;
    private Integer q5g;
    private Integer q5h;
    private Integer q5i;
    private Integer q5j;
    private String q5jReason;
    private Integer q6;
    private Integer q7;
    private Integer q8;
    private Integer q9;
    private Integer q10a;
    private Integer q10b;
    private Integer q10c;
    private Integer q10d;
    private Integer q10e;
    private String q10eDetail;
    private Integer component1Quality;
    private Integer component2Latency;
    private Integer component3Duration;
    private Integer component4Efficiency;
    private Integer component5Disturbance;
    private Integer component6Medication;
    private Integer component7Dysfunction;
    private Integer totalScore;
    private String interpretation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static PSQIResponseDto from(PSQIResponse entity) {
        return PSQIResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .q1Bedtime(entity.getQ1Bedtime())
                .q2H(entity.getQ2H())
                .q2M(entity.getQ2M())
                .q3Waketime(entity.getQ3Waketime())
                .q4H(entity.getQ4H())
                .q4M(entity.getQ4M())
                .q5a(entity.getQ5a())
                .q5b(entity.getQ5b())
                .q5c(entity.getQ5c())
                .q5d(entity.getQ5d())
                .q5e(entity.getQ5e())
                .q5f(entity.getQ5f())
                .q5g(entity.getQ5g())
                .q5h(entity.getQ5h())
                .q5i(entity.getQ5i())
                .q5j(entity.getQ5j())
                .q5jReason(entity.getQ5jReason())
                .q6(entity.getQ6())
                .q7(entity.getQ7())
                .q8(entity.getQ8())
                .q9(entity.getQ9())
                .q10a(entity.getQ10a())
                .q10b(entity.getQ10b())
                .q10c(entity.getQ10c())
                .q10d(entity.getQ10d())
                .q10e(entity.getQ10e())
                .q10eDetail(entity.getQ10eDetail())
                .component1Quality(entity.getComponent1Quality())
                .component2Latency(entity.getComponent2Latency())
                .component3Duration(entity.getComponent3Duration())
                .component4Efficiency(entity.getComponent4Efficiency())
                .component5Disturbance(entity.getComponent5Disturbance())
                .component6Medication(entity.getComponent6Medication())
                .component7Dysfunction(entity.getComponent7Dysfunction())
                .totalScore(entity.getTotalScore())
                .interpretation(entity.getInterpretation())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
