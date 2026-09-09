package com.sleepwell.sleepwell_backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ASMR 재생 세션 상태
 */
@Getter
@RequiredArgsConstructor
public enum ASMRSessionStatus {

    /**
     * 활성 상태 - 재생 중
     */
    ACTIVE("활성", "재생 중"),

    /**
     * 일시 정지 상태
     */
    PAUSED("일시정지", "일시 정지됨"),

    /**
     * 중지 상태 - 사용자가 직접 중지
     */
    STOPPED("중지", "사용자가 중지함"),

    /**
     * 타이머 만료로 자동 종료
     */
    TIMER_EXPIRED("타이머만료", "타이머로 자동 종료됨"),

    /**
     * 페이드아웃 진행 중
     */
    FADING_OUT("페이드아웃", "볼륨 감소 중"),

    /**
     * 오류로 인한 중단
     */
    ERROR("오류", "오류로 인해 중단됨");

    private final String koreanName;
    private final String description;

    /**
     * 세션이 진행 중인 상태인지 확인
     */
    public boolean isOngoing() {
        return this == ACTIVE || this == PAUSED || this == FADING_OUT;
    }

    /**
     * 세션이 종료된 상태인지 확인
     */
    public boolean isTerminated() {
        return this == STOPPED || this == TIMER_EXPIRED || this == ERROR;
    }

    /**
     * 타이머에 의해 제어 가능한 상태인지 확인
     */
    public boolean isTimerControllable() {
        return this == ACTIVE || this == PAUSED;
    }
}