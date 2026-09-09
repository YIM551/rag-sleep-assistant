package com.sleepwell.sleepwell_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * BDI-II 설문 응답 요청 DTO
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BDIResponseRequestDto {

    @NotNull(message = "q1은 필수입니다")
    @Min(0) @Max(3)
    private Integer q1;

    @NotNull(message = "q2는 필수입니다")
    @Min(0) @Max(3)
    private Integer q2;

    @NotNull(message = "q3은 필수입니다")
    @Min(0) @Max(3)
    private Integer q3;

    @NotNull(message = "q4는 필수입니다")
    @Min(0) @Max(3)
    private Integer q4;

    @NotNull(message = "q5는 필수입니다")
    @Min(0) @Max(3)
    private Integer q5;

    @NotNull(message = "q6은 필수입니다")
    @Min(0) @Max(3)
    private Integer q6;

    @NotNull(message = "q7은 필수입니다")
    @Min(0) @Max(3)
    private Integer q7;

    @NotNull(message = "q8은 필수입니다")
    @Min(0) @Max(3)
    private Integer q8;

    @NotNull(message = "q9는 필수입니다 (자살 사고 문항)")
    @Min(0) @Max(3)
    private Integer q9;

    @NotNull(message = "q10은 필수입니다")
    @Min(0) @Max(3)
    private Integer q10;

    @NotNull(message = "q11은 필수입니다")
    @Min(0) @Max(3)
    private Integer q11;

    @NotNull(message = "q12는 필수입니다")
    @Min(0) @Max(3)
    private Integer q12;

    @NotNull(message = "q13은 필수입니다")
    @Min(0) @Max(3)
    private Integer q13;

    @NotNull(message = "q14는 필수입니다")
    @Min(0) @Max(3)
    private Integer q14;

    @NotNull(message = "q15는 필수입니다")
    @Min(0) @Max(3)
    private Integer q15;

    @NotNull(message = "q16은 필수입니다")
    @Min(0) @Max(3)
    private Integer q16;

    @NotNull(message = "q17은 필수입니다")
    @Min(0) @Max(3)
    private Integer q17;

    @NotNull(message = "q18은 필수입니다")
    @Min(0) @Max(3)
    private Integer q18;

    @NotNull(message = "q19는 필수입니다")
    @Min(0) @Max(3)
    private Integer q19;

    @NotNull(message = "q20은 필수입니다")
    @Min(0) @Max(3)
    private Integer q20;

    @NotNull(message = "q21은 필수입니다")
    @Min(0) @Max(3)
    private Integer q21;

    /**
     * 데이터 무결성 검증
     */
    public void validateDataIntegrity() {
        int total = q1 + q2 + q3 + q4 + q5 + q6 + q7 + q8 + q9 + q10 +
                    q11 + q12 + q13 + q14 + q15 + q16 + q17 + q18 + q19 + q20 + q21;

        if (total < 0 || total > 63) {
            throw new IllegalArgumentException("BDI-II 총점은 0-63 사이여야 합니다: " + total);
        }

        // 자살 위험도 경고 로깅 (q9 >= 2)
        if (q9 >= 2) {
            // 서비스 레이어에서 처리할 것
        }
    }
}
