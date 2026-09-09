package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.sleep.UserSleepSettingDto;
import com.sleepwell.sleepwell_backend.dto.sleep.UserSleepSettingRequestDto;

import java.util.List;

/**
 * 사용자 수면 설정 서비스 인터페이스
 * 
 * 개인화된 수면 알림 및 스케줄링을 위한 설정 관리 서비스입니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public interface UserSleepSettingService {

    /**
     * 사용자의 수면 설정 조회
     * 설정이 없으면 기본 설정으로 자동 생성
     * 
     * @param userId 사용자 ID
     * @return 사용자 수면 설정 DTO
     */
    UserSleepSettingDto getUserSleepSetting(Long userId);

    /**
     * 사용자의 수면 설정 생성
     * 
     * @param userId 사용자 ID
     * @param requestDto 설정 요청 DTO
     * @return 생성된 설정 DTO
     */
    UserSleepSettingDto createUserSleepSetting(Long userId, UserSleepSettingRequestDto requestDto);

    /**
     * 사용자의 수면 설정 업데이트
     * 
     * @param userId 사용자 ID
     * @param requestDto 설정 요청 DTO
     * @return 업데이트된 설정 DTO
     */
    UserSleepSettingDto updateUserSleepSetting(Long userId, UserSleepSettingRequestDto requestDto);

    /**
     * 사용자의 수면 설정 삭제 (기본값으로 리셋)
     * 
     * @param userId 사용자 ID
     */
    void deleteUserSleepSetting(Long userId);

    /**
     * 수면 알림이 활성화된 모든 사용자 설정 조회
     * 스케줄러에서 사용
     * 
     * @return 알림 활성화된 사용자들의 설정 목록
     */
    List<UserSleepSettingDto> getAllUsersWithSleepReminderEnabled();

    /**
     * 스마트 타이밍이 활성화된 사용자 설정 조회
     * AI 최적화에 사용
     * 
     * @return 스마트 타이밍 사용자들의 설정 목록
     */
    List<UserSleepSettingDto> getAllUsersWithSmartTimingEnabled();

    /**
     * 알림 무시 횟수 증가
     * 스마트 타이밍 학습용
     * 
     * @param userId 사용자 ID
     */
    void incrementIgnoredNotifications(Long userId);

    /**
     * 알림 응답 시 학습 데이터 업데이트
     * 스마트 타이밍 학습용
     * 
     * @param userId 사용자 ID
     * @param effectiveMinutes 효과적이었던 알림 시간 (분)
     */
    void recordEffectiveNotificationTiming(Long userId, Integer effectiveMinutes);

    /**
     * 사용자가 기본 설정을 사용하는지 확인
     * 
     * @param userId 사용자 ID
     * @return 기본 설정 사용 여부
     */
    boolean isUsingDefaultSettings(Long userId);

    /**
     * 사용자 설정이 존재하는지 확인
     * 
     * @param userId 사용자 ID
     * @return 설정 존재 여부
     */
    boolean userSettingExists(Long userId);
}