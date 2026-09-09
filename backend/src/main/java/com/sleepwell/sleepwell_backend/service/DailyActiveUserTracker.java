package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.event.UserLoginEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 일일 활성 사용자 (DAU) 추적
 *
 * JWT 자동 인증을 포함한 모든 접속을 추적하되,
 * 하루에 한 번만 카운트하여 정확한 DAU를 측정합니다.
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyActiveUserTracker {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 오늘 이미 추적된 사용자 캐시
     * Key: "YYYY-MM-DD:userId"
     * Value: true
     */
    private final ConcurrentHashMap<String, Boolean> todayTracked = new ConcurrentHashMap<>();

    /**
     * 오늘 첫 접속인 경우 로그인 이벤트 발행
     *
     * @param userId 사용자 ID
     * @param userEmail 사용자 이메일
     */
    public void trackIfFirstToday(Long userId, String userEmail) {
        if (userId == null) {
            return;
        }

        String today = LocalDate.now().toString();
        String key = today + ":" + userId;

        // ✅ 오늘 첫 요청이면 이벤트 발행 (ConcurrentHashMap의 원자적 연산)
        if (todayTracked.putIfAbsent(key, Boolean.TRUE) == null) {
            try {
                log.debug("일일 활성 사용자 추적 - userId: {}, date: {}", userId, today);

                UserLoginEvent loginEvent = new UserLoginEvent(
                        userId,
                        userEmail,
                        null,  // sessionId (JWT 자동 인증이라 없음)
                        null,  // ipAddress (필터에서 가져오기 어려움)
                        null,  // userAgent
                        "AUTO"  // 자동 로그인 표시
                );

                eventPublisher.publishEvent(loginEvent);
            } catch (Exception e) {
                log.warn("DAU 추적 이벤트 발행 실패: {}", e.getMessage());
            }
        }
    }

    /**
     * 매일 자정에 캐시 초기화
     * 메모리 누수 방지 및 새로운 날의 추적 준비
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void clearDailyCache() {
        int size = todayTracked.size();
        todayTracked.clear();
        log.info("DAU 캐시 초기화 완료 - 어제 활성 사용자: {}명", size);
    }

    /**
     * 현재 캐시된 오늘의 활성 사용자 수 조회 (모니터링용)
     *
     * @return 오늘 활성 사용자 수
     */
    public int getTodayActiveUserCount() {
        return todayTracked.size();
    }
}
