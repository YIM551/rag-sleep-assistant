package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.SleepStageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 수면 단계 생성/수정 요청 DTO
 *
 * 웨어러블 기기나 수동 입력으로 수면 단계 데이터를 등록할 때 사용합니다.
 * 각 수면 단계의 시작/종료 시간과 타입을 기록합니다.
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "수면 단계 생성/수정 요청")
public class SleepStageRequestDto {

    @Schema(description = "수면 단계 타입 (DEEP, LIGHT, REM, AWAKE, UNKNOWN)",
            example = "DEEP", required = true)
    @NotNull(message = "수면 단계 타입은 필수입니다")
    private SleepStageType stageType;

    @Schema(description = "단계 시작 시간",
            example = "2024-01-15T23:30:00", required = true)
    @NotNull(message = "시작 시간은 필수입니다")
    private LocalDateTime startTime;

    @Schema(description = "단계 종료 시간",
            example = "2024-01-16T01:00:00", required = true)
    @NotNull(message = "종료 시간은 필수입니다")
    private LocalDateTime endTime;

    @Schema(description = "측정 신뢰도 (0-100)",
            example = "95", minimum = "0", maximum = "100")
    @Min(value = 0, message = "신뢰도는 0 이상이어야 합니다")
    @Max(value = 100, message = "신뢰도는 100 이하여야 합니다")
    private Integer confidenceScore;

    @Schema(description = "추가 메타데이터 (JSON 형태)",
            example = "{\"movement_intensity\": 2.5, \"hrv\": 45}")
    @Size(max = 2000, message = "메타데이터는 2000자를 초과할 수 없습니다")
    private String metadata;

    /**
     * 시간 일관성 검증
     * 종료 시간이 시작 시간보다 이후여야 함
     */
    public void validateTimeConsistency() {
        if (startTime != null && endTime != null) {
            if (!endTime.isAfter(startTime)) {
                throw new IllegalArgumentException("종료 시간은 시작 시간보다 이후여야 합니다");
            }

            // 수면 단계는 최대 12시간을 초과할 수 없음
            long durationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime);
            if (durationMinutes > 720) {
                throw new IllegalArgumentException("수면 단계 지속 시간은 12시간(720분)을 초과할 수 없습니다");
            }
        }
    }

    /**
     * 수면 단계 타입별 합리성 검증
     */
    public void validateStageTypeConsistency() {
        if (startTime != null && endTime != null && stageType != null) {
            long durationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime);

            // REM 수면은 일반적으로 5-120분
            if (stageType == SleepStageType.REM && (durationMinutes < 1 || durationMinutes > 120)) {
                // 경고만 로그 (서비스 레이어에서 처리)
            }

            // 깊은 수면은 일반적으로 10-120분
            if (stageType == SleepStageType.DEEP && (durationMinutes < 1 || durationMinutes > 120)) {
                // 경고만 로그 (서비스 레이어에서 처리)
            }
        }
    }
}
