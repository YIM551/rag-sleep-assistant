package com.sleepwell.sleepwell_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * ISI 설문 응답 요청 DTO
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ISIResponseRequestDto {

    @NotNull(message = "q1a는 필수입니다")
    @Min(value = 0, message = "q1a는 0 이상이어야 합니다")
    @Max(value = 4, message = "q1a는 4 이하여야 합니다")
    private Integer q1a;

    @NotNull(message = "q1b는 필수입니다")
    @Min(value = 0, message = "q1b는 0 이상이어야 합니다")
    @Max(value = 4, message = "q1b는 4 이하여야 합니다")
    private Integer q1b;

    @NotNull(message = "q1c는 필수입니다")
    @Min(value = 0, message = "q1c는 0 이상이어야 합니다")
    @Max(value = 4, message = "q1c는 4 이하여야 합니다")
    private Integer q1c;

    @NotNull(message = "q2는 필수입니다")
    @Min(value = 0, message = "q2는 0 이상이어야 합니다")
    @Max(value = 4, message = "q2는 4 이하여야 합니다")
    private Integer q2;

    @NotNull(message = "q3은 필수입니다")
    @Min(value = 0, message = "q3은 0 이상이어야 합니다")
    @Max(value = 4, message = "q3은 4 이하여야 합니다")
    private Integer q3;

    @NotNull(message = "q4는 필수입니다")
    @Min(value = 0, message = "q4는 0 이상이어야 합니다")
    @Max(value = 4, message = "q4는 4 이하여야 합니다")
    private Integer q4;

    @NotNull(message = "q5는 필수입니다")
    @Min(value = 0, message = "q5는 0 이상이어야 합니다")
    @Max(value = 4, message = "q5는 4 이하여야 합니다")
    private Integer q5;

    /**
     * 데이터 무결성 검증
     */
    public void validateDataIntegrity() {
        int total = q1a + q1b + q1c + q2 + q3 + q4 + q5;
        if (total < 0 || total > 28) {
            throw new IllegalArgumentException("ISI 총점은 0-28 사이여야 합니다: " + total);
        }
    }
}
