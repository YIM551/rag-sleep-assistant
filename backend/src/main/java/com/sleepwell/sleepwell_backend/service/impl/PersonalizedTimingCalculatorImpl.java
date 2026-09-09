package com.sleepwell.sleepwell_backend.service.impl;

import com.sleepwell.sleepwell_backend.entity.UserSleepSetting;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.UserSleepSettingRepository;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.service.PersonalizedTimingCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * 개인화된 타이밍 계산 서비스 구현체
 * 
 * 통계적 알고리즘과 규칙 기반 로직을 사용하여 
 * 사용자별 최적의 알림 시간을 계산합니다.
 * 
 * 주요 계산 로직:
 * - 수면 기록 기반 패턴 분석
 * - 알림 반응률 통계
 * - 스마트폰 비활성 시간 고려
 * - 평일/주말 패턴 구분
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalizedTimingCalculatorImpl implements PersonalizedTimingCalculator {

    private final UserSleepSettingRepository userSleepSettingRepository;
    private final SleepRecordRepository sleepRecordRepository;

    // 상수 정의
    private static final int MIN_DATA_POINTS = 7; // 최소 7일 데이터 필요
    private static final int ANALYSIS_PERIOD_DAYS = 30; // 최근 30일 데이터 분석
    private static final int MIN_REMINDER_MINUTES = 15; // 최소 15분 전 알림
    private static final int MAX_REMINDER_MINUTES = 180; // 최대 3시간 전 알림
    private static final double MIN_RESPONSE_RATE = 0.3; // 최소 30% 반응률

    @Override
    public Optional<LocalTime> calculateOptimalSleepReminderTime(UserSleepSetting setting, boolean isWeekend) {
        log.debug("사용자 {}의 최적 수면 알림 시간 계산 (주말: {})", setting.getUser().getId(), isWeekend);
        
        if (!setting.getSmartTimingEnabled()) {
            log.debug("스마트 타이밍이 비활성화됨");
            return Optional.empty();
        }
        
        // 최근 수면 기록 분석
        List<SleepRecord> recentRecords = getRecentSleepRecords(setting.getUser().getId(), isWeekend);
        
        if (recentRecords.size() < MIN_DATA_POINTS) {
            log.debug("충분한 데이터가 없음. 현재 {}개 기록", recentRecords.size());
            throw new BusinessException(ErrorCode.INSUFFICIENT_SLEEP_DATA_FOR_ANALYSIS, 
                String.format("스마트 타이밍 분석에는 최소 %d일의 수면 기록이 필요합니다. 현재: %d개", 
                    MIN_DATA_POINTS, recentRecords.size()));
        }
        
        // 평균 취침 시간 계산
        OptionalDouble averageBedtime = recentRecords.stream()
            .filter(record -> record.getSleepStartTime() != null)
            .mapToDouble(record -> timeToMinutes(record.getSleepStartTime().toLocalTime()))
            .average();
        
        if (averageBedtime.isEmpty()) {
            log.debug("유효한 취침 시간 데이터가 없음");
            return Optional.empty();
        }
        
        // 최적 알림 시간 계산
        double avgMinutes = averageBedtime.getAsDouble();
        LocalTime averageBedtimeLocal = minutesToTime((int) avgMinutes);
        
        // 효과적인 알림 분수 적용
        int effectiveMinutes = setting.getEffectiveReminderMinutes();
        
        // 스마트폰 비활성 시간 고려
        LocalTime calculatedTime = averageBedtimeLocal.minusMinutes(effectiveMinutes);
        LocalTime adjustedTime = adjustForPhoneInactiveTime(setting, calculatedTime);
        
        // 유효성 검증
        if (!validateCalculatedTiming(setting, adjustedTime)) {
            log.debug("계산된 시간이 유효하지 않음: {}", adjustedTime);
            return Optional.empty();
        }
        
        log.debug("계산된 최적 수면 알림 시간: {}", adjustedTime);
        return Optional.of(adjustedTime);
    }

    @Override
    public Optional<LocalTime> calculateOptimalWakeUpReminderTime(UserSleepSetting setting, boolean isWeekend) {
        log.debug("사용자 {}의 최적 기상 알림 시간 계산 (주말: {})", setting.getUser().getId(), isWeekend);
        
        if (!setting.getSmartTimingEnabled()) {
            return Optional.empty();
        }
        
        // 최근 수면 기록에서 기상 시간 패턴 분석
        List<SleepRecord> recentRecords = getRecentSleepRecords(setting.getUser().getId(), isWeekend);
        
        if (recentRecords.size() < MIN_DATA_POINTS) {
            return Optional.empty();
        }
        
        // 평균 기상 시간 계산
        OptionalDouble averageWakeTime = recentRecords.stream()
            .filter(record -> record.getSleepEndTime() != null)
            .mapToDouble(record -> timeToMinutes(record.getSleepEndTime().toLocalTime()))
            .average();
        
        if (averageWakeTime.isEmpty()) {
            return Optional.empty();
        }
        
        LocalTime calculatedWakeTime = minutesToTime((int) averageWakeTime.getAsDouble());
        
        // 기상 알림은 정확한 시간에 설정 (수면과 달리 미리 알릴 필요 없음)
        if (!validateCalculatedTiming(setting, calculatedWakeTime)) {
            return Optional.empty();
        }
        
        log.debug("계산된 최적 기상 알림 시간: {}", calculatedWakeTime);
        return Optional.of(calculatedWakeTime);
    }

    @Override
    public LocalTime adjustForPhoneInactiveTime(UserSleepSetting setting, LocalTime targetTime) {
        if (setting.getPhoneInactiveStartTime() == null || setting.getPhoneInactiveEndTime() == null) {
            return targetTime;
        }
        
        // 폰 비활성 시간대인지 확인
        if (setting.isPhoneInactiveTime(targetTime)) {
            log.debug("알림 시간 {}이 폰 비활성 시간대에 포함됨", targetTime);
            
            // 폰 비활성 시작 시간 15분 전으로 조정
            LocalTime adjustedTime = setting.getPhoneInactiveStartTime().minusMinutes(15);
            
            // 너무 이른 시간인지 확인 (오후 6시 이전은 너무 이름)
            if (adjustedTime.isBefore(LocalTime.of(18, 0))) {
                // 폰 비활성 종료 후 15분 뒤로 설정
                adjustedTime = setting.getPhoneInactiveEndTime().plusMinutes(15);
            }
            
            log.debug("폰 비활성 시간 고려하여 {}로 조정", adjustedTime);
            return adjustedTime;
        }
        
        return targetTime;
    }

    @Override
    public TimingAnalysisResult analyzeUserNotificationPattern(Long userId) {
        log.debug("사용자 {}의 알림 반응 패턴 분석", userId);
        
        UserSleepSetting setting = userSleepSettingRepository.findByUserIdWithUser(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SETTING_NOT_FOUND));
        
        // 분석 결과 생성 (실제로는 알림 로그 테이블에서 데이터를 가져와야 함)
        // 현재는 설정에서 기본 정보만 사용
        
        int totalNotifications = Math.max(1, setting.getConsecutiveIgnoredNotifications() + 10);
        int responseCount = Math.max(1, totalNotifications - setting.getConsecutiveIgnoredNotifications());
        double responseRate = (double) responseCount / totalNotifications;
        
        LocalTime mostEffectiveTime = setting.getBedtimeReminderTime();
        LocalTime averageResponseTime = mostEffectiveTime != null ? mostEffectiveTime : LocalTime.of(22, 0);
        boolean hasEnoughData = totalNotifications >= MIN_DATA_POINTS;
        
        return new TimingAnalysisResult(
            mostEffectiveTime,
            responseRate,
            totalNotifications,
            responseCount,
            averageResponseTime,
            hasEnoughData
        );
    }

    @Override
    public Optional<Integer> recommendOptimalReminderMinutes(Long userId) {
        log.debug("사용자 {}의 최적 알림 분수 추천", userId);
        
        // 수면 기록 기반 분석
        List<SleepRecord> recentRecords = getRecentSleepRecords(userId, false);
        
        if (recentRecords.size() < MIN_DATA_POINTS) {
            return Optional.empty();
        }
        
        // 수면의 질 점수 기반 최적 알림 시간 계산
        OptionalDouble averageQuality = recentRecords.stream()
            .filter(record -> record.getSleepQualityScore() != null)
            .mapToInt(SleepRecord::getSleepQualityScore)
            .average();
        
        if (averageQuality.isEmpty()) {
            return Optional.empty();
        }
        
        // 수면의 질에 따른 알림 시간 조정
        double quality = averageQuality.getAsDouble();
        int recommendedMinutes;
        
        if (quality >= 80) {
            // 수면의 질이 좋으면 짧은 준비 시간
            recommendedMinutes = 30;
        } else if (quality >= 60) {
            // 보통 수면의 질
            recommendedMinutes = 60;
        } else {
            // 수면의 질이 나쁘면 긴 준비 시간
            recommendedMinutes = 90;
        }
        
        // 범위 내로 제한
        recommendedMinutes = Math.max(MIN_REMINDER_MINUTES, 
                             Math.min(MAX_REMINDER_MINUTES, recommendedMinutes));
        
        log.debug("추천 알림 분수: {}분 (수면 질 점수: {})", recommendedMinutes, quality);
        return Optional.of(recommendedMinutes);
    }

    @Override
    @Transactional
    public void recordUserResponse(Long userId, LocalTime reminderTime, boolean responded, LocalTime actualSleepTime) {
        log.debug("사용자 {} 알림 반응 기록: 시간={}, 반응={}", userId, reminderTime, responded);
        
        UserSleepSetting setting = userSleepSettingRepository.findByUserIdWithUser(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SETTING_NOT_FOUND));
        
        if (responded && actualSleepTime != null) {
            // 효과적인 알림 시간 계산
            long minutesBetween = ChronoUnit.MINUTES.between(reminderTime, actualSleepTime);
            int effectiveMinutes = (int) Math.abs(minutesBetween);
            
            // 설정 업데이트
            setting.resetIgnoredNotificationsAndLearn(effectiveMinutes);
            
            log.debug("효과적인 알림 시간 학습: {}분", effectiveMinutes);
        } else {
            // 무시한 경우
            setting.incrementIgnoredNotifications();
            log.debug("알림 무시 횟수 증가: {}회", setting.getConsecutiveIgnoredNotifications());
        }
        
        userSleepSettingRepository.save(setting);
    }

    @Override
    @Transactional
    public void recalculateAllUserTimings() {
        log.info("모든 사용자의 스마트 타이밍 재계산 시작");
        
        List<UserSleepSetting> smartTimingUsers = userSleepSettingRepository.findAllWithSmartTimingEnabled();
        
        for (UserSleepSetting setting : smartTimingUsers) {
            try {
                recalculateUserTiming(setting.getUser().getId());
            } catch (Exception e) {
                log.error("사용자 {}의 타이밍 재계산 실패", setting.getUser().getId(), e);
            }
        }
        
        log.info("모든 사용자의 스마트 타이밍 재계산 완료. 대상: {}명", smartTimingUsers.size());
    }

    @Override
    @Transactional
    public void recalculateUserTiming(Long userId) {
        log.debug("사용자 {}의 스마트 타이밍 재계산", userId);
        
        UserSleepSetting setting = userSleepSettingRepository.findByUserIdWithUser(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SETTING_NOT_FOUND));
        
        if (!setting.getSmartTimingEnabled()) {
            log.debug("스마트 타이밍이 비활성화됨");
            return;
        }
        
        // 최적 알림 분수 재계산
        Optional<Integer> recommendedMinutes = recommendOptimalReminderMinutes(userId);
        
        if (recommendedMinutes.isPresent()) {
            // 현재 설정 업데이트 (유효한 추천이 있는 경우만)
            setting.updateSettings(
                recommendedMinutes.get(),
                setting.getPhoneInactiveStartTime(),
                setting.getPhoneInactiveEndTime(),
                setting.getWeekendDifferentSchedule(),
                setting.getWeekendBedtime(),
                setting.getWeekendWakeupTime(),
                setting.getSmartTimingEnabled(),
                setting.getSleepReminderEnabled(),
                setting.getWakeupReminderEnabled(),
                setting.getNotificationSound()
            );
            
            userSleepSettingRepository.save(setting);
            log.debug("사용자 {}의 타이밍 업데이트 완료: {}분", userId, recommendedMinutes.get());
        }
    }

    @Override
    public boolean validateCalculatedTiming(UserSleepSetting setting, LocalTime calculatedTime) {
        if (calculatedTime == null) {
            return false;
        }
        
        // 너무 이른 시간 (오후 6시 이전) 또는 너무 늦은 시간 (새벽 2시 이후) 제외
        if (calculatedTime.isBefore(LocalTime.of(18, 0)) || 
            calculatedTime.isAfter(LocalTime.of(2, 0))) {
            return false;
        }
        
        // 기본 취침 시간과 너무 큰 차이가 나면 제외 (2시간 이상 차이)
        LocalTime baseBedtime = setting.getApplicableBedtime(false);
        if (baseBedtime != null) {
            long hoursDiff = Math.abs(ChronoUnit.HOURS.between(calculatedTime, baseBedtime));
            if (hoursDiff > 2) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public double evaluateTimingEffectiveness(Long userId) {
        TimingAnalysisResult analysis = analyzeUserNotificationPattern(userId);
        
        if (!analysis.hasEnoughData()) {
            return 0.0;
        }
        
        // 반응률 기반 효과성 평가
        double effectiveness = analysis.responseRate();
        
        // 최소 반응률 이하면 0점
        if (effectiveness < MIN_RESPONSE_RATE) {
            return 0.0;
        }
        
        // 0.0 ~ 1.0 범위로 정규화
        return Math.min(1.0, effectiveness);
    }

    // === 내부 헬퍼 메서드들 ===

    private List<SleepRecord> getRecentSleepRecords(Long userId, boolean isWeekend) {
        LocalDateTime since = LocalDateTime.now().minusDays(ANALYSIS_PERIOD_DAYS);
        List<SleepRecord> allRecords = sleepRecordRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, since);
        
        // 평일/주말 필터링
        return allRecords.stream()
            .filter(record -> {
                if (record.getSleepStartTime() == null) return false;
                DayOfWeek dayOfWeek = record.getSleepStartTime().getDayOfWeek();
                boolean recordIsWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
                return recordIsWeekend == isWeekend;
            })
            .limit(MIN_DATA_POINTS * 2) // 충분한 데이터 확보
            .toList();
    }

    private int timeToMinutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    private LocalTime minutesToTime(int minutes) {
        // 24시간을 넘어가는 경우 처리
        minutes = minutes % (24 * 60);
        if (minutes < 0) {
            minutes += 24 * 60;
        }
        return LocalTime.of(minutes / 60, minutes % 60);
    }
}