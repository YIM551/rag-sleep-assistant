package com.sleepwell.sleepwell_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * SIS 설문 응답 요청 DTO
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SISResponseRequestDto {

    // ===== Domain 1: Daily Activities (일상 활동) - 8 questions =====
    @NotNull(message = "q1a는 필수입니다")
    @Min(value = 1, message = "q1a는 1 이상이어야 합니다")
    @Max(value = 5, message = "q1a는 5 이하여야 합니다")
    private Integer q1a;

    @NotNull(message = "q1b는 필수입니다")
    @Min(value = 1, message = "q1b는 1 이상이어야 합니다")
    @Max(value = 5, message = "q1b는 5 이하여야 합니다")
    private Integer q1b;

    @NotNull(message = "q1c는 필수입니다")
    @Min(value = 1, message = "q1c는 1 이상이어야 합니다")
    @Max(value = 5, message = "q1c는 5 이하여야 합니다")
    private Integer q1c;

    @NotNull(message = "q1d는 필수입니다")
    @Min(value = 1, message = "q1d는 1 이상이어야 합니다")
    @Max(value = 5, message = "q1d는 5 이하여야 합니다")
    private Integer q1d;

    @NotNull(message = "q1e는 필수입니다")
    @Min(value = 1, message = "q1e는 1 이상이어야 합니다")
    @Max(value = 5, message = "q1e는 5 이하여야 합니다")
    private Integer q1e;

    @NotNull(message = "q1f는 필수입니다")
    @Min(value = 1, message = "q1f는 1 이상이어야 합니다")
    @Max(value = 5, message = "q1f는 5 이하여야 합니다")
    private Integer q1f;

    @NotNull(message = "q1g는 필수입니다")
    @Min(value = 1, message = "q1g는 1 이상이어야 합니다")
    @Max(value = 5, message = "q1g는 5 이하여야 합니다")
    private Integer q1g;

    @NotNull(message = "q1h는 필수입니다")
    @Min(value = 1, message = "q1h는 1 이상이어야 합니다")
    @Max(value = 5, message = "q1h는 5 이하여야 합니다")
    private Integer q1h;

    // ===== Domain 2: Daytime Emotional Impact (주간 정서 영향) - 4 questions =====
    @NotNull(message = "q2a는 필수입니다")
    @Min(value = 1, message = "q2a는 1 이상이어야 합니다")
    @Max(value = 5, message = "q2a는 5 이하여야 합니다")
    private Integer q2a;

    @NotNull(message = "q2b는 필수입니다")
    @Min(value = 1, message = "q2b는 1 이상이어야 합니다")
    @Max(value = 5, message = "q2b는 5 이하여야 합니다")
    private Integer q2b;

    @NotNull(message = "q2c는 필수입니다")
    @Min(value = 1, message = "q2c는 1 이상이어야 합니다")
    @Max(value = 5, message = "q2c는 5 이하여야 합니다")
    private Integer q2c;

    @NotNull(message = "q2d는 필수입니다")
    @Min(value = 1, message = "q2d는 1 이상이어야 합니다")
    @Max(value = 5, message = "q2d는 5 이하여야 합니다")
    private Integer q2d;

    // ===== Domain 3: Nighttime Emotional Impact (야간 정서 영향) - 5 questions =====
    @NotNull(message = "q3a는 필수입니다")
    @Min(value = 1, message = "q3a는 1 이상이어야 합니다")
    @Max(value = 5, message = "q3a는 5 이하여야 합니다")
    private Integer q3a;

    @NotNull(message = "q3b는 필수입니다")
    @Min(value = 1, message = "q3b는 1 이상이어야 합니다")
    @Max(value = 5, message = "q3b는 5 이하여야 합니다")
    private Integer q3b;

    @NotNull(message = "q3c는 필수입니다")
    @Min(value = 1, message = "q3c는 1 이상이어야 합니다")
    @Max(value = 5, message = "q3c는 5 이하여야 합니다")
    private Integer q3c;

    @NotNull(message = "q3d는 필수입니다")
    @Min(value = 1, message = "q3d는 1 이상이어야 합니다")
    @Max(value = 5, message = "q3d는 5 이하여야 합니다")
    private Integer q3d;

    @NotNull(message = "q3e는 필수입니다")
    @Min(value = 1, message = "q3e는 1 이상이어야 합니다")
    @Max(value = 5, message = "q3e는 5 이하여야 합니다")
    private Integer q3e;

    // ===== Domain 4: Fatigue (피로) - 7 questions =====
    @NotNull(message = "q4a는 필수입니다")
    @Min(value = 1, message = "q4a는 1 이상이어야 합니다")
    @Max(value = 5, message = "q4a는 5 이하여야 합니다")
    private Integer q4a;

    @NotNull(message = "q4b는 필수입니다")
    @Min(value = 1, message = "q4b는 1 이상이어야 합니다")
    @Max(value = 5, message = "q4b는 5 이하여야 합니다")
    private Integer q4b;

    @NotNull(message = "q4c는 필수입니다")
    @Min(value = 1, message = "q4c는 1 이상이어야 합니다")
    @Max(value = 5, message = "q4c는 5 이하여야 합니다")
    private Integer q4c;

    @NotNull(message = "q4d는 필수입니다")
    @Min(value = 1, message = "q4d는 1 이상이어야 합니다")
    @Max(value = 5, message = "q4d는 5 이하여야 합니다")
    private Integer q4d;

    @NotNull(message = "q4e는 필수입니다")
    @Min(value = 1, message = "q4e는 1 이상이어야 합니다")
    @Max(value = 5, message = "q4e는 5 이하여야 합니다")
    private Integer q4e;

    @NotNull(message = "q4f는 필수입니다")
    @Min(value = 1, message = "q4f는 1 이상이어야 합니다")
    @Max(value = 5, message = "q4f는 5 이하여야 합니다")
    private Integer q4f;

    @NotNull(message = "q4g는 필수입니다")
    @Min(value = 1, message = "q4g는 1 이상이어야 합니다")
    @Max(value = 5, message = "q4g는 5 이하여야 합니다")
    private Integer q4g;

    // ===== Domain 5: Social Impact (사회적 영향) - 4 questions =====
    @NotNull(message = "q5a는 필수입니다")
    @Min(value = 1, message = "q5a는 1 이상이어야 합니다")
    @Max(value = 5, message = "q5a는 5 이하여야 합니다")
    private Integer q5a;

    @NotNull(message = "q5b는 필수입니다")
    @Min(value = 1, message = "q5b는 1 이상이어야 합니다")
    @Max(value = 5, message = "q5b는 5 이하여야 합니다")
    private Integer q5b;

    @NotNull(message = "q5c는 필수입니다")
    @Min(value = 1, message = "q5c는 1 이상이어야 합니다")
    @Max(value = 5, message = "q5c는 5 이하여야 합니다")
    private Integer q5c;

    @NotNull(message = "q5d는 필수입니다")
    @Min(value = 1, message = "q5d는 1 이상이어야 합니다")
    @Max(value = 5, message = "q5d는 5 이하여야 합니다")
    private Integer q5d;

    // ===== Domain 6: Mental Fatigue (정신적 피로) - 4 questions =====
    @NotNull(message = "q6a는 필수입니다")
    @Min(value = 1, message = "q6a는 1 이상이어야 합니다")
    @Max(value = 5, message = "q6a는 5 이하여야 합니다")
    private Integer q6a;

    @NotNull(message = "q6b는 필수입니다")
    @Min(value = 1, message = "q6b는 1 이상이어야 합니다")
    @Max(value = 5, message = "q6b는 5 이하여야 합니다")
    private Integer q6b;

    @NotNull(message = "q6c는 필수입니다")
    @Min(value = 1, message = "q6c는 1 이상이어야 합니다")
    @Max(value = 5, message = "q6c는 5 이하여야 합니다")
    private Integer q6c;

    @NotNull(message = "q6d는 필수입니다")
    @Min(value = 1, message = "q6d는 1 이상이어야 합니다")
    @Max(value = 5, message = "q6d는 5 이하여야 합니다")
    private Integer q6d;

    // ===== Domain 7: Sleep Satisfaction (수면 만족도) - 3 questions =====
    @NotNull(message = "q7a는 필수입니다")
    @Min(value = 1, message = "q7a는 1 이상이어야 합니다")
    @Max(value = 5, message = "q7a는 5 이하여야 합니다")
    private Integer q7a;

    @NotNull(message = "q7b는 필수입니다")
    @Min(value = 1, message = "q7b는 1 이상이어야 합니다")
    @Max(value = 5, message = "q7b는 5 이하여야 합니다")
    private Integer q7b;

    @NotNull(message = "q7c는 필수입니다")
    @Min(value = 1, message = "q7c는 1 이상이어야 합니다")
    @Max(value = 5, message = "q7c는 5 이하여야 합니다")
    private Integer q7c;

    /**
     * 데이터 무결성 검증
     */
    public void validateDataIntegrity() {
        int total = q1a + q1b + q1c + q1d + q1e + q1f + q1g + q1h +
                   q2a + q2b + q2c + q2d +
                   q3a + q3b + q3c + q3d + q3e +
                   q4a + q4b + q4c + q4d + q4e + q4f + q4g +
                   q5a + q5b + q5c + q5d +
                   q6a + q6b + q6c + q6d +
                   q7a + q7b + q7c;

        if (total < 35 || total > 175) {
            throw new IllegalArgumentException("SIS 총점은 35-175 사이여야 합니다: " + total);
        }
    }
}
