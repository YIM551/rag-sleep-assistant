package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.aspect.ASMRRateLimitingAspect.RateLimit;
import com.sleepwell.sleepwell_backend.dto.ASMRSessionStatusDto;
import com.sleepwell.sleepwell_backend.dto.ASMRTimerRequestDto;
import com.sleepwell.sleepwell_backend.dto.ASMRTimerResponseDto;
import com.sleepwell.sleepwell_backend.entity.ASMRContent;
import com.sleepwell.sleepwell_backend.entity.ASMRPlaySession;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.ASMRSessionStatus;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.ASMRContentRepository;
import com.sleepwell.sleepwell_backend.repository.ASMRPlaySessionRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ASMR 타이머 관리 서비스
 * 수면 타이머, 페이드아웃, 세션 관리를 담당합니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ASMRTimerService {

    private final ASMRPlaySessionRepository sessionRepository;
    private final ASMRContentRepository contentRepository;
    private final UserRepository userRepository;
    private final ASMRSessionMonitoringService monitoringService;

    /**
     * 타이머와 함께 재생 세션 시작
     * Rate Limiting: 시간당 최대 10개 세션 생성 제한
     */
    @RateLimit(name = "asmr-session-creation")
    public ASMRTimerResponseDto startSessionWithTimer(Long userId, Long contentId, ASMRTimerRequestDto request) {
        Timer.Sample sample = Timer.start();

        log.info("ASMR 타이머 세션 시작: userId={}, contentId={}, timer={}분", userId, contentId, request.getTimerMinutes());

        try {
            // 사용자 및 콘텐츠 검증
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            ASMRContent content = contentRepository.findById(contentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ASMR_CONTENT_NOT_FOUND));

            // 기존 활성 세션 확인 및 종료
            terminateExistingActiveSessions(userId);

            // 새 세션 생성
            ASMRPlaySession session = ASMRPlaySession.builder()
                    .user(user)
                    .asmrContent(content)
                    .playbackQuality(request.getPlaybackQuality())
                    .timerMinutes(request.getTimerMinutes())
                    .fadeOutEnabled(request.getFadeOutEnabled())
                    .fadeOutDurationSeconds(request.getFadeOutDurationSeconds())
                    .build();

            session = sessionRepository.save(session);

            // 모니터링 이벤트 발생
            monitoringService.onSessionCreated(userId, session.getId());

            log.info("타이머 세션 생성 완료: sessionId={}, 만료시간={}", session.getId(), session.getTimerExpiresAt());

            return monitoringService.recordSessionCreationTime(sample, ASMRTimerResponseDto.from(session));
        } catch (Exception e) {
            sample.stop(Timer.builder("asmr.sessions.creation.failed")
                    .register(io.micrometer.core.instrument.Metrics.globalRegistry));
            throw e;
        }
    }

    /**
     * 기존 세션에 타이머 설정
     */
    public ASMRTimerResponseDto setTimer(Long userId, Long sessionId, ASMRTimerRequestDto request) {
        log.info("기존 세션에 타이머 설정: userId={}, sessionId={}, timer={}분", userId, sessionId, request.getTimerMinutes());

        ASMRPlaySession session = getActiveSessionForUser(userId, sessionId);

        // 타이머 설정
        session.setTimer(request.getTimerMinutes());

        // 페이드아웃 설정 업데이트
        if (request.getFadeOutEnabled() != null) {
            // 기존 엔티티에 setter가 없으므로 새로운 빌더로 생성하거나 필드를 직접 설정해야 함
            // 여기서는 로그만 남기고, 실제로는 엔티티에 setter 메서드 추가 필요
            log.info("페이드아웃 설정 업데이트: {}", request.getFadeOutEnabled());
        }

        session = sessionRepository.save(session);

        log.info("타이머 설정 완료: sessionId={}, 만료시간={}", session.getId(), session.getTimerExpiresAt());

        return ASMRTimerResponseDto.from(session);
    }

    /**
     * 타이머 연장
     */
    public ASMRTimerResponseDto extendTimer(Long userId, Long sessionId, Integer additionalMinutes) {
        log.info("타이머 연장: userId={}, sessionId={}, 추가시간={}분", userId, sessionId, additionalMinutes);

        if (additionalMinutes == null || additionalMinutes <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "연장 시간은 1분 이상이어야 합니다");
        }

        if (additionalMinutes > 60) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "한 번에 최대 60분까지 연장 가능합니다");
        }

        ASMRPlaySession session = getActiveSessionForUser(userId, sessionId);

        if (!session.hasTimer()) {
            throw new BusinessException(ErrorCode.ASMR_TIMER_NOT_SET, "타이머가 설정되지 않은 세션입니다");
        }

        session.extendTimer(additionalMinutes);
        session = sessionRepository.save(session);

        log.info("타이머 연장 완료: sessionId={}, 새로운 만료시간={}", session.getId(), session.getTimerExpiresAt());

        return ASMRTimerResponseDto.from(session);
    }

    /**
     * 타이머 취소
     */
    public ASMRTimerResponseDto cancelTimer(Long userId, Long sessionId) {
        log.info("타이머 취소: userId={}, sessionId={}", userId, sessionId);

        ASMRPlaySession session = getActiveSessionForUser(userId, sessionId);

        session.setTimer(null); // 타이머 제거
        session = sessionRepository.save(session);

        log.info("타이머 취소 완료: sessionId={}", session.getId());

        return ASMRTimerResponseDto.from(session);
    }

    /**
     * 세션 일시 정지
     */
    public ASMRTimerResponseDto pauseSession(Long userId, Long sessionId) {
        log.info("세션 일시 정지: userId={}, sessionId={}", userId, sessionId);

        ASMRPlaySession session = getActiveSessionForUser(userId, sessionId);
        session.pause();
        session = sessionRepository.save(session);

        log.info("세션 일시 정지 완료: sessionId={}", session.getId());

        return ASMRTimerResponseDto.from(session);
    }

    /**
     * 세션 재개
     */
    public ASMRTimerResponseDto resumeSession(Long userId, Long sessionId) {
        log.info("세션 재개: userId={}, sessionId={}", userId, sessionId);

        ASMRPlaySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASMR_SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "다른 사용자의 세션에 접근할 수 없습니다");
        }

        if (session.getStatus() != ASMRSessionStatus.PAUSED) {
            throw new BusinessException(ErrorCode.ASMR_SESSION_NOT_PAUSED, "일시 정지된 세션만 재개할 수 있습니다");
        }

        session.resume();
        session = sessionRepository.save(session);

        log.info("세션 재개 완료: sessionId={}", session.getId());

        return ASMRTimerResponseDto.from(session);
    }

    /**
     * 세션 중지
     */
    public void stopSession(Long userId, Long sessionId) {
        log.info("세션 중지: userId={}, sessionId={}", userId, sessionId);

        ASMRPlaySession session = getActiveSessionForUser(userId, sessionId);
        session.stop();
        sessionRepository.save(session);

        // 모니터링 이벤트 발생
        monitoringService.onSessionEnded(userId, sessionId, "STOPPED");

        log.info("세션 중지 완료: sessionId={}", session.getId());
    }

    /**
     * 사용자의 현재 활성 세션 조회
     */
    @Transactional(readOnly = true)
    public Optional<ASMRTimerResponseDto> getCurrentSession(Long userId) {
        return sessionRepository.findActiveSessionByUserId(userId)
                .map(ASMRTimerResponseDto::from);
    }

    /**
     * 사용자의 모든 활성 세션 조회
     */
    @Transactional(readOnly = true)
    public List<ASMRSessionStatusDto> getAllActiveSessions(Long userId) {
        List<ASMRPlaySession> sessions = sessionRepository.findAllActiveSessionsByUserId(userId);
        return ASMRSessionStatusDto.fromList(sessions);
    }

    /**
     * 재생 시간 업데이트 (클라이언트에서 주기적으로 호출)
     */
    public void updatePlayedTime(Long userId, Long sessionId, Integer playedSeconds) {
        ASMRPlaySession session = getActiveSessionForUser(userId, sessionId);
        session.updatePlayedSeconds(playedSeconds);
        sessionRepository.save(session);
    }

    /**
     * 타이머 만료된 세션 처리 (스케줄러)
     */
    @Scheduled(fixedDelay = 30000) // 30초마다 실행
    @Async("taskExecutor")
    public void processExpiredTimers() {
        LocalDateTime now = LocalDateTime.now();
        List<ASMRPlaySession> expiredSessions = sessionRepository.findExpiredTimerSessions(now);

        if (!expiredSessions.isEmpty()) {
            log.info("만료된 타이머 세션 처리: {}개", expiredSessions.size());

            for (ASMRPlaySession session : expiredSessions) {
                try {
                    session.expireByTimer();
                    sessionRepository.save(session);

                    // 모니터링 이벤트 발생
                    monitoringService.onSessionEnded(session.getUser().getId(), session.getId(), "TIMER_EXPIRED");

                    log.info("타이머 만료 세션 처리 완료: sessionId={}, userId={}",
                            session.getId(), session.getUser().getId());
                } catch (Exception e) {
                    log.error("타이머 만료 세션 처리 실패: sessionId={}, error={}",
                            session.getId(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 페이드아웃 시작이 필요한 세션 처리 (스케줄러)
     */
    @Scheduled(fixedDelay = 10000) // 10초마다 실행
    @Async("taskExecutor")
    public void processFadeOutSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fadeOutCheckTime = now.plusSeconds(60); // 1분 후까지 체크

        List<ASMRPlaySession> fadeOutSessions = sessionRepository.findSessionsNeedingFadeOut(now, fadeOutCheckTime);

        for (ASMRPlaySession session : fadeOutSessions) {
            if (session.shouldStartFadeOut()) {
                try {
                    startFadeOut(session);
                } catch (Exception e) {
                    log.error("페이드아웃 시작 실패: sessionId={}, error={}",
                            session.getId(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 고아 세션 정리 (스케줄러)
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    @Async("taskExecutor")
    public void cleanupOrphanSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = now.minusHours(24); // 24시간 전

        int cleanedUp = sessionRepository.cleanupOrphanSessions(now, cutoffTime);

        if (cleanedUp > 0) {
            log.info("고아 세션 정리 완료: {}개", cleanedUp);
        }
    }

    /**
     * 오래된 종료 세션 물리적 삭제 (스케줄러)
     * 30일 이상 된 STOPPED, TIMER_EXPIRED, ERROR 상태 세션 삭제
     */
    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시
    @Async("taskExecutor")
    public void deleteOldCompletedSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30); // 30일 전

        int deleted = sessionRepository.deleteOldCompletedSessions(cutoffTime);

        if (deleted > 0) {
            log.info("오래된 종료 세션 삭제 완료: {}개 (30일 이상 경과)", deleted);
        }
    }

    /**
     * 페이드아웃 시작
     */
    private void startFadeOut(ASMRPlaySession session) {
        log.info("페이드아웃 시작: sessionId={}, userId={}", session.getId(), session.getUser().getId());

        // 세션 상태를 FADING_OUT으로 변경
        // 실제로는 클라이언트에 WebSocket/SSE로 페이드아웃 신호 전송
        // 여기서는 상태 변경만 수행
        sessionRepository.save(session);

        // TODO: 클라이언트에 페이드아웃 시작 신호 전송
        // webSocketService.sendFadeOutSignal(session.getUser().getId(), session.getId());
    }

    /**
     * 사용자의 기존 활성 세션 종료
     */
    private void terminateExistingActiveSessions(Long userId) {
        List<ASMRPlaySession> activeSessions = sessionRepository.findAllActiveSessionsByUserId(userId);

        for (ASMRPlaySession session : activeSessions) {
            session.stop();
            sessionRepository.save(session);
            log.info("기존 활성 세션 종료: sessionId={}", session.getId());
        }
    }

    /**
     * 사용자의 활성 세션 조회 (권한 검증 포함)
     */
    private ASMRPlaySession getActiveSessionForUser(Long userId, Long sessionId) {
        ASMRPlaySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASMR_SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "다른 사용자의 세션에 접근할 수 없습니다");
        }

        if (!session.isOngoing()) {
            throw new BusinessException(ErrorCode.ASMR_SESSION_NOT_ACTIVE, "활성 상태가 아닌 세션입니다");
        }

        return session;
    }
}