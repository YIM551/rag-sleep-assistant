package com.sleepwell.sleepwell_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ASMR 타이머 설정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "ASMR 타이머 설정 요청")
public class ASMRTimerRequestDto {

    @Schema(description = "타이머 시간 (분 단위)", example = "30", minimum = "1", maximum = "480")
    @NotNull(message = "타이머 시간은 필수입니다")
    @Min(value = 1, message = "타이머는 최소 1분이어야 합니다")
    @Max(value = 480, message = "타이머는 최대 8시간(480분)까지 설정 가능합니다")
    private Integer timerMinutes;

    @Schema(description = "페이드아웃 사용 여부", example = "true", defaultValue = "true")
    private Boolean fadeOutEnabled = true;

    @Schema(description = "페이드아웃 지속 시간 (초 단위)", example = "10", minimum = "5", maximum = "60")
    @Min(value = 5, message = "페이드아웃 시간은 최소 5초입니다")
    @Max(value = 60, message = "페이드아웃 시간은 최대 60초입니다")
    private Integer fadeOutDurationSeconds = 10;

    @Schema(description = "재생 품질", example = "medium", allowableValues = {"high", "medium", "low"})
    private String playbackQuality = "medium";

    @Builder
    public ASMRTimerRequestDto(Integer timerMinutes, Boolean fadeOutEnabled,
                              Integer fadeOutDurationSeconds, String playbackQuality) {
        this.timerMinutes = timerMinutes;
        this.fadeOutEnabled = fadeOutEnabled != null ? fadeOutEnabled : true;
        this.fadeOutDurationSeconds = fadeOutDurationSeconds != null ? fadeOutDurationSeconds : 10;
        this.playbackQuality = playbackQuality != null ? playbackQuality : "medium";
    }

    /**
     * 유효한 재생 품질인지 확인
     */
    public boolean isValidQuality() {
        return playbackQuality != null &&
               (playbackQuality.equals("high") || playbackQuality.equals("medium") || playbackQuality.equals("low"));
    }

    /**
     * 프리셋 타이머 시간인지 확인 (Spotify 스타일)
     */
    public boolean isPresetTimer() {
        return timerMinutes != null &&
               (timerMinutes == 5 || timerMinutes == 10 || timerMinutes == 15 ||
                timerMinutes == 30 || timerMinutes == 45 || timerMinutes == 60);
    }
}