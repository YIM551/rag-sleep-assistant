package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.entity.ASMRPlaySession;
import com.sleepwell.sleepwell_backend.enums.ASMRSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * ASMR 타이머 상태 응답 DTO
 */
@Getter
@Builder
@Schema(description = "ASMR 타이머 상태 응답")
public class ASMRTimerResponseDto {

    @Schema(description = "세션 ID", example = "123")
    private Long sessionId;

    @Schema(description = "콘텐츠 ID", example = "456")
    private Long contentId;

    @Schema(description = "콘텐츠 제목", example = "빗소리 ASMR")
    private String contentTitle;

    @Schema(description = "세션 상태", example = "ACTIVE")
    private ASMRSessionStatus status;

    @Schema(description = "타이머 설정 시간 (분)", example = "30")
    private Integer timerMinutes;

    @Schema(description = "타이머 남은 시간 (분)", example = "23")
    private Long remainingMinutes;

    @Schema(description = "타이머 남은 시간 (초)", example = "1380")
    private Long remainingSeconds;

    @Schema(description = "타이머 만료 시간", example = "2025-01-17T22:30:00")
    private LocalDateTime timerExpiresAt;

    @Schema(description = "재생 시작 시간", example = "2025-01-17T22:00:00")
    private LocalDateTime startedAt;

    @Schema(description = "총 재생 시간 (초)", example = "1800")
    private Integer totalPlayedSeconds;

    @Schema(description = "페이드아웃 활성화 여부", example = "true")
    private Boolean fadeOutEnabled;

    @Schema(description = "페이드아웃 지속 시간 (초)", example = "10")
    private Integer fadeOutDurationSeconds;

    @Schema(description = "재생 품질", example = "medium")
    private String playbackQuality;

    @Schema(description = "타이머 활성화 여부", example = "true")
    private Boolean hasTimer;

    @Schema(description = "페이드아웃 진행 중 여부", example = "false")
    private Boolean isFadingOut;

    @Schema(description = "타이머 만료 여부", example = "false")
    private Boolean isExpired;

    /**
     * ASMRPlaySession에서 DTO 생성
     */
    public static ASMRTimerResponseDto from(ASMRPlaySession session) {
        Long remainingSeconds = null;
        Long remainingMinutes = null;

        if (session.hasTimer() && !session.isTimerExpired()) {
            remainingSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), session.getTimerExpiresAt());
            remainingMinutes = remainingSeconds / 60;
        }

        return ASMRTimerResponseDto.builder()
                .sessionId(session.getId())
                .contentId(session.getAsmrContent().getId())
                .contentTitle(session.getAsmrContent().getTitle())
                .status(session.getStatus())
                .timerMinutes(session.getTimerMinutes())
                .remainingMinutes(remainingMinutes)
                .remainingSeconds(remainingSeconds)
                .timerExpiresAt(session.getTimerExpiresAt())
                .startedAt(session.getStartedAt())
                .totalPlayedSeconds(session.getTotalPlayedSeconds())
                .fadeOutEnabled(session.getFadeOutEnabled())
                .fadeOutDurationSeconds(session.getFadeOutDurationSeconds())
                .playbackQuality(session.getPlaybackQuality())
                .hasTimer(session.hasTimer())
                .isFadingOut(session.getStatus() == ASMRSessionStatus.FADING_OUT)
                .isExpired(session.isTimerExpired())
                .build();
    }

    /**
     * 타이머 진행률 계산 (0-100%)
     */
    public Double getProgressPercentage() {
        if (!hasTimer || timerMinutes == null || remainingMinutes == null) {
            return null;
        }

        if (remainingMinutes <= 0) {
            return 100.0;
        }

        double totalSeconds = timerMinutes * 60.0;
        double elapsed = totalSeconds - (remainingSeconds != null ? remainingSeconds : 0);
        return (elapsed / totalSeconds) * 100.0;
    }

    /**
     * 사용자 친화적인 남은 시간 문자열
     */
    public String getRemainingTimeFormatted() {
        if (remainingSeconds == null || remainingSeconds <= 0) {
            return "타이머 없음";
        }

        long hours = remainingSeconds / 3600;
        long minutes = (remainingSeconds % 3600) / 60;
        long seconds = remainingSeconds % 60;

        if (hours > 0) {
            return String.format("%d시간 %d분", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d분 %d초", minutes, seconds);
        } else {
            return String.format("%d초", seconds);
        }
    }

    /**
     * 세션 상태의 한국어 설명
     */
    public String getStatusDescription() {
        if (status != null) {
            return status.getDescription();
        }
        return "알 수 없음";
    }
}