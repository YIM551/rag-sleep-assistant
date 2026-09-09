package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.sleep.UserSleepSettingDto;
import com.sleepwell.sleepwell_backend.dto.sleep.UserSleepSettingRequestDto;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.service.UserService;
import com.sleepwell.sleepwell_backend.service.UserSleepSettingService;
import com.sleepwell.sleepwell_backend.service.PersonalizedSchedulerService;
import com.sleepwell.sleepwell_backend.service.PersonalizedTimingCalculator;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 수면 설정 컨트롤러
 * 
 * 개인화된 수면 알림 및 스케줄링을 위한 사용자별 설정 관리 API를 제공합니다.
 * Slack의 사례를 참고하여 User 테이블에서 분리된 설정 전용 엔드포인트입니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Tag(name = "수면 설정", description = "개인화된 수면 알림 및 스케줄 설정 관리")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequestMapping("/api/users/sleep-settings")
public class UserSleepSettingController {

    private final UserSleepSettingService userSleepSettingService;
    private final UserService userService;
    
    // 임시로 순환 의존성 제거 - 기본 기능 먼저 확인
    // private PersonalizedSchedulerService personalizedSchedulerService;
    // private PersonalizedTimingCalculator timingCalculator;
    
    // 생성자 주입 (순환 의존성 제거 버전)
    public UserSleepSettingController(
            UserSleepSettingService userSleepSettingService,
            UserService userService) {
        this.userSleepSettingService = userSleepSettingService;
        this.userService = userService;
    }
    
    @PostConstruct
    public void init() {
        log.info("UserSleepSettingController 초기화 완료 - 기본 모드");
        log.info("UserSleepSettingService 로딩: {}", userSleepSettingService != null ? "성공" : "실패");
        log.info("UserService 로딩: {}", userService != null ? "성공" : "실패");
    }

    /**
     * 현재 사용자의 수면 설정 조회
     */
    @Operation(
        summary = "수면 설정 조회", 
        description = "현재 로그인한 사용자의 개인화된 수면 설정을 조회합니다. " +
                      "설정이 없으면 기본 설정으로 자동 생성됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "수면 설정 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserSleepSettingDto.class),
                examples = @ExampleObject(
                    name = "성공 응답 예시",
                    value = """
                    {
                        "id": 1,
                        "userId": 123,
                        "sleepReminderMinutesBefore": 60,
                        "phoneInactiveStartTime": "22:00:00",
                        "phoneInactiveEndTime": "07:00:00",
                        "weekendDifferentSchedule": true,
                        "weekendBedtime": "23:30:00",
                        "weekendWakeupTime": "09:00:00",
                        "smartTimingEnabled": false,
                        "consecutiveIgnoredNotifications": 0,
                        "mostEffectiveReminderMinutes": null,
                        "sleepReminderEnabled": true,
                        "wakeupReminderEnabled": true,
                        "notificationSound": "default"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "error": "인증이 필요합니다"
                    }
                    """
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<UserSleepSettingDto> getUserSleepSetting(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.info("사용자 수면 설정 조회 요청 - userId: {}", userId);
        
        UserSleepSettingDto setting = userSleepSettingService.getUserSleepSetting(userId);
        
        log.info("사용자 수면 설정 조회 성공 - userId: {}, settingId: {}", userId, setting.getId());
        return ResponseEntity.ok(setting);
    }

    /**
     * 사용자의 수면 설정 생성
     */
    @Operation(
        summary = "수면 설정 생성", 
        description = "현재 사용자의 새로운 수면 설정을 생성합니다. " +
                      "이미 설정이 있는 경우 409 Conflict 에러를 반환합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "수면 설정 생성 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserSleepSettingDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 데이터",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2025-01-06T12:00:00.000Z",
                        "status": 400,
                        "error": "Validation Failed",
                        "message": "입력 값 유효성 검사에 실패했습니다.",
                        "path": "/api/users/sleep-settings",
                        "details": {
                            "sleepReminderMinutesBefore": [
                                "알림 시간은 최소 15분 이상이어야 합니다."
                            ]
                        }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "이미 설정이 존재함",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "error": "이미 수면 설정이 존재합니다"
                    }
                    """
                )
            )
        )
    })
    @PostMapping
    public ResponseEntity<UserSleepSettingDto> createUserSleepSetting(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "수면 설정 생성 정보",
                content = @Content(
                    schema = @Schema(implementation = UserSleepSettingRequestDto.class)
                )
            )
            @Valid @RequestBody UserSleepSettingRequestDto request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.info("사용자 수면 설정 생성 요청 - userId: {}", userId);
        
        UserSleepSettingDto setting = userSleepSettingService.createUserSleepSetting(userId, request);
        
        log.info("사용자 수면 설정 생성 성공 - userId: {}, settingId: {}", userId, setting.getId());
        return ResponseEntity.status(201).body(setting);
    }

    /**
     * 사용자의 수면 설정 업데이트
     */
    @Operation(
        summary = "수면 설정 업데이트", 
        description = "현재 사용자의 수면 설정을 업데이트합니다. " +
                      "설정이 없는 경우 자동으로 생성 후 업데이트합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "수면 설정 업데이트 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserSleepSettingDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 데이터",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "error": "알림 시간은 15분에서 180분 사이여야 합니다"
                    }
                    """
                )
            )
        )
    })
    @PutMapping
    public ResponseEntity<UserSleepSettingDto> updateUserSleepSetting(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "수면 설정 업데이트 정보",
                content = @Content(
                    schema = @Schema(implementation = UserSleepSettingRequestDto.class)
                )
            )
            @Valid @RequestBody UserSleepSettingRequestDto request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.info("사용자 수면 설정 업데이트 요청 - userId: {}", userId);
        
        UserSleepSettingDto setting;
        if (userSleepSettingService.userSettingExists(userId)) {
            setting = userSleepSettingService.updateUserSleepSetting(userId, request);
            log.info("사용자 수면 설정 업데이트 성공 - userId: {}, settingId: {}", userId, setting.getId());
        } else {
            setting = userSleepSettingService.createUserSleepSetting(userId, request);
            log.info("사용자 수면 설정 생성 후 업데이트 완료 - userId: {}, settingId: {}", userId, setting.getId());
        }
        
        return ResponseEntity.ok(setting);
    }

    /**
     * 사용자의 수면 설정 삭제 (기본값으로 리셋)
     */
    @Operation(
        summary = "수면 설정 초기화", 
        description = "현재 사용자의 수면 설정을 삭제하여 기본값으로 초기화합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "수면 설정 초기화 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    "수면 설정이 기본값으로 초기화되었습니다"
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "설정을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "error": "삭제할 수면 설정을 찾을 수 없습니다"
                    }
                    """
                )
            )
        )
    })
    @DeleteMapping
    public ResponseEntity<String> deleteUserSleepSetting(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.info("사용자 수면 설정 삭제 요청 - userId: {}", userId);
        
        userSleepSettingService.deleteUserSleepSetting(userId);
        
        log.info("사용자 수면 설정 삭제 성공 - userId: {}", userId);
        return ResponseEntity.ok("수면 설정이 기본값으로 초기화되었습니다");
    }

    /**
     * 알림 무시 기록 (스마트 타이밍 학습용)
     */
    @Operation(
        summary = "알림 무시 기록", 
        description = "사용자가 수면 알림을 무시했을 때 호출하는 API입니다. " +
                      "스마트 타이밍 기능의 학습 데이터로 활용됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "알림 무시 기록 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    "알림 무시가 기록되었습니다"
                    """
                )
            )
        )
    })
    @PostMapping("/ignore-notification")
    public ResponseEntity<String> recordIgnoredNotification(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.debug("알림 무시 기록 요청 - userId: {}", userId);
        
        userSleepSettingService.incrementIgnoredNotifications(userId);
        
        log.debug("알림 무시 기록 완료 - userId: {}", userId);
        return ResponseEntity.ok("알림 무시가 기록되었습니다");
    }

    /**
     * 효과적인 알림 시간 기록 (스마트 타이밍 학습용)
     */
    @Operation(
        summary = "효과적인 알림 시간 기록", 
        description = "사용자가 알림에 반응했을 때의 시간을 기록하는 API입니다. " +
                      "스마트 타이밍 기능의 학습 데이터로 활용됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "효과적인 알림 시간 기록 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    "효과적인 알림 시간이 학습되었습니다"
                    """
                )
            )
        )
    })
    @PostMapping("/record-effective-timing")
    public ResponseEntity<String> recordEffectiveTiming(
            @RequestParam("minutes") Integer effectiveMinutes,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.info("효과적인 알림 시간 기록 요청 - userId: {}, effectiveMinutes: {}", userId, effectiveMinutes);
        
        userSleepSettingService.recordEffectiveNotificationTiming(userId, effectiveMinutes);
        
        log.info("효과적인 알림 시간 기록 완료 - userId: {}, effectiveMinutes: {}", userId, effectiveMinutes);
        return ResponseEntity.ok("효과적인 알림 시간이 학습되었습니다");
    }

    /**
     * 기본 설정 사용 여부 확인
     */
    @Operation(
        summary = "기본 설정 사용 여부 확인", 
        description = "현재 사용자가 기본 설정을 사용하는지 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "확인 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "isUsingDefaultSettings": true
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/is-default")
    public ResponseEntity<Object> isUsingDefaultSettings(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        log.debug("기본 설정 사용 여부 확인 요청 - userId: {}", userId);
        
        boolean isDefault = userSleepSettingService.isUsingDefaultSettings(userId);
        
        return ResponseEntity.ok(java.util.Map.of("isUsingDefaultSettings", isDefault));
    }

    // === 스마트 타이밍 및 스케줄링 관련 API ===
    // TODO: 임시로 주석 처리 - PersonalizedSchedulerService, PersonalizedTimingCalculator 의존성 문제

    /**
     * 사용자의 스마트 타이밍 재계산 및 스케줄 업데이트
     */
    /*
    @Operation(
        summary = "스마트 타이밍 재계산",
        description = "현재 사용자의 수면 패턴을 분석하여 최적의 알림 시간을 재계산하고 스케줄을 업데이트합니다. " +
                      "충분한 수면 기록 데이터가 있어야 정확한 계산이 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "스마트 타이밍 재계산 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"message\": \"스마트 타이밍이 성공적으로 업데이트되었습니다\"}")
            )
        ),
        @ApiResponse(responseCode = "400", description = "충분한 데이터가 없거나 스마트 타이밍이 비활성화됨"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자 설정을 찾을 수 없음")
    })
    @PostMapping("/smart-timing/recalculate")
    public ResponseEntity<?> recalculateSmartTiming(Authentication authentication) {
        log.info("스마트 타이밍 재계산 요청");
        
        try {
            Long userId = getCurrentUserId(authentication);
            
            // 스마트 타이밍이 활성화되어 있는지 확인
            UserSleepSettingDto setting = userSleepSettingService.getUserSleepSetting(userId);
            if (!setting.getSmartTimingEnabled()) {
                throw new BusinessException(ErrorCode.SMART_TIMING_DISABLED);
            }
            
            // 스마트 타이밍 재계산
            timingCalculator.recalculateUserTiming(userId);
            
            // 스케줄 업데이트
            personalizedSchedulerService.updateUserSchedule(userId);
            
            log.info("사용자 {}의 스마트 타이밍 재계산 완료", userId);
            
            return ResponseEntity.ok()
                .body(java.util.Map.of("message", "스마트 타이밍이 성공적으로 업데이트되었습니다"));
                
        } catch (BusinessException e) {
            log.error("스마트 타이밍 재계산 실패 - 비즈니스 예외: {}", e.getMessage());
            throw e; // GlobalExceptionHandler에서 처리
        } catch (Exception e) {
            log.error("스마트 타이밍 재계산 중 예상치 못한 오류 발생", e);
            throw new BusinessException(ErrorCode.INVALID_TIMING_CALCULATION);
        }
    }
    */

    /**
     * 사용자의 알림 반응 패턴 분석 결과 조회
     */
    /*
    @Operation(
        summary = "알림 패턴 분석 조회",
        description = "현재 사용자의 알림 반응 패턴을 분석한 결과를 조회합니다. " +
                      "반응률, 최적 시간, 통계 등을 포함합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "패턴 분석 결과 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자 설정을 찾을 수 없음")
    })
    @GetMapping("/smart-timing/analysis")
    public ResponseEntity<?> getNotificationPatternAnalysis(Authentication authentication) {
        log.info("알림 패턴 분석 조회 요청");
        
        try {
            Long userId = getCurrentUserId(authentication);
            
            PersonalizedTimingCalculator.TimingAnalysisResult analysis = 
                timingCalculator.analyzeUserNotificationPattern(userId);
            
            log.info("사용자 {}의 패턴 분석 완료", userId);
            
            return ResponseEntity.ok(analysis);
            
        } catch (BusinessException e) {
            log.error("패턴 분석 조회 실패 - 비즈니스 예외: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("패턴 분석 조회 중 예상치 못한 오류 발생", e);
            throw new BusinessException(ErrorCode.TIMING_ANALYSIS_FAILED);
        }
    }
    */

    /**
     * 추천 알림 시간 조회
     */
    /*
    @Operation(
        summary = "추천 알림 시간 조회",
        description = "현재 사용자의 수면 기록을 기반으로 최적의 알림 시간(분 단위)을 추천합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "추천 시간 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"recommendedMinutes\": 60, \"hasRecommendation\": true}")
            )
        ),
        @ApiResponse(responseCode = "200", description = "추천할 데이터가 부족함",
            content = @Content(
                mediaType = "application/json", 
                examples = @ExampleObject(value = "{\"recommendedMinutes\": null, \"hasRecommendation\": false}")
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/smart-timing/recommendation")
    public ResponseEntity<?> getRecommendedReminderTime(Authentication authentication) {
        log.info("추천 알림 시간 조회 요청");
        
        Long userId = getCurrentUserId(authentication);
        
        java.util.Optional<Integer> recommendation = 
            timingCalculator.recommendOptimalReminderMinutes(userId);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("recommendedMinutes", recommendation.orElse(null));
        response.put("hasRecommendation", recommendation.isPresent());
        
        log.info("사용자 {}의 추천 시간 조회 완료: {}", userId, recommendation.orElse(null));
        
        return ResponseEntity.ok(response);
    }
    */

    /**
     * 스마트 타이밍 효과성 평가 조회
     */
    /*
    @Operation(
        summary = "스마트 타이밍 효과성 조회",
        description = "현재 사용자의 스마트 타이밍 시스템 효과성을 0.0~1.0 점수로 평가합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "효과성 평가 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"effectiveness\": 0.75, \"rating\": \"좋음\"}")
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/smart-timing/effectiveness")
    public ResponseEntity<?> getTimingEffectiveness(Authentication authentication) {
        log.info("스마트 타이밍 효과성 조회 요청");
        
        Long userId = getCurrentUserId(authentication);
        
        double effectiveness = timingCalculator.evaluateTimingEffectiveness(userId);
        
        String rating;
        if (effectiveness >= 0.8) {
            rating = "매우 좋음";
        } else if (effectiveness >= 0.6) {
            rating = "좋음";
        } else if (effectiveness >= 0.4) {
            rating = "보통";
        } else if (effectiveness >= 0.2) {
            rating = "개선 필요";
        } else {
            rating = "데이터 부족";
        }
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("effectiveness", effectiveness);
        response.put("rating", rating);
        
        log.info("사용자 {}의 효과성 평가 완료: {}", userId, effectiveness);
        
        return ResponseEntity.ok(response);
    }
    */

    /**
     * 현재 활성 스케줄 상태 조회
     */
    /*
    @Operation(
        summary = "활성 스케줄 상태 조회",
        description = "현재 사용자의 활성화된 개인화 스케줄 개수와 다음 예정 알림 시간을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "스케줄 상태 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/schedule/status")
    public ResponseEntity<?> getScheduleStatus(Authentication authentication) {
        log.info("스케줄 상태 조회 요청");
        
        Long userId = getCurrentUserId(authentication);
        
        int userActiveSchedules = personalizedSchedulerService.getUserActiveScheduleCount(userId);
        int totalActiveSchedules = personalizedSchedulerService.getActiveScheduleCount();
        java.util.List<java.time.LocalTime> upcomingTimes = personalizedSchedulerService.getUpcomingReminderTimes();
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("userActiveSchedules", userActiveSchedules);
        response.put("totalActiveSchedules", totalActiveSchedules);
        response.put("upcomingReminderTimes", upcomingTimes);
        
        log.info("사용자 {}의 스케줄 상태 조회 완료", userId);
        
        return ResponseEntity.ok(response);
    }
    */

    // === 내부 헬퍼 메서드 ===

    /**
     * 인증 객체에서 사용자 ID 추출
     * 기존 UserProfileController의 패턴을 따름
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("인증되지 않은 요청");
            throw new IllegalArgumentException("인증이 필요합니다");
        }
        
        String userEmail = getCurrentUserEmail(authentication);
        User user = userService.findUserByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        return user.getId();
    }

    /**
     * 인증 객체에서 사용자 이메일 추출
     * UserProfileController와 동일한 로직
     */
    private String getCurrentUserEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String userEmail;
        
        if (principal instanceof UserDetails) {
            userEmail = ((UserDetails) principal).getUsername();
        } else {
            userEmail = authentication.getName();
        }
        
        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.error("유효하지 않은 사용자 이메일: {}", userEmail);
            throw new IllegalArgumentException("유효하지 않은 사용자 이메일입니다");
        }
        
        return userEmail;
    }
}