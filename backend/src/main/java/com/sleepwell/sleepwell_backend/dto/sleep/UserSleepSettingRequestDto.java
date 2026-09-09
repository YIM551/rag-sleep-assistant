package com.sleepwell.sleepwell_backend.dto.sleep;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 사용자 수면 설정 요청 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSleepSettingRequestDto {

    /**
     * 수면 알림을 몇 분 전에 받을지 (15분~180분)
     */
    @Min(value = 15, message = "알림 시간은 최소 15분 이상이어야 합니다.")
    @Max(value = 180, message = "알림 시간은 최대 180분(3시간) 이하여야 합니다.")
    private Integer sleepReminderMinutesBefore;

    /**
     * 스마트폰 사용을 줄이기 시작하는 시간
     */
    private LocalTime phoneInactiveStartTime;

    /**
     * 스마트폰 사용을 다시 시작하는 시간
     */
    private LocalTime phoneInactiveEndTime;

    /**
     * 주말에 다른 스케줄을 사용할지 여부
     */
    private Boolean weekendDifferentSchedule;

    /**
     * 주말 취침 시간
     */
    private LocalTime weekendBedtime;

    /**
     * 주말 기상 시간
     */
    private LocalTime weekendWakeupTime;

    /**
     * 스마트 타이밍 기능 활성화 여부
     */
    private Boolean smartTimingEnabled;

    /**
     * 수면 준비 알림 활성화 여부
     */
    private Boolean sleepReminderEnabled;

    /**
     * 기상 알림 활성화 여부
     */
    private Boolean wakeupReminderEnabled;

    /**
     * 알림 톤/사운드 설정
     */
    @Size(max = 50, message = "알림 사운드 이름은 50자 이하여야 합니다.")
    private String notificationSound;
}