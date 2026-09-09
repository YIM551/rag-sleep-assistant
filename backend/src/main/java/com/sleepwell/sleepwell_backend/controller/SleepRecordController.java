package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.SleepRecordRequestDto;
import com.sleepwell.sleepwell_backend.dto.SleepRecordResponseDto;
import com.sleepwell.sleepwell_backend.dto.FlutterHealthSleepDataDto;
import com.sleepwell.sleepwell_backend.dto.PlatformSleepDataDto;
import com.sleepwell.sleepwell_backend.dto.UnifiedSleepAnalysisDto;
import com.sleepwell.sleepwell_backend.dto.SleepAudioEventRequestDto;
import com.sleepwell.sleepwell_backend.dto.SleepAudioEventResponseDto;
import com.sleepwell.sleepwell_backend.dto.AudioEventStatisticsDto;
import com.sleepwell.sleepwell_backend.dto.SleepAudioTrendAnalysisDto;
import com.sleepwell.sleepwell_backend.dto.SleepTrendAnalysisWithInsightsDto;
import com.sleepwell.sleepwell_backend.dto.InsightDto;
import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import com.sleepwell.sleepwell_backend.entity.AnalysisExecutionJob;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import com.sleepwell.sleepwell_backend.enums.Priority;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorResponse;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import com.sleepwell.sleepwell_backend.service.SleepRecordService;
import com.sleepwell.sleepwell_backend.service.PlatformSleepDataIntegrationService;
import com.sleepwell.sleepwell_backend.service.SleepAnalysisExecutionService;
import com.sleepwell.sleepwell_backend.service.AudioEventStatisticsService;
import com.sleepwell.sleepwell_backend.service.SleepAudioTrendAnalysisService;
import com.sleepwell.sleepwell_backend.service.InsightGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.temporal.ChronoUnit;

/**
 * 수면 기록 관리 REST API 컨트롤러
 * 
 * 사용자의 수면 데이터 전체 생명주기를 관리하는 REST API를 제공합니다.
 * 다양한 플랫폼(Apple Health, Samsung Health, Flutter Health)의 데이터를 
 * 통합하여 처리하고, AI 기반 분석 시스템과 연동합니다.
 * 
 * 주요 기능:
 * - 수면 기록 CRUD 작업 (생성, 조회, 삭제)
 * - 다중 플랫폼 데이터 통합 처리
 * - 실시간 데이터 동기화 및 분석 트리거
 * - 날짜/기간별 수면 데이터 조회
 * - AI 상담 시스템 자동 트리거
 * 
 * 지원 플랫폼:
 * - Apple Health (HealthKit)
 * - Samsung Health
 * - Flutter Health Plugin
 * - 수동 입력 데이터
 * 
 * 보안 및 인증:
 * - JWT 토큰 기반 인증 필수
 * - 사용자별 데이터 접근 제어
 * - 요청 데이터 유효성 검증
 * 
 * 성능 최적화:
 * - 페이징을 통한 대용량 데이터 처리
 * - 비동기 분석 작업 큐 시스템
 * - 플랫폼별 데이터 처리 최적화
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepRecordService
 * @see PlatformSleepDataIntegrationService
 * @see SleepAnalysisExecutionService
 */
@RestController
@RequestMapping("/api/sleep/records")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "수면 기록", description = "수면 기록 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class SleepRecordController {

    private final SleepRecordService sleepRecordService;
    private final PlatformSleepDataIntegrationService platformSleepDataIntegrationService;
    private final SleepAnalysisExecutionService sleepAnalysisExecutionService;
    private final AudioEventStatisticsService audioEventStatisticsService;
    private final SleepAudioTrendAnalysisService sleepAudioTrendAnalysisService;
    private final InsightGenerationService insightGenerationService;
    private final UserRepository userRepository;

    /**
     * 수면 기록 생성
     */
    @PostMapping
    @Operation(summary = "수면 기록 생성", description = "새로운 수면 기록을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "수면 기록 생성 성공",
                    content = @Content(schema = @Schema(implementation = SleepRecordResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "데이터 중복 (이미 해당 날짜의 기록이 존재)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "요청 한도 초과 (Rate Limit)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SleepRecordResponseDto> createSleepRecord(
            @Valid @RequestBody SleepRecordRequestDto requestDto,
            Authentication authentication) {
        
        log.info("Creating sleep record for user: {}", authentication.getName());
        
        // 데이터 무결성 검증
        requestDto.validateDataIntegrity();
        
        Long userId = extractUserIdFromAuthentication(authentication);
        SleepRecordResponseDto responseDto = sleepRecordService.createSleepRecord(userId, requestDto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * Flutter Health 데이터로 수면 기록 생성
     */
    @PostMapping("/flutter-health")
    @Operation(summary = "Flutter Health 데이터로 수면 기록 생성", 
               description = "Flutter Health 패키지에서 수집한 수면 데이터로 수면 기록을 생성합니다. iOS HealthKit과 Android Health Connect 데이터를 모두 지원합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "수면 기록 생성 성공", 
                    content = @Content(schema = @Schema(implementation = SleepRecordResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<SleepRecordResponseDto> createSleepRecordFromFlutterHealth(
            @Valid @RequestBody FlutterHealthSleepDataDto flutterHealthData,
            Authentication authentication) {
        
        log.info("Creating sleep record from Flutter Health data for user: {}", authentication.getName());
        
        Long userId = extractUserIdFromAuthentication(authentication);
        
        // Flutter Health DTO를 기존 SleepRecordRequestDto로 변환
        SleepRecordRequestDto requestDto = flutterHealthData.toSleepRecordRequestDto();
        
        SleepRecordResponseDto responseDto = sleepRecordService.createSleepRecord(userId, requestDto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * 수면 기록 목록 조회 (페이징)
     */
    @GetMapping
    @Operation(summary = "수면 기록 목록 조회", description = "사용자의 수면 기록 목록을 페이징으로 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<Page<SleepRecordResponseDto>> getSleepRecords(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준 필드") @RequestParam(defaultValue = "recordDate") String sortBy,
            @Parameter(description = "정렬 방향 (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        
        log.info("Getting sleep records for user: {}, page: {}, size: {}", authentication.getName(), page, size);
        
        Long userId = extractUserIdFromAuthentication(authentication);
        Page<SleepRecordResponseDto> responseDto = sleepRecordService.getSleepRecords(userId, page, size, sortBy, sortDir);
        
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 특정 수면 기록 조회
     */
    @GetMapping("/{recordId}")
    @Operation(summary = "특정 수면 기록 조회", description = "ID로 특정 수면 기록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", 
                    content = @Content(schema = @Schema(implementation = SleepRecordResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "수면 기록을 찾을 수 없음"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<SleepRecordResponseDto> getSleepRecord(
            @Parameter(description = "수면 기록 ID") @PathVariable Long recordId,
            Authentication authentication) {
        
        log.info("Getting sleep record: {} for user: {}", recordId, authentication.getName());
        
        Long userId = extractUserIdFromAuthentication(authentication);
        SleepRecordResponseDto responseDto = sleepRecordService.getSleepRecord(userId, recordId);
        
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 특정 날짜의 수면 기록 조회
     */
    @GetMapping("/date/{date}")
    @Operation(summary = "특정 날짜 수면 기록 조회", description = "특정 날짜의 수면 기록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", 
                    content = @Content(schema = @Schema(implementation = SleepRecordResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "해당 날짜의 수면 기록을 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<SleepRecordResponseDto> getSleepRecordByDate(
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd 형식)")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        
        log.info("Getting sleep record for user: {} on date: {}", authentication.getName(), date);
        
        Long userId = extractUserIdFromAuthentication(authentication);
        SleepRecordResponseDto responseDto = sleepRecordService.getSleepRecordByDate(userId, date);
        
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 기간별 수면 기록 조회 (페이징)
     */
    @GetMapping("/range")
    @Operation(summary = "기간별 수면 기록 조회", description = "지정된 기간 내의 수면 기록을 페이징으로 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 날짜 범위"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Page<SleepRecordResponseDto>> getSleepRecordsBetweenDates(
            @Parameter(description = "시작 날짜 (yyyy-MM-dd 형식)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd 형식)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(sort = "recordDate", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        
        log.info("Getting sleep records for user: {} between {} and {} with paging", 
                authentication.getName(), startDate, endDate);
        
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("시작 날짜가 종료 날짜보다 늦을 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        
        Long userId = extractUserIdFromAuthentication(authentication);
        Page<SleepRecordResponseDto> responseDto = sleepRecordService.getSleepRecordsBetweenDates(userId, startDate, endDate, pageable);
        
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 수면 기록 삭제
     */
    @DeleteMapping("/{recordId}")
    @Operation(summary = "수면 기록 삭제", description = "특정 수면 기록을 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "404", description = "수면 기록을 찾을 수 없음")
    @ApiResponse(responseCode = "403", description = "접근 권한 없음")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Void> deleteSleepRecord(
            @Parameter(description = "삭제할 수면 기록 ID") @PathVariable Long recordId,
            Authentication authentication) {
        
        log.info("Deleting sleep record: {} for user: {}", recordId, authentication.getName());
        
        Long userId = extractUserIdFromAuthentication(authentication);
        sleepRecordService.deleteSleepRecord(userId, recordId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Authentication에서 사용자 ID 추출
     */
    private Long extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("인증 정보가 존재하지 않습니다");
        }
        
        String userEmail = authentication.getName();
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 이메일이 존재하지 않습니다");
        }
        
        // 이메일로 사용자 조회
        return userRepository.findByEmail(userEmail)
                .map(User::getId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다: " + userEmail, HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
    }

    // ==================== 플랫폼 통합 API 엔드포인트 ====================

    /**
     * Samsung Health 플랫폼 데이터로 수면 기록 생성
     */
    @PostMapping("/platform/samsung")
    @Operation(summary = "Samsung Health 플랫폼 데이터로 수면 기록 생성",
               description = "Samsung Health에서 제공하는 개인화된 수면 분석 데이터를 활용하여 수면 기록을 생성하고 AI 상담 시스템을 트리거합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "수면 기록 생성 성공", 
                    content = @Content(schema = @Schema(implementation = SleepRecordResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 플랫폼 데이터 형식"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "409", description = "동일한 시간대의 수면 기록이 이미 존재합니다"),
        @ApiResponse(responseCode = "422", description = "데이터 처리 불가")
    })
    public ResponseEntity<SleepRecordResponseDto> createSleepRecordFromSamsungHealth(
            @Valid @RequestBody PlatformSleepDataDto platformData,
            Authentication authentication) {
        
        log.info("Creating sleep record from Samsung Health data for user: {}", authentication.getName());
        
        try {
            Long userId = extractUserIdFromAuthentication(authentication);
            
            // Samsung Health 플랫폼 데이터 처리 및 통합
            UnifiedSleepAnalysisDto unifiedAnalysis = platformSleepDataIntegrationService
                    .processPlatformData(WearableSource.SAMSUNG_HEALTH, platformData);
            
            // 수면 기록 생성
            SleepRecordResponseDto responseDto = sleepRecordService
                    .createSleepRecordFromPlatformData(userId, unifiedAnalysis);
            
            // AI 상담 시스템 트리거 조건 확인 및 실행
            checkAndTriggerAiConsultation(userId, unifiedAnalysis);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
            
        } catch (BusinessException e) {
            log.error("Business logic error in Samsung Health data processing for user: {}", authentication.getName(), e);
            throw e; // Re-throw BusinessException to preserve the original status code
        } catch (Exception e) {
            log.error("Failed to process Samsung Health data for user: {}", authentication.getName(), e);
            throw new BusinessException("Samsung Health 데이터 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * Apple Health 플랫폼 데이터로 수면 기록 생성
     */
    @PostMapping("/platform/apple")
    @Operation(summary = "Apple Health 플랫폼 데이터로 수면 기록 생성",
               description = "Apple Health에서 제공하는 개인화된 수면 분석 데이터를 활용하여 수면 기록을 생성하고 AI 상담 시스템을 트리거합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "수면 기록 생성 성공", 
                    content = @Content(schema = @Schema(implementation = SleepRecordResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 플랫폼 데이터 형식"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "409", description = "동일한 시간대의 수면 기록이 이미 존재합니다"),
        @ApiResponse(responseCode = "422", description = "데이터 처리 불가")
    })
    public ResponseEntity<SleepRecordResponseDto> createSleepRecordFromAppleHealth(
            @Valid @RequestBody PlatformSleepDataDto platformData,
            Authentication authentication) {
        
        log.info("Creating sleep record from Apple Health data for user: {}", authentication.getName());
        
        try {
            Long userId = extractUserIdFromAuthentication(authentication);
            
            // Apple Health 플랫폼 데이터 처리 및 통합
            UnifiedSleepAnalysisDto unifiedAnalysis = platformSleepDataIntegrationService
                    .processPlatformData(WearableSource.APPLE_HEALTH, platformData);
            
            // 수면 기록 생성
            SleepRecordResponseDto responseDto = sleepRecordService
                    .createSleepRecordFromPlatformData(userId, unifiedAnalysis);
            
            // AI 상담 시스템 트리거 조건 확인 및 실행
            checkAndTriggerAiConsultation(userId, unifiedAnalysis);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
            
        } catch (BusinessException e) {
            log.error("Business logic error in Apple Health data processing for user: {}", authentication.getName(), e);
            throw e; // Re-throw BusinessException to preserve the original status code
        } catch (Exception e) {
            log.error("Failed to process Apple Health data for user: {}", authentication.getName(), e);
            throw new BusinessException("Apple Health 데이터 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 통합 플랫폼 데이터로 수면 기록 생성
     */
    @PostMapping("/platform/integrated")
    @Operation(summary = "통합 플랫폼 데이터로 수면 기록 생성",
               description = "여러 플랫폼의 개인화된 수면 분석 데이터를 통합하여 수면 기록을 생성하고 AI 상담 시스템을 트리거합니다. 플랫폼 자동 감지 기능을 제공합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "수면 기록 생성 성공", 
                    content = @Content(schema = @Schema(implementation = SleepRecordResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 플랫폼 데이터 형식"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "409", description = "동일한 시간대의 수면 기록이 이미 존재합니다"),
        @ApiResponse(responseCode = "422", description = "데이터 처리 불가")
    })
    public ResponseEntity<SleepRecordResponseDto> createSleepRecordFromIntegratedPlatform(
            @Valid @RequestBody PlatformSleepDataDto platformData,
            Authentication authentication) {
        
        log.info("Creating sleep record from integrated platform data for user: {}", authentication.getName());
        
        try {
            Long userId = extractUserIdFromAuthentication(authentication);
            
            // 플랫폼 자동 감지 및 데이터 처리
            WearableSource detectedPlatform = platformSleepDataIntegrationService
                    .detectPlatformFromData(platformData);
            
            log.info("Detected platform: {} for user: {}", detectedPlatform, authentication.getName());
            
            // 감지된 플랫폼에 따른 데이터 처리
            UnifiedSleepAnalysisDto unifiedAnalysis = platformSleepDataIntegrationService
                    .processPlatformData(detectedPlatform, platformData);
            
            // 수면 기록 생성
            SleepRecordResponseDto responseDto = sleepRecordService
                    .createSleepRecordFromPlatformData(userId, unifiedAnalysis);
            
            // AI 상담 시스템 트리거 조건 확인 및 실행
            checkAndTriggerAiConsultation(userId, unifiedAnalysis);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
            
        } catch (BusinessException e) {
            log.error("Business logic error in integrated platform data processing for user: {}", authentication.getName(), e);
            throw e; // Re-throw BusinessException to preserve the original status code
        } catch (Exception e) {
            log.error("Failed to process integrated platform data for user: {}", authentication.getName(), e);
            throw new BusinessException("통합 플랫폼 데이터 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 플랫폼 데이터 동기화 트리거
     */
    @PostMapping("/platform/sync")
    @Operation(summary = "플랫폼 데이터 동기화 트리거",
               description = "플랫폼에서 새로운 수면 데이터 동기화를 트리거하고 분석 작업을 예약합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "동기화 작업 예약 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 동기화 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "409", description = "이미 동기화 작업이 진행 중입니다")
    })
    @Transactional
    public ResponseEntity<Map<String, Object>> triggerPlatformDataSync(
            @Parameter(description = "동기화할 플랫폼")
            @RequestParam WearableSource platform,
            @Parameter(description = "동기화 우선순위 (기본값: NORMAL)")
            @RequestParam(defaultValue = "NORMAL") Priority priority,
            Authentication authentication) {
        
        log.info("Triggering platform data sync for user: {}, platform: {}, priority: {}", 
                authentication.getName(), platform, priority);
        
        try {
            Long userId = extractUserIdFromAuthentication(authentication);
            
            // 플랫폼 데이터 동기화 작업 예약 (더미 데이터로 임시 처리)
            PlatformSleepDataDto dummyData = PlatformSleepDataDto.builder()
                    .source(platform)
                    .sleepStartTime(LocalDateTime.now().minusHours(8))
                    .sleepEndTime(LocalDateTime.now())
                    .collectedAt(LocalDateTime.now())
                    .dataQualityScore(85)
                    .build();
            
            AnalysisExecutionJob job = sleepAnalysisExecutionService.scheduleAnalysis(
                    userId, platform, "sync-" + System.currentTimeMillis(), dummyData, 1);
            Long jobId = job.getId();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "플랫폼 데이터 동기화가 예약되었습니다");
            response.put("jobId", jobId);
            response.put("platform", platform);
            response.put("priority", priority);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            
        } catch (Exception e) {
            log.error("Failed to trigger platform data sync for user: {}", authentication.getName(), e);
            throw new BusinessException("플랫폼 데이터 동기화 예약 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 분석 작업 상태 조회
     */
    @GetMapping("/platform/sync/status/{jobId}")
    @Operation(summary = "분석 작업 상태 조회",
               description = "예약된 분석 작업의 현재 상태를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "작업 상태 조회 성공")
    @ApiResponse(responseCode = "404", description = "작업을 찾을 수 없음")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getAnalysisJobStatus(
            @Parameter(description = "작업 ID") @PathVariable Long jobId,
            Authentication authentication) {
        
        log.info("Getting analysis job status: {} for user: {}", jobId, authentication.getName());
        
        Long userId = extractUserIdFromAuthentication(authentication);
        
        // 작업 상태 조회
        Optional<AnalysisExecutionJob> jobOpt = sleepAnalysisExecutionService.getAnalysisJobById(jobId);
        
        if (jobOpt.isEmpty()) {
            throw new BusinessException("분석 작업을 찾을 수 없습니다: " + jobId);
        }
        
        AnalysisExecutionJob job = jobOpt.get();
        
        // 사용자 권한 확인
        if (!job.getUser().getId().equals(userId)) {
            throw new BusinessException("해당 작업에 대한 접근 권한이 없습니다");
        }
        
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("jobId", job.getId());
        jobStatus.put("status", job.getStatus());
        jobStatus.put("platform", job.getPlatformSource());
        jobStatus.put("createdAt", job.getCreatedAt());
        jobStatus.put("updatedAt", job.getUpdatedAt());
        jobStatus.put("priority", job.getPriority());
        
        return ResponseEntity.ok(jobStatus);
    }

    // ==================== 헬퍼 메서드 ====================

    /**
     * AI 상담 시스템 트리거 조건 확인 및 실행
     */
    private void checkAndTriggerAiConsultation(Long userId, UnifiedSleepAnalysisDto analysis) {
        try {
            // AI 상담 트리거 조건 확인
            boolean shouldTriggerConsultation = shouldTriggerAiConsultation(analysis);
            
            if (shouldTriggerConsultation) {
                log.info("Triggering AI consultation for user: {}", userId);
                
                // 더미 데이터로 AI 상담 트리거
                PlatformSleepDataDto consultationData = PlatformSleepDataDto.builder()
                        .source(WearableSource.HEALTH_CONNECT)
                        .sleepStartTime(LocalDateTime.now().minusHours(8))
                        .sleepEndTime(LocalDateTime.now())
                        .collectedAt(LocalDateTime.now())
                        .dataQualityScore(90)
                        .build();
                
                sleepAnalysisExecutionService.scheduleAnalysis(
                        userId, 
                        WearableSource.HEALTH_CONNECT, 
                        "ai-consultation-" + System.currentTimeMillis(),
                        consultationData,
                        1 // 높은 우선순위
                );
            }
        } catch (Exception e) {
            log.warn("Failed to trigger AI consultation for user: {}", userId, e);
            // AI 상담 트리거 실패는 메인 플로우에 영향을 주지 않음
        }
    }

    /**
     * AI 상담 트리거 조건 판단
     */
    private boolean shouldTriggerAiConsultation(UnifiedSleepAnalysisDto analysis) {
        // 통합 수면 점수가 낮은 경우 (60점 미만)
        if (analysis.getUnifiedSleepScore() != null && analysis.getUnifiedSleepScore() < 60) {
            return true;
        }
        
        // 건강 알림이 있는 경우
        if (analysis.getHealthAlerts() != null && !analysis.getHealthAlerts().isEmpty()) {
            return true;
        }
        
        // 패턴 인사이트가 있는 경우 (이상 패턴 감지)
        if (analysis.getPatternInsights() != null && !analysis.getPatternInsights().isEmpty()) {
            return true;
        }
        
        // 높은 신뢰도와 주목할 만한 발견사항이 있는 경우
        if (analysis.getReliabilityScore() != null && analysis.getReliabilityScore() > 80 &&
            analysis.getKeyInsights() != null && !analysis.getKeyInsights().isEmpty()) {
            return true;
        }
        
        return false;
    }

    // ==================== 오디오 이벤트 메타데이터 API ====================

    /**
     * 수면 오디오 이벤트 메타데이터 생성
     */
    @PostMapping("/{recordId}/audio-events")
    @Operation(summary = "수면 오디오 이벤트 메타데이터 생성",
               description = "웨어러블 기기에서 감지된 수면 중 오디오 이벤트 메타데이터를 생성합니다. " +
                           "실제 오디오 파일이 아닌 분석된 메타데이터만을 처리하여 개인정보 보호와 성능을 보장합니다.")
    @ApiResponse(responseCode = "201", description = "오디오 이벤트 메타데이터 생성 성공", 
                content = @Content(schema = @Schema(implementation = SleepAudioEventResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    @ApiResponse(responseCode = "404", description = "수면 기록을 찾을 수 없음")
    @ApiResponse(responseCode = "403", description = "접근 권한 없음")
    public ResponseEntity<SleepAudioEventResponseDto> createAudioEventMetadata(
            @Parameter(description = "수면 기록 ID") @PathVariable Long recordId,
            @Valid @RequestBody SleepAudioEventRequestDto requestDto,
            Authentication authentication) {
        
        log.info("Creating audio event metadata for sleep record: {} by user: {}", 
                recordId, authentication.getName());
        
        Long userId = extractUserIdFromAuthentication(authentication);
        SleepAudioEventResponseDto responseDto = sleepRecordService
                .createAudioEventMetadata(userId, recordId, requestDto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * 수면 기록의 오디오 이벤트 목록 조회
     */
    @GetMapping("/{recordId}/audio-events")
    @Operation(summary = "수면 기록의 오디오 이벤트 목록 조회",
               description = "특정 수면 기록에 속한 모든 오디오 이벤트 메타데이터를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "수면 기록을 찾을 수 없음"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    public ResponseEntity<List<SleepAudioEventResponseDto>> getAudioEvents(
            @Parameter(description = "수면 기록 ID") @PathVariable Long recordId,
            @Parameter(description = "이벤트 유형 필터") @RequestParam(required = false) AudioEventType eventType,
            Authentication authentication) {
        
        log.info("Getting audio events for sleep record: {} by user: {}", 
                recordId, authentication.getName());
        
        Long userId = extractUserIdFromAuthentication(authentication);
        List<SleepAudioEventResponseDto> responseDto = sleepRecordService
                .getAudioEvents(userId, recordId, eventType);
        
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 특정 오디오 이벤트 조회
     */
    @GetMapping("/{recordId}/audio-events/{eventId}")
    @Operation(summary = "특정 오디오 이벤트 조회",
               description = "ID로 특정 오디오 이벤트 메타데이터를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", 
                    content = @Content(schema = @Schema(implementation = SleepAudioEventResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "오디오 이벤트를 찾을 수 없음"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<SleepAudioEventResponseDto> getAudioEvent(
            @Parameter(description = "수면 기록 ID") @PathVariable Long recordId,
            @Parameter(description = "오디오 이벤트 ID") @PathVariable Long eventId,
            Authentication authentication) {
        
        log.info("Getting audio event: {} for sleep record: {} by user: {}", 
                eventId, recordId, authentication.getName());
        
        Long userId = extractUserIdFromAuthentication(authentication);
        SleepAudioEventResponseDto responseDto = sleepRecordService
                .getAudioEvent(userId, recordId, eventId);
        
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 배치 오디오 이벤트 메타데이터 생성
     */
    @PostMapping("/{recordId}/audio-events/batch")
    @Operation(summary = "배치 오디오 이벤트 메타데이터 생성",
               description = "여러 오디오 이벤트 메타데이터를 한 번에 생성합니다. " +
                           "웨어러블 기기에서 한 밤 동안 감지된 모든 이벤트를 효율적으로 처리할 수 있습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "배치 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "수면 기록을 찾을 수 없음"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    public ResponseEntity<List<SleepAudioEventResponseDto>> createAudioEventMetadataBatch(
            @Parameter(description = "수면 기록 ID") @PathVariable Long recordId,
            @Valid @RequestBody List<SleepAudioEventRequestDto> requestDtos,
            Authentication authentication) {
        
        log.info("Creating {} audio event metadata in batch for sleep record: {} by user: {}", 
                requestDtos.size(), recordId, authentication.getName());
        
        Long userId = extractUserIdFromAuthentication(authentication);
        List<SleepAudioEventResponseDto> responseDtos = sleepRecordService
                .createAudioEventMetadataBatch(userId, recordId, requestDtos);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDtos);
    }

    // ==================== 오디오 이벤트 통계 API ====================

    /**
     * 특정 수면 기록의 오디오 이벤트 통계 조회
     */
    @GetMapping("/{recordId}/audio-events/statistics")
    @Operation(summary = "수면 기록 오디오 이벤트 통계 조회",
               description = "특정 수면 기록에 속한 오디오 이벤트들의 상세 통계를 조회합니다. " +
                           "이벤트 유형별 분석, 강도 분포, 의료적 권장사항 등을 포함합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "통계 조회 성공", 
                    content = @Content(schema = @Schema(implementation = AudioEventStatisticsDto.class))),
        @ApiResponse(responseCode = "404", description = "수면 기록을 찾을 수 없음"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<AudioEventStatisticsDto> getAudioEventStatisticsForRecord(
            @Parameter(description = "수면 기록 ID") @PathVariable Long recordId,
            Authentication authentication) {
        
        log.info("Getting audio event statistics for sleep record: {} by user: {}", 
                recordId, authentication.getName());
        
        Long userId = extractUserIdFromAuthentication(authentication);
        
        // 수면 기록의 날짜를 기준으로 하루 통계 계산
        SleepRecordResponseDto sleepRecord = sleepRecordService.getSleepRecord(userId, recordId);
        LocalDate recordDate = sleepRecord.getRecordDate();
        
        AudioEventStatisticsDto statistics = audioEventStatisticsService
                .calculateStatistics(userId, recordDate, recordDate);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 사용자의 기간별 오디오 이벤트 통계 조회
     */
    @GetMapping("/audio-events/statistics")
    @Operation(summary = "사용자 기간별 오디오 이벤트 통계 조회",
               description = "사용자의 지정된 기간 내 모든 오디오 이벤트에 대한 종합 통계를 조회합니다. " +
                           "트렌드 분석, 패턴 감지, 수면 품질 영향 분석, 의료적 권장사항을 포함합니다.")
    @ApiResponse(responseCode = "200", description = "통계 조회 성공", 
                content = @Content(schema = @Schema(implementation = AudioEventStatisticsDto.class)))
    @ApiResponse(responseCode = "400", description = "잘못된 날짜 범위")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<AudioEventStatisticsDto> getAudioEventStatisticsForPeriod(
            @Parameter(description = "시작 날짜 (yyyy-MM-dd 형식)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd 형식)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        
        log.info("Getting audio event statistics for user: {} from {} to {}", 
                authentication.getName(), startDate, endDate);
        
        // 날짜 범위 유효성 검증
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("시작 날짜는 종료 날짜보다 이전이어야 합니다");
        }
        
        // 최대 1년 범위 제한
        if (startDate.plusYears(1).isBefore(endDate)) {
            throw new BusinessException("조회 가능한 최대 기간은 1년입니다");
        }
        
        Long userId = extractUserIdFromAuthentication(authentication);
        AudioEventStatisticsDto statistics = audioEventStatisticsService
                .calculateStatistics(userId, startDate, endDate);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 사용자의 최근 30일 오디오 이벤트 통계 조회
     */
    @GetMapping("/audio-events/statistics/recent")
    @Operation(summary = "최근 30일 오디오 이벤트 통계 조회",
               description = "사용자의 최근 30일간 오디오 이벤트 통계를 조회합니다. " +
                           "빠른 건강 상태 확인과 트렌드 파악에 유용합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "통계 조회 성공", 
                    content = @Content(schema = @Schema(implementation = AudioEventStatisticsDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<AudioEventStatisticsDto> getRecentAudioEventStatistics(
            Authentication authentication) {
        
        log.info("Getting recent 30-day audio event statistics for user: {}", authentication.getName());
        
        Long userId = extractUserIdFromAuthentication(authentication);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        AudioEventStatisticsDto statistics = audioEventStatisticsService
                .calculateStatistics(userId, startDate, endDate);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 오디오 이벤트 트렌드 분석 조회
     * 
     * 사용자의 수면 오디오 이벤트 데이터를 바탕으로 포괄적인 트렌드 분석을 제공합니다.
     * 시계열 분석, 패턴 감지, 예측 분석, 건강 지표 등을 포함합니다.
     * 
     * @param startDate 분석 시작일 (yyyy-MM-dd 형식)
     * @param endDate 분석 종료일 (yyyy-MM-dd 형식)
     * @param authentication 사용자 인증 정보
     * @return 트렌드 분석 결과 DTO
     */
    @GetMapping("/audio-events/trend-analysis")
    @Operation(summary = "수면 오디오 이벤트 트렌드 분석 및 인사이트 조회", description = "지정된 기간 동안의 수면 중 오디오 이벤트 발생 트렌드를 분석하고, 이를 기반으로 사용자에게 맞춤형 인사이트를 제공합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "트렌드 분석 및 인사이트 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SleepTrendAnalysisWithInsightsDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 날짜 범위 오류, 기간 너무 김)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SleepTrendAnalysisWithInsightsDto> getAudioEventTrendAnalysis(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "분석 시작 날짜 (YYYY-MM-DD)", required = true, example = "2024-06-01") @RequestParam LocalDate startDate,
            @Parameter(description = "분석 종료 날짜 (YYYY-MM-DD)", required = true, example = "2024-06-30") @RequestParam LocalDate endDate) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        
        User user = userDetails.getUser();

        // 1. 트렌드 분석 수행
        SleepAudioTrendAnalysisDto trendAnalysis = sleepAudioTrendAnalysisService.analyzeTrend(user.getId(), startDate, endDate);
        
        // 2. 분석 결과를 기반으로 인사이트 생성
        List<InsightDto> insights = insightGenerationService.generateInsights(trendAnalysis);
        
        // 3. 분석 결과와 인사이트를 통합하여 응답
        SleepTrendAnalysisWithInsightsDto response = new SleepTrendAnalysisWithInsightsDto(trendAnalysis, insights);
        
        return ResponseEntity.ok(response);
    }
} 