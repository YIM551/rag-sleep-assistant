package com.sleepwell.sleepwell_backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * PSQI 설문 응답 요청 DTO
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PSQIResponseRequestDto {

    @NotBlank(message = "취침 시간은 필수입니다")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "취침 시간은 HH:MM 형식이어야 합니다")
    private String q1Bedtime;

    @NotNull(message = "잠들기까지 걸린 시간(시)은 필수입니다")
    @Min(value = 0, message = "시간은 0 이상이어야 합니다")
    private Integer q2H;

    @NotNull(message = "잠들기까지 걸린 시간(분)은 필수입니다")
    @Min(value = 0, message = "분은 0 이상이어야 합니다")
    @Max(value = 59, message = "분은 59 이하여야 합니다")
    private Integer q2M;

    @NotBlank(message = "기상 시간은 필수입니다")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "기상 시간은 HH:MM 형식이어야 합니다")
    private String q3Waketime;

    @NotNull(message = "실제 수면 시간(시)은 필수입니다")
    @Min(value = 0, message = "시간은 0 이상이어야 합니다")
    private Integer q4H;

    @NotNull(message = "실제 수면 시간(분)은 필수입니다")
    @Min(value = 0, message = "분은 0 이상이어야 합니다")
    @Max(value = 59, message = "분은 59 이하여야 합니다")
    private Integer q4M;

    @NotNull(message = "q5a는 필수입니다")
    @Min(0) @Max(3)
    private Integer q5a;

    @NotNull(message = "q5b는 필수입니다")
    @Min(0) @Max(3)
    private Integer q5b;

    @NotNull(message = "q5c는 필수입니다")
    @Min(0) @Max(3)
    private Integer q5c;

    @NotNull(message = "q5d는 필수입니다")
    @Min(0) @Max(3)
    private Integer q5d;

    @NotNull(message = "q5e는 필수입니다")
    @Min(0) @Max(3)
    private Integer q5e;

    @NotNull(message = "q5f는 필수입니다")
    @Min(0) @Max(3)
    private Integer q5f;

    @NotNull(message = "q5g는 필수입니다")
    @Min(0) @Max(3)
    private Integer q5g;

    @NotNull(message = "q5h는 필수입니다")
    @Min(0) @Max(3)
    private Integer q5h;

    @NotNull(message = "q5i는 필수입니다")
    @Min(0) @Max(3)
    private Integer q5i;

    @NotNull(message = "q5j는 필수입니다")
    @Min(0) @Max(3)
    private Integer q5j;

    private String q5jReason;

    @NotNull(message = "q6은 필수입니다")
    @Min(0) @Max(3)
    private Integer q6;

    @NotNull(message = "q7은 필수입니다")
    @Min(0) @Max(3)
    private Integer q7;

    @NotNull(message = "q8은 필수입니다")
    @Min(0) @Max(3)
    private Integer q8;

    @NotNull(message = "q9는 필수입니다")
    @Min(0) @Max(3)
    private Integer q9;

    @NotNull(message = "q10a는 필수입니다")
    @Min(0) @Max(3)
    private Integer q10a;

    @NotNull(message = "q10b는 필수입니다")
    @Min(0) @Max(3)
    private Integer q10b;

    @NotNull(message = "q10c는 필수입니다")
    @Min(0) @Max(3)
    private Integer q10c;

    @NotNull(message = "q10d는 필수입니다")
    @Min(0) @Max(3)
    private Integer q10d;

    @NotNull(message = "q10e는 필수입니다")
    @Min(0) @Max(3)
    private Integer q10e;

    private String q10eDetail;

    /**
     * 데이터 무결성 검증
     */
    public void validateDataIntegrity() {
        // 시간 형식 검증은 @Pattern 어노테이션으로 처리

        // 수면 시간이 음수가 아닌지 검증
        int totalSleepMinutes = (q4H * 60) + q4M;
        if (totalSleepMinutes < 0 || totalSleepMinutes > 1440) {
            throw new IllegalArgumentException("실제 수면 시간은 0-24시간 사이여야 합니다");
        }
    }
}
