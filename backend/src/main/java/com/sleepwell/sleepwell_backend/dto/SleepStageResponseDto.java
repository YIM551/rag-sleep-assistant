package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.entity.SleepStage;
import com.sleepwell.sleepwell_backend.enums.SleepStageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 수면 단계 응답 DTO
 *
 * 수면 단계 조회 시 클라이언트에게 전달되는 데이터 구조입니다.
 * 엔티티의 모든 정보와 계산된 지속 시간을 포함합니다.
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "수면 단계 응답")
public class SleepStageResponseDto {

    @Schema(description = "수면 단계 ID", example = "1")
    private Long id;

    @Schema(description = "수면 기록 ID", example = "123")
    private Long sleepRecordId;

    @Schema(description = "수면 단계 타입", example = "DEEP")
    private SleepStageType stageType;

    @Schema(description = "수면 단계 타입 설명", example = "깊은 수면")
    private String stageTypeDescription;

    @Schema(description = "단계 시작 시간", example = "2024-01-15T23:30:00")
    private LocalDateTime startTime;

    @Schema(description = "단계 종료 시간", example = "2024-01-16T01:00:00")
    private LocalDateTime endTime;

    @Schema(description = "지속 시간 (분)", example = "90")
    private Integer durationMinutes;

    @Schema(description = "측정 신뢰도 (0-100)", example = "95")
    private Integer confidenceScore;

    @Schema(description = "추가 메타데이터 (JSON 형태)",
            example = "{\"movement_intensity\": 2.5, \"hrv\": 45}")
    private String metadata;

    @Schema(description = "회복성 수면 여부 (DEEP 또는 REM)", example = "true")
    private Boolean isRestorative;

    /**
     * SleepStage 엔티티로부터 ResponseDto를 생성하는 정적 메서드
     *
     * @param sleepStage 수면 단계 엔티티
     * @return 변환된 응답 DTO
     */
    public static SleepStageResponseDto from(SleepStage sleepStage) {
        return SleepStageResponseDto.builder()
                .id(sleepStage.getId())
                .sleepRecordId(sleepStage.getSleepRecord().getId())
                .stageType(sleepStage.getStageType())
                .stageTypeDescription(sleepStage.getStageType().getDescription())
                .startTime(sleepStage.getStartTime())
                .endTime(sleepStage.getEndTime())
                .durationMinutes(sleepStage.getDurationMinutes())
                .confidenceScore(sleepStage.getConfidenceScore())
                .metadata(sleepStage.getMetadata())
                .isRestorative(sleepStage.isRestorative())
                .build();
    }

    /**
     * 요약 버전 DTO 생성 (리스트 조회용)
     * 핵심 정보만 포함
     *
     * @param sleepStage 수면 단계 엔티티
     * @return 요약된 응답 DTO
     */
    public static SleepStageResponseDto fromSummary(SleepStage sleepStage) {
        return SleepStageResponseDto.builder()
                .id(sleepStage.getId())
                .sleepRecordId(sleepStage.getSleepRecord().getId())
                .stageType(sleepStage.getStageType())
                .stageTypeDescription(sleepStage.getStageType().getDescription())
                .startTime(sleepStage.getStartTime())
                .endTime(sleepStage.getEndTime())
                .durationMinutes(sleepStage.getDurationMinutes())
                .isRestorative(sleepStage.isRestorative())
                .build();
    }
}
