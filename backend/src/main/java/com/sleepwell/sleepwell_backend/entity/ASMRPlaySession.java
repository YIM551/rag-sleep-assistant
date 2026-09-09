package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.ASMRSessionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ASMR 재생 세션 엔티티
 * 사용자의 ASMR 재생 세션과 타이머 설정을 관리합니다.
 */
@Entity
@Table(name = "asmr_play_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ASMRPlaySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 세션을 시작한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 재생 중인 ASMR 콘텐츠
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asmr_content_id", nullable = false)
    private ASMRContent asmrContent;

    /**
     * 세션 상태 (ACTIVE, PAUSED, STOPPED, TIMER_EXPIRED)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ASMRSessionStatus status;

    /**
     * 재생 시작 시간
     */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /**
     * 재생 종료 시간 (종료된 경우)
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * 타이머 설정 시간 (분 단위, null이면 타이머 없음)
     */
    @Column(name = "timer_minutes")
    private Integer timerMinutes;

    /**
     * 타이머 만료 예정 시간
     */
    @Column(name = "timer_expires_at")
    private LocalDateTime timerExpiresAt;

    /**
     * 재생 품질 (high, medium, low)
     */
    @Column(name = "playback_quality", length = 20)
    private String playbackQuality;

    /**
     * 총 재생 시간 (초 단위)
     */
    @Column(name = "total_played_seconds")
    private Integer totalPlayedSeconds = 0;

    /**
     * 페이드아웃 여부
     */
    @Column(name = "fade_out_enabled")
    private Boolean fadeOutEnabled = true;

    /**
     * 페이드아웃 시작 시간 (타이머 만료 전 몇 초부터 시작할지)
     */
    @Column(name = "fade_out_duration_seconds")
    private Integer fadeOutDurationSeconds = 10;

    /**
     * 세션 생성 시간
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 세션 수정 시간
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public ASMRPlaySession(User user, ASMRContent asmrContent, String playbackQuality,
                          Integer timerMinutes, Boolean fadeOutEnabled, Integer fadeOutDurationSeconds) {
        this.user = user;
        this.asmrContent = asmrContent;
        this.status = ASMRSessionStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
        this.playbackQuality = playbackQuality != null ? playbackQuality : "medium";
        this.timerMinutes = timerMinutes;
        this.fadeOutEnabled = fadeOutEnabled != null ? fadeOutEnabled : true;
        this.fadeOutDurationSeconds = fadeOutDurationSeconds != null ? fadeOutDurationSeconds : 10;

        if (timerMinutes != null && timerMinutes > 0) {
            this.timerExpiresAt = this.startedAt.plusMinutes(timerMinutes);
        }
    }

    /**
     * 타이머 설정
     */
    public void setTimer(Integer minutes) {
        this.timerMinutes = minutes;
        if (minutes != null && minutes > 0) {
            this.timerExpiresAt = LocalDateTime.now().plusMinutes(minutes);
        } else {
            this.timerExpiresAt = null;
        }
    }

    /**
     * 타이머 연장
     */
    public void extendTimer(Integer additionalMinutes) {
        if (this.timerExpiresAt != null && additionalMinutes > 0) {
            this.timerExpiresAt = this.timerExpiresAt.plusMinutes(additionalMinutes);
            if (this.timerMinutes != null) {
                this.timerMinutes += additionalMinutes;
            }
        }
    }

    /**
     * 세션 일시 정지
     */
    public void pause() {
        if (this.status == ASMRSessionStatus.ACTIVE) {
            this.status = ASMRSessionStatus.PAUSED;
        }
    }

    /**
     * 세션 재개
     */
    public void resume() {
        if (this.status == ASMRSessionStatus.PAUSED) {
            this.status = ASMRSessionStatus.ACTIVE;
        }
    }

    /**
     * 세션 중지
     */
    public void stop() {
        this.status = ASMRSessionStatus.STOPPED;
        this.endedAt = LocalDateTime.now();
    }

    /**
     * 타이머 만료로 세션 종료
     */
    public void expireByTimer() {
        this.status = ASMRSessionStatus.TIMER_EXPIRED;
        this.endedAt = LocalDateTime.now();
    }

    /**
     * 재생 시간 업데이트
     */
    public void updatePlayedSeconds(Integer seconds) {
        if (seconds > 0) {
            this.totalPlayedSeconds = (this.totalPlayedSeconds != null ? this.totalPlayedSeconds : 0) + seconds;
        }
    }

    /**
     * 타이머가 설정되어 있는지 확인
     */
    public boolean hasTimer() {
        return this.timerMinutes != null && this.timerMinutes > 0 && this.timerExpiresAt != null;
    }

    /**
     * 타이머가 만료되었는지 확인
     */
    public boolean isTimerExpired() {
        return hasTimer() && LocalDateTime.now().isAfter(this.timerExpiresAt);
    }

    /**
     * 페이드아웃 시작 시간인지 확인
     */
    public boolean shouldStartFadeOut() {
        if (!hasTimer() || !fadeOutEnabled) {
            return false;
        }
        LocalDateTime fadeOutStartTime = this.timerExpiresAt.minusSeconds(fadeOutDurationSeconds);
        return LocalDateTime.now().isAfter(fadeOutStartTime) &&
               this.status == ASMRSessionStatus.ACTIVE;
    }

    /**
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return this.status == ASMRSessionStatus.ACTIVE;
    }

    /**
     * 세션이 진행 중인지 확인 (ACTIVE 또는 PAUSED)
     */
    public boolean isOngoing() {
        return this.status == ASMRSessionStatus.ACTIVE || this.status == ASMRSessionStatus.PAUSED;
    }
}