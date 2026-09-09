package com.sleepwell.sleepwell_backend.enums;

/**
 * 수면 단계 타입
 *
 * 수면 중 발생하는 다양한 수면 단계를 정의합니다.
 * 각 단계는 특정한 뇌파 패턴과 생리적 특성을 가집니다.
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
public enum SleepStageType {
    /**
     * 깊은 수면 (Deep Sleep / Slow-Wave Sleep)
     * - 가장 회복력이 높은 수면 단계
     * - 신체 회복, 면역 강화, 기억 강화
     * - 델타파 뇌파가 지배적
     */
    DEEP("깊은 수면"),

    /**
     * 얕은 수면 (Light Sleep)
     * - 수면과 각성 사이의 전환 단계
     * - N1, N2 단계를 포함
     * - 쉽게 깨어날 수 있는 상태
     */
    LIGHT("얕은 수면"),

    /**
     * REM 수면 (Rapid Eye Movement)
     * - 빠른 안구 운동이 특징
     * - 꿈을 꾸는 단계
     * - 학습과 정서적 기억 처리
     */
    REM("렘수면"),

    /**
     * 각성 상태 (Awake)
     * - 수면 중 깨어있는 상태
     * - 중간 각성 포함
     */
    AWAKE("각성"),

    /**
     * 알 수 없음 (Unknown)
     * - 측정되지 않거나 분류할 수 없는 단계
     */
    UNKNOWN("알 수 없음");

    private final String description;

    SleepStageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
