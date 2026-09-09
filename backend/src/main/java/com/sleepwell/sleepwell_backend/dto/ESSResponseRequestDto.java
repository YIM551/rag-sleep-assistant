package com.sleepwell.sleepwell_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * ESS 설문 응답 요청 DTO
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ESSResponseRequestDto {

    @NotNull(message = "q1은 필수입니다")
    @Min(value = 0, message = "q1은 0 이상이어야 합니다")
    @Max(value = 3, message = "q1은 3 이하여야 합니다")
    private Integer q1;

    @NotNull(message = "q2는 필수입니다")
    @Min(value = 0, message = "q2는 0 이상이어야 합니다")
    @Max(value = 3, message = "q2는 3 이하여야 합니다")
    private Integer q2;

    @NotNull(message = "q3은 필수입니다")
    @Min(value = 0, message = "q3은 0 이상이어야 합니다")
    @Max(value = 3, message = "q3은 3 이하여야 합니다")
    private Integer q3;

    @NotNull(message = "q4는 필수입니다")
    @Min(value = 0, message = "q4는 0 이상이어야 합니다")
    @Max(value = 3, message = "q4는 3 이하여야 합니다")
    private Integer q4;

    @NotNull(message = "q5는 필수입니다")
    @Min(value = 0, message = "q5는 0 이상이어야 합니다")
    @Max(value = 3, message = "q5는 3 이하여야 합니다")
    private Integer q5;

    @NotNull(message = "q6은 필수입니다")
    @Min(value = 0, message = "q6은 0 이상이어야 합니다")
    @Max(value = 3, message = "q6은 3 이하여야 합니다")
    private Integer q6;

    @NotNull(message = "q7은 필수입니다")
    @Min(value = 0, message = "q7은 0 이상이어야 합니다")
    @Max(value = 3, message = "q7은 3 이하여야 합니다")
    private Integer q7;

    @NotNull(message = "q8은 필수입니다")
    @Min(value = 0, message = "q8은 0 이상이어야 합니다")
    @Max(value = 3, message = "q8은 3 이하여야 합니다")
    private Integer q8;

    /**
     * 데이터 무결성 검증
     */
    public void validateDataIntegrity() {
        int total = q1 + q2 + q3 + q4 + q5 + q6 + q7 + q8;
        if (total < 0 || total > 24) {
            throw new IllegalArgumentException("ESS 총점은 0-24 사이여야 합니다: " + total);
        }
    }
}
