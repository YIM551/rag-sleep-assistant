package com.sleepwell.sleepwell_backend.service.impl;

import com.sleepwell.sleepwell_backend.dto.sleep.UserSleepSettingDto;
import com.sleepwell.sleepwell_backend.dto.sleep.UserSleepSettingRequestDto;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.entity.UserSleepSetting;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.repository.UserSleepSettingRepository;
import com.sleepwell.sleepwell_backend.service.UserSleepSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 수면 설정 서비스 구현체
 * 
 * 개인화된 수면 알림 및 스케줄링을 위한 사용자별 설정 관리를 담당합니다.
 * 스마트 타이밍 학습, 알림 무시 패턴 분석, 동적 스케줄링 등의 기능을 제공합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSleepSettingServiceImpl implements UserSleepSettingService {

    private final UserSleepSettingRepository userSleepSettingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserSleepSettingDto getUserSleepSetting(Long userId) {
        log.debug("사용자 수면 설정 조회: userId = {}", userId);
        
        // User와 함께 조회 (LazyInitializationException 방지)
        UserSleepSetting setting = userSleepSettingRepository.findByUserIdWithUser(userId)
                .orElse(null);
        
        if (setting == null) {
            log.info("수면 설정이 없음. 기본 설정 생성: userId = {}", userId);
            setting = createDefaultSetting(userId);
            // 기본 설정 생성 후 User와 함께 다시 조회
            setting = userSleepSettingRepository.findByUserIdWithUser(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SETTING_NOT_FOUND));
        }
        
        return new UserSleepSettingDto(setting);
    }

    @Override
    @Transactional
    public UserSleepSettingDto createUserSleepSetting(Long userId, UserSleepSettingRequestDto requestDto) {
        log.info("사용자 수면 설정 생성 시도: userId = {}", userId);
        
        // 중복 설정 확인
        if (userSleepSettingRepository.existsByUserId(userId)) {
            log.warn("이미 존재하는 수면 설정에 대한 생성 시도: userId = {}", userId);
            throw new BusinessException(ErrorCode.DUPLICATE_SLEEP_SETTING);
        }
        
        User user = findUserById(userId);
        UserSleepSetting setting = buildUserSleepSetting(user, requestDto);
        
        UserSleepSetting savedSetting = userSleepSettingRepository.save(setting);
        log.info("사용자 수면 설정 생성 완료: userId = {}, settingId = {}", userId, savedSetting.getId());
        
        return new UserSleepSettingDto(savedSetting);
    }

    @Override
    @Transactional
    public UserSleepSettingDto updateUserSleepSetting(Long userId, UserSleepSettingRequestDto requestDto) {
        log.info("사용자 수면 설정 업데이트: userId = {}", userId);
        
        UserSleepSetting setting = userSleepSettingRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SETTING_NOT_FOUND));
        
        // 설정 업데이트
        setting.updateSettings(
                requestDto.getSleepReminderMinutesBefore(),
                requestDto.getPhoneInactiveStartTime(),
                requestDto.getPhoneInactiveEndTime(),
                requestDto.getWeekendDifferentSchedule(),
                requestDto.getWeekendBedtime(),
                requestDto.getWeekendWakeupTime(),
                requestDto.getSmartTimingEnabled(),
                requestDto.getSleepReminderEnabled(),
                requestDto.getWakeupReminderEnabled(),
                requestDto.getNotificationSound()
        );
        
        UserSleepSetting updatedSetting = userSleepSettingRepository.save(setting);
        log.info("사용자 수면 설정 업데이트 완료: userId = {}, settingId = {}", userId, updatedSetting.getId());
        
        return new UserSleepSettingDto(updatedSetting);
    }

    @Override
    @Transactional
    public void deleteUserSleepSetting(Long userId) {
        log.info("사용자 수면 설정 삭제: userId = {}", userId);
        
        if (!userSleepSettingRepository.existsByUserId(userId)) {
            log.warn("존재하지 않는 수면 설정 삭제 시도: userId = {}", userId);
            throw new BusinessException(ErrorCode.SLEEP_SETTING_NOT_FOUND);
        }
        
        userSleepSettingRepository.deleteByUserId(userId);
        log.info("사용자 수면 설정 삭제 완료: userId = {}", userId);
    }

    @Override
    public List<UserSleepSettingDto> getAllUsersWithSleepReminderEnabled() {
        log.debug("수면 알림 활성화 사용자 설정 조회");
        
        List<UserSleepSetting> settings = userSleepSettingRepository.findAllWithSleepReminderEnabled();
        
        return settings.stream()
                .map(UserSleepSettingDto::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserSleepSettingDto> getAllUsersWithSmartTimingEnabled() {
        log.debug("스마트 타이밍 활성화 사용자 설정 조회");
        
        List<UserSleepSetting> settings = userSleepSettingRepository.findAllWithSmartTimingEnabled();
        
        return settings.stream()
                .map(UserSleepSettingDto::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void incrementIgnoredNotifications(Long userId) {
        log.debug("알림 무시 횟수 증가: userId = {}", userId);
        
        UserSleepSetting setting = userSleepSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SETTING_NOT_FOUND));
        
        setting.incrementIgnoredNotifications();
        userSleepSettingRepository.save(setting);
        
        log.info("알림 무시 횟수 증가 완료: userId = {}, 현재 횟수 = {}", 
                userId, setting.getConsecutiveIgnoredNotifications());
    }

    @Override
    @Transactional
    public void recordEffectiveNotificationTiming(Long userId, Integer effectiveMinutes) {
        log.info("효과적인 알림 시간 기록: userId = {}, effectiveMinutes = {}", userId, effectiveMinutes);
        
        UserSleepSetting setting = userSleepSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SETTING_NOT_FOUND));
        
        setting.resetIgnoredNotificationsAndLearn(effectiveMinutes);
        userSleepSettingRepository.save(setting);
        
        log.info("효과적인 알림 시간 기록 완료: userId = {}, 학습된 시간 = {}분", userId, effectiveMinutes);
    }

    @Override
    public boolean isUsingDefaultSettings(Long userId) {
        UserSleepSetting setting = userSleepSettingRepository.findByUserId(userId).orElse(null);
        
        if (setting == null) {
            return true; // 설정이 없으면 기본값 사용중
        }
        
        // 기본값과 비교
        return setting.getSleepReminderMinutesBefore().equals(60) &&
               !setting.getSmartTimingEnabled() &&
               setting.getPhoneInactiveStartTime() == null &&
               !setting.getWeekendDifferentSchedule();
    }

    @Override
    public boolean userSettingExists(Long userId) {
        return userSleepSettingRepository.existsByUserId(userId);
    }

    // === 내부 헬퍼 메서드 ===

    /**
     * 기본 설정 생성
     */
    @Transactional(readOnly = false, propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected UserSleepSetting createDefaultSetting(Long userId) {
        log.info("기본 수면 설정 생성: userId = {}", userId);
        
        try {
            User user = findUserById(userId);
            log.info("사용자 조회 성공: userId = {}", userId);
            
            // @Builder.Default 충돌 방지를 위해 명시적으로 모든 값 설정
            UserSleepSetting defaultSetting = UserSleepSetting.builder()
                    .user(user)
                    .sleepReminderMinutesBefore(60)
                    .phoneInactiveStartTime(null)
                    .phoneInactiveEndTime(null)
                    .weekendDifferentSchedule(false)
                    .weekendBedtime(null)
                    .weekendWakeupTime(null)
                    .smartTimingEnabled(false)
                    .consecutiveIgnoredNotifications(0)
                    .mostEffectiveReminderMinutes(null)
                    .sleepReminderEnabled(true)
                    .wakeupReminderEnabled(true)
                    .notificationSound("default")
                    .build();
            
            log.info("기본 설정 객체 생성 완료");
            
            UserSleepSetting savedSetting = userSleepSettingRepository.save(defaultSetting);
            log.info("기본 수면 설정 저장 완료: userId = {}, settingId = {}", userId, savedSetting.getId());
            
            return savedSetting;
        } catch (Exception e) {
            log.error("기본 설정 생성 중 오류: userId = {}, error = {}", userId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 요청 DTO로부터 UserSleepSetting 엔티티 생성
     */
    private UserSleepSetting buildUserSleepSetting(User user, UserSleepSettingRequestDto requestDto) {
        return UserSleepSetting.builder()
                .user(user)
                .sleepReminderMinutesBefore(requestDto.getSleepReminderMinutesBefore() != null ? 
                        requestDto.getSleepReminderMinutesBefore() : 60)
                .phoneInactiveStartTime(requestDto.getPhoneInactiveStartTime())
                .phoneInactiveEndTime(requestDto.getPhoneInactiveEndTime())
                .weekendDifferentSchedule(requestDto.getWeekendDifferentSchedule() != null ? 
                        requestDto.getWeekendDifferentSchedule() : false)
                .weekendBedtime(requestDto.getWeekendBedtime())
                .weekendWakeupTime(requestDto.getWeekendWakeupTime())
                .smartTimingEnabled(requestDto.getSmartTimingEnabled() != null ? 
                        requestDto.getSmartTimingEnabled() : false)
                .sleepReminderEnabled(requestDto.getSleepReminderEnabled() != null ? 
                        requestDto.getSleepReminderEnabled() : true)
                .wakeupReminderEnabled(requestDto.getWakeupReminderEnabled() != null ? 
                        requestDto.getWakeupReminderEnabled() : true)
                .notificationSound(requestDto.getNotificationSound() != null ? 
                        requestDto.getNotificationSound() : "default")
                .consecutiveIgnoredNotifications(0)
                .build();
    }

    /**
     * 사용자 조회 (예외 처리 포함)
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}