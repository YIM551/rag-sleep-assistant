package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.entity.ASMRPlaySession;
import com.sleepwell.sleepwell_backend.enums.ASMRSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ASMR 세션 상태 정보 DTO
 */
@Getter
@Builder
@Schema(description = "ASMR 세션 상태 정보")
public class ASMRSessionStatusDto {

    @Schema(description = "세션 ID", example = "123")
    private Long sessionId;

    @Schema(description = "사용자 ID", example = "456")
    private Long userId;

    @Schema(description = "콘텐츠 ID", example = "789")
    private Long contentId;

    @Schema(description = "콘텐츠 제목", example = "자연 소리 ASMR")
    private String contentTitle;

    @Schema(description = "세션 상태", example = "ACTIVE")
    private ASMRSessionStatus status;

    @Schema(description = "재생 시작 시간", example = "2025-01-17T22:00:00")
    private LocalDateTime startedAt;

    @Schema(description = "재생 종료 시간", example = "2025-01-17T22:30:00")
    private LocalDateTime endedAt;

    @Schema(description = "총 재생 시간 (초)", example = "1800")
    private Integer totalPlayedSeconds;

    @Schema(description = "재생 품질", example = "medium")
    private String playbackQuality;

    @Schema(description = "타이머 설정 시간 (분)", example = "30")
    private Integer timerMinutes;

    @Schema(description = "타이머 만료 시간", example = "2025-01-17T22:30:00")
    private LocalDateTime timerExpiresAt;

    @Schema(description = "페이드아웃 활성화 여부", example = "true")
    private Boolean fadeOutEnabled;

    @Schema(description = "세션 진행 중 여부", example = "true")
    private Boolean isOngoing;

    @Schema(description = "타이머 활성화 여부", example = "true")
    private Boolean hasTimer;

    /**
     * ASMRPlaySession에서 DTO 생성
     */
    public static ASMRSessionStatusDto from(ASMRPlaySession session) {
        return ASMRSessionStatusDto.builder()
                .sessionId(session.getId())
                .userId(session.getUser().getId())
                .contentId(session.getAsmrContent().getId())
                .contentTitle(session.getAsmrContent().getTitle())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .totalPlayedSeconds(session.getTotalPlayedSeconds())
                .playbackQuality(session.getPlaybackQuality())
                .timerMinutes(session.getTimerMinutes())
                .timerExpiresAt(session.getTimerExpiresAt())
                .fadeOutEnabled(session.getFadeOutEnabled())
                .isOngoing(session.isOngoing())
                .hasTimer(session.hasTimer())
                .build();
    }

    /**
     * 세션 리스트를 DTO 리스트로 변환
     */
    public static List<ASMRSessionStatusDto> fromList(List<ASMRPlaySession> sessions) {
        return sessions.stream()
                .map(ASMRSessionStatusDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 재생 시간을 분:초 형식으로 반환
     */
    public String getFormattedPlayTime() {
        if (totalPlayedSeconds == null || totalPlayedSeconds <= 0) {
            return "0:00";
        }

        int minutes = totalPlayedSeconds / 60;
        int seconds = totalPlayedSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * 세션 지속 시간 계산 (분 단위)
     */
    public Long getSessionDurationMinutes() {
        if (startedAt == null) return null;

        LocalDateTime endTime = endedAt != null ? endedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).toMinutes();
    }

    /**
     * 타이머 사용 여부와 완료 여부 요약
     */
    public String getTimerSummary() {
        if (!hasTimer) {
            return "타이머 사용 안함";
        }

        if (status == ASMRSessionStatus.TIMER_EXPIRED) {
            return String.format("타이머 완료 (%d분)", timerMinutes);
        }

        if (timerMinutes != null) {
            return String.format("타이머 설정 (%d분)", timerMinutes);
        }

        return "타이머 설정됨";
    }
}