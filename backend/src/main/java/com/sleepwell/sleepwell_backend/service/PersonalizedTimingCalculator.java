package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.entity.UserSleepSetting;
import java.time.LocalTime;
import java.util.Optional;

/**
 * 개인화된 타이밍 계산 서비스 인터페이스
 * 
 * 사용자의 수면 패턴과 스마트폰 사용 패턴을 분석하여
 * 최적의 알림 시간을 계산하는 서비스입니다.
 * 
 * 실제 AI/ML이 아닌 통계적 알고리즘과 규칙 기반 로직을 사용하여
 * 사용자별 맞춤 알림 타이밍을 제공합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public interface PersonalizedTimingCalculator {

    // === 메인 계산 메서드들 ===

    /**
     * 사용자의 최적 수면 알림 시간을 계산합니다.
     * 
     * @param setting 사용자 수면 설정
     * @param isWeekend 주말 여부
     * @return 계산된 최적 알림 시간 (없으면 기본 설정 사용)
     */
    Optional<LocalTime> calculateOptimalSleepReminderTime(UserSleepSetting setting, boolean isWeekend);

    /**
     * 사용자의 최적 기상 알림 시간을 계산합니다.
     * 
     * @param setting 사용자 수면 설정
     * @param isWeekend 주말 여부
     * @return 계산된 최적 기상 알림 시간 (없으면 기본 설정 사용)
     */
    Optional<LocalTime> calculateOptimalWakeUpReminderTime(UserSleepSetting setting, boolean isWeekend);

    /**
     * 사용자의 스마트폰 비활성 시간을 고려한 알림 시간을 계산합니다.
     * 
     * @param setting 사용자 수면 설정
     * @param targetTime 목표 알림 시간
     * @return 조정된 알림 시간
     */
    LocalTime adjustForPhoneInactiveTime(UserSleepSetting setting, LocalTime targetTime);

    // === 패턴 분석 메서드들 ===

    /**
     * 사용자의 알림 반응 패턴을 분석합니다.
     * 
     * @param userId 사용자 ID
     * @return 분석된 패턴 정보
     */
    TimingAnalysisResult analyzeUserNotificationPattern(Long userId);

    /**
     * 사용자의 수면 기록을 기반으로 최적 알림 시간을 추천합니다.
     * 
     * @param userId 사용자 ID
     * @return 추천 알림 시간 (분 단위)
     */
    Optional<Integer> recommendOptimalReminderMinutes(Long userId);

    // === 학습 및 업데이트 메서드들 ===

    /**
     * 사용자가 알림에 반응했을 때 학습 데이터를 업데이트합니다.
     * 
     * @param userId 사용자 ID
     * @param reminderTime 알림 시간
     * @param responded 알림에 반응했는지 여부
     * @param actualSleepTime 실제 잠든 시간 (선택적)
     */
    void recordUserResponse(Long userId, LocalTime reminderTime, boolean responded, LocalTime actualSleepTime);

    /**
     * 모든 사용자의 스마트 타이밍 데이터를 재계산합니다.
     */
    void recalculateAllUserTimings();

    /**
     * 특정 사용자의 스마트 타이밍 데이터를 재계산합니다.
     * 
     * @param userId 사용자 ID
     */
    void recalculateUserTiming(Long userId);

    // === 통계 및 검증 메서드들 ===

    /**
     * 계산된 타이밍의 유효성을 검증합니다.
     * 
     * @param setting 사용자 설정
     * @param calculatedTime 계산된 시간
     * @return 유효성 검증 결과
     */
    boolean validateCalculatedTiming(UserSleepSetting setting, LocalTime calculatedTime);

    /**
     * 사용자의 스마트 타이밍 효과성을 평가합니다.
     * 
     * @param userId 사용자 ID
     * @return 효과성 점수 (0.0 ~ 1.0)
     */
    double evaluateTimingEffectiveness(Long userId);

    /**
     * 타이밍 분석 결과 데이터 클래스
     */
    record TimingAnalysisResult(
        LocalTime mostEffectiveTime,
        double responseRate,
        int totalNotifications,
        int responseCount,
        LocalTime averageResponseTime,
        boolean hasEnoughData
    ) {}
}