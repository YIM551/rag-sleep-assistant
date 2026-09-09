package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.PlatformSleepDataDto;
import com.sleepwell.sleepwell_backend.entity.AnalysisExecutionJob;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import com.sleepwell.sleepwell_backend.service.SleepAnalysisExecutionService;
import com.sleepwell.sleepwell_backend.service.CustomUserDetailsService;
import com.sleepwell.sleepwell_backend.service.PlatformSleepDataIntegrationService;
import com.sleepwell.sleepwell_backend.service.AISleepAnalysisService;
import com.sleepwell.sleepwell_backend.repository.SleepDiaryRepository;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.repository.SleepAnalysisRepository;
import com.sleepwell.sleepwell_backend.entity.SleepDiary;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.SleepAnalysis;
import com.sleepwell.sleepwell_backend.dto.analysis.AnalysisExecutionJobResponseDto;
import com.sleepwell.sleepwell_backend.dto.analysis.AnalysisExecutionJobListDto;
import java.util.concurrent.CompletableFuture;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.dto.ScheduleAnalysisRequestDto;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 수면 분석 실행 컨트롤러
 * 
 * 예약 분석 실행 시스템의 REST API를 제공합니다.
 * 플랫폼 데이터 동기화 트리거 및 분석 작업 관리 기능을 담당합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(name = "수면 분석 실행", description = "수면 분석 작업 예약 및 실행 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class AnalysisExecutionController {

    private final SleepAnalysisExecutionService analysisExecutionService;
    private final CustomUserDetailsService userDetailsService;
    private final PlatformSleepDataIntegrationService platformDataIntegrationService;
    private final AISleepAnalysisService aiSleepAnalysisService;
    private final SleepDiaryRepository sleepDiaryRepository;
    private final SleepRecordRepository sleepRecordRepository;
    private final SleepAnalysisRepository sleepAnalysisRepository;

    /**
     * 새로운 플랫폼 데이터에 대한 분석 작업을 스케줄링합니다.
     * 
     * @param platformSource 플랫폼 소스
     * @param platformDataId 플랫폼 데이터 ID
     * @param platformData 플랫폼 수면 데이터
     * @param priority 작업 우선순위 (1-5, 선택적)
     * @param userDetails 현재 사용자 정보
     * @return 생성된 분석 작업 정보
     */
    @Operation(
        summary = "수면 분석 작업 스케줄링", 
        description = "플랫폼 데이터에 대한 새로운 분석 작업을 예약하고 큐에 추가합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "분석 작업 스케줄링 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AnalysisExecutionJobResponseDto.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/schedule")
    public ResponseEntity<AnalysisExecutionJobResponseDto> scheduleAnalysis(
            @Valid @RequestBody ScheduleAnalysisRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        
        User user = userDetails.getUser();
        
        log.info("분석 작업 스케줄링 요청 - 사용자: {}, 플랫폼: {}", 
                user.getId(), request.getPlatformSource());

        Long userId = user.getId();
        
        // 방어적 프로그래밍: null 체크와 기본값 설정
        String platformDataId = "ANALYSIS-" + System.currentTimeMillis();
        
        // NPE 방지: 안전한 비교
        Integer priority = "HIGH".equals(request.getPriority()) ? 1 : 3;
        
        // 기본값 설정으로 NPE 방지
        LocalDate targetDate = request.getTargetDate() != null ? 
                request.getTargetDate() : LocalDate.now();
        
        // 임시 플랫폼 데이터 생성 - 모든 필수 필드에 기본값 설정
        PlatformSleepDataDto platformData = PlatformSleepDataDto.builder()
                .source(request.getPlatformSource())
                .sleepStartTime(targetDate.atTime(23, 0))
                .sleepEndTime(targetDate.plusDays(1).atTime(7, 0))
                .totalSleepMinutes(480)
                .collectedAt(LocalDateTime.now()) // NPE 방지: 명시적 설정
                .dataQualityScore(80) // 기본 품질 점수 (Integer)
                .build();
        
        AnalysisExecutionJob job = analysisExecutionService.scheduleAnalysis(
                userId, request.getPlatformSource(), platformDataId, platformData, priority);
        
        return ResponseEntity.ok(AnalysisExecutionJobResponseDto.from(job));
    }

    /**
     * 사용자의 분석 작업 히스토리를 조회합니다.
     * 
     * @param userDetails 현재 사용자 정보
     * @return 사용자의 분석 작업 목록
     */
    @Operation(
        summary = "분석 작업 히스토리 조회", 
        description = "현재 사용자의 모든 분석 작업 히스토리를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "분석 작업 히스토리 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = List.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AnalysisExecutionJobListDto>> getAnalysisHistory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        List<AnalysisExecutionJob> jobs = analysisExecutionService.getUserAnalysisHistory(user.getId());
        List<AnalysisExecutionJobListDto> dtos = jobs.stream()
                .map(AnalysisExecutionJobListDto::from)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * 시스템의 작업 큐 상태를 조회합니다.
     * 관리자 또는 모니터링 용도로 사용됩니다.
     * 
     * @return 작업 큐 상태 정보
     */
    @Operation(
        summary = "작업 큐 상태 조회", 
        description = "현재 시스템의 분석 작업 큐 상태를 조회합니다. 대기 중, 처리 중, 재시도 가능한 작업 수를 확인할 수 있습니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "작업 큐 상태 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @GetMapping("/queue-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        
        log.debug("작업 큐 상태 조회 요청");

        List<AnalysisExecutionJob> queuedJobs = analysisExecutionService.getQueuedJobs();
        List<AnalysisExecutionJob> processingJobs = analysisExecutionService.getProcessingJobs();
        List<AnalysisExecutionJob> retryableJobs = analysisExecutionService.getRetryableJobs();
        
        Map<String, Object> queueStatus = Map.of(
                "queuedJobs", queuedJobs.size(),
                "processingJobs", processingJobs.size(),
                "retryableJobs", retryableJobs.size(),
                "queuedJobsList", queuedJobs.stream()
                        .map(AnalysisExecutionJobListDto::from)
                        .collect(java.util.stream.Collectors.toList()),
                "processingJobsList", processingJobs.stream()
                        .map(AnalysisExecutionJobListDto::from)
                        .collect(java.util.stream.Collectors.toList()),
                "retryableJobsList", retryableJobs.stream()
                        .map(AnalysisExecutionJobListDto::from)
                        .collect(java.util.stream.Collectors.toList())
        );
        
        return ResponseEntity.ok(queueStatus);
    }

    /**
     * 특정 분석 작업의 상세 정보를 조회합니다.
     * 
     * @param jobId 작업 ID
     * @param userDetails 현재 사용자 정보
     * @return 분석 작업 정보
     */
    @Operation(
        summary = "분석 작업 상세 조회", 
        description = "특정 분석 작업의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "분석 작업 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AnalysisExecutionJobResponseDto.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "작업을 찾을 수 없음")
    })
    @GetMapping("/{jobId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AnalysisExecutionJobResponseDto> getAnalysisJob(
            @Parameter(description = "조회할 작업 ID") @PathVariable Long jobId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        AnalysisExecutionJob job = analysisExecutionService.getAnalysisJobByIdAndUserId(jobId, user.getId());
        return ResponseEntity.ok(AnalysisExecutionJobResponseDto.from(job));
    }

    /**
     * 플랫폼 데이터 동기화를 수동으로 트리거합니다.
     * 개발 및 테스트 용도로 사용됩니다.
     * 
     * @param platformSource 플랫폼 소스
     * @param userDetails 현재 사용자 정보
     * @return 트리거 결과
     */
    @Operation(
        summary = "플랫폼 데이터 동기화 트리거", 
        description = "특정 플랫폼의 데이터 동기화를 수동으로 트리거합니다. 개발 및 테스트 용도로 사용됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "데이터 동기화 트리거 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 플랫폼 소스"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/trigger-sync")
    public ResponseEntity<Map<String, String>> triggerDataSync(
            @Parameter(description = "플랫폼 소스 (SAMSUNG_HEALTH, APPLE_HEALTH, etc.)") @RequestParam WearableSource platformSource,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        
        User user = userDetails.getUser();
        
        log.info("수동 데이터 동기화 트리거 - 사용자: {}, 플랫폼: {}", 
                user.getId(), platformSource);
        
        // 플랫폼별 데이터 동기화 로직
        // 실제 구현에서는 외부 API 호출이나 백그라운드 작업을 통해 데이터를 가져옴
        log.info("데이터 동기화 트리거 완료 - 사용자 ID: {}, 플랫폼: {}", user.getId(), platformSource);
        
        Map<String, String> result = Map.of(
                "status", "triggered",
                "message", "데이터 동기화가 시작되었습니다.",
                "platform", platformSource.toString(),
                "userId", user.getId().toString()
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * 실패한 분석 작업들을 재시도합니다.
     * 관리자 권한이 필요한 작업입니다.
     */
    @Operation(
        summary = "실패한 분석 작업 재시도",
        description = "재시도 가능한 실패한 분석 작업들을 재시도합니다. 관리자 권한이 필요합니다."
    )
    @ApiResponses({
    @ApiResponse(
        responseCode = "200", 
            description = "재시도 요청 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Map.class)
        )
        ),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> retryFailedJobs() {
        int retryableJobsCount = analysisExecutionService.retryFailedJobs();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "실패한 작업들의 재시도가 시작되었습니다.");
        response.put("status", "initiated");
        response.put("retryableJobsCount", retryableJobsCount);
        return ResponseEntity.ok(response);
    }

    /**
     * 수면 기록과 수면 일지를 통합하여 AI 분석을 수행합니다.
     *
     * @param diaryId 수면 일지 ID
     * @param userDetails 현재 사용자 정보
     * @return 분석 시작 응답 (비동기)
     */
    @Operation(
        summary = "통합 수면 분석 (수면 기록 + 수면 일지)",
        description = "수면 기록 데이터와 수면 일지를 함께 분석하여 객관적 데이터와 주관적 경험을 결합한 종합 분석을 제공합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "202",
            description = "분석 시작됨 (비동기 처리)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (일치하는 수면 기록 없음)"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "수면 일지를 찾을 수 없음")
    })
    @PostMapping("/diary-integrated")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> analyzeDiaryIntegrated(
            @Parameter(description = "수면 일지 ID") @RequestParam Long diaryId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userDetails.getUser();

        log.info("통합 수면 분석 요청 - 사용자: {}, 일지 ID: {}", user.getId(), diaryId);

        // 수면 일지 조회
        SleepDiary sleepDiary = sleepDiaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException("수면 일지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 권한 확인
        if (!sleepDiary.getUser().getId().equals(user.getId())) {
            throw new BusinessException("해당 수면 일지에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        // 동일 날짜의 수면 기록 조회
        SleepRecord sleepRecord = sleepRecordRepository.findByUserAndRecordDate(user, sleepDiary.getDiaryDate())
                .orElseThrow(() -> new BusinessException(
                    "일치하는 수면 기록을 찾을 수 없습니다. 통합 분석은 수면 기록과 일지가 모두 있어야 합니다.",
                    HttpStatus.BAD_REQUEST));

        // 비동기 AI 분석 시작
        CompletableFuture<SleepAnalysis> analysisFuture = aiSleepAnalysisService
                .performIntegratedAnalysis(sleepRecord, sleepDiary);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "processing");
        response.put("message", "통합 분석이 시작되었습니다. 결과는 잠시 후 조회할 수 있습니다.");
        response.put("diaryId", diaryId);
        response.put("sleepRecordId", sleepRecord.getId());
        response.put("analysisType", "INTEGRATED");

        log.info("통합 분석 시작 완료 - 일지 ID: {}, 수면 기록 ID: {}", diaryId, sleepRecord.getId());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * 수면 일지만을 사용하여 AI 분석을 수행합니다.
     * 수면 기록이 없는 경우 사용할 수 있습니다.
     *
     * @param diaryId 수면 일지 ID
     * @param userDetails 현재 사용자 정보
     * @return 분석 시작 응답 (비동기)
     */
    @Operation(
        summary = "수면 일지 단독 분석",
        description = "수면 일지의 주관적 데이터만을 사용하여 AI 분석을 수행합니다. 수면 기록이 없는 경우에 사용됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "202",
            description = "분석 시작됨 (비동기 처리)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "수면 일지를 찾을 수 없음")
    })
    @PostMapping("/diary-only")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> analyzeDiaryOnly(
            @Parameter(description = "수면 일지 ID") @RequestParam Long diaryId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userDetails.getUser();

        log.info("수면 일지 단독 분석 요청 - 사용자: {}, 일지 ID: {}", user.getId(), diaryId);

        // 수면 일지 조회
        SleepDiary sleepDiary = sleepDiaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException("수면 일지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 권한 확인
        if (!sleepDiary.getUser().getId().equals(user.getId())) {
            throw new BusinessException("해당 수면 일지에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        // 비동기 AI 분석 시작
        CompletableFuture<SleepAnalysis> analysisFuture = aiSleepAnalysisService
                .performDiaryOnlyAnalysis(sleepDiary);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "processing");
        response.put("message", "수면 일지 분석이 시작되었습니다. 결과는 잠시 후 조회할 수 있습니다.");
        response.put("diaryId", diaryId);
        response.put("analysisType", "DIARY_ONLY");

        log.info("수면 일지 분석 시작 완료 - 일지 ID: {}", diaryId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * 특정 수면 일지의 분석 결과를 조회합니다.
     *
     * @param diaryId 수면 일지 ID
     * @param userDetails 현재 사용자 정보
     * @return 분석 결과 또는 처리 중 상태
     */
    @Operation(
        summary = "수면 일지 분석 결과 조회",
        description = "특정 수면 일지의 AI 분석 결과를 조회합니다. 분석이 완료되지 않은 경우 처리 중 상태를 반환합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "분석 결과 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SleepAnalysis.class)
            )
        ),
        @ApiResponse(responseCode = "202", description = "분석 처리 중"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "수면 일지 또는 분석 결과를 찾을 수 없음")
    })
    @GetMapping("/diary/{diaryId}/result")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getDiaryAnalysisResult(
            @Parameter(description = "수면 일지 ID") @PathVariable Long diaryId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userDetails.getUser();

        log.info("수면 일지 분석 결과 조회 - 사용자: {}, 일지 ID: {}", user.getId(), diaryId);

        // 수면 일지 조회
        SleepDiary sleepDiary = sleepDiaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException("수면 일지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 권한 확인
        if (!sleepDiary.getUser().getId().equals(user.getId())) {
            throw new BusinessException("해당 수면 일지에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        // 동일 날짜의 수면 기록 조회 (선택적)
        var sleepRecordOptional = sleepRecordRepository.findByUserAndRecordDate(user, sleepDiary.getDiaryDate());

        Map<String, Object> response = new HashMap<>();

        if (sleepRecordOptional.isPresent()) {
            SleepRecord sleepRecord = sleepRecordOptional.get();

            // 해당 수면 기록의 분석 결과 조회
            Optional<SleepAnalysis> analysisOptional = sleepAnalysisRepository.findBySleepRecord(sleepRecord);

            if (analysisOptional.isPresent()) {
                SleepAnalysis analysis = analysisOptional.get();
                response.put("status", "completed");
                response.put("analysisType", "INTEGRATED");
                response.put("analysis", analysis);
                response.put("sleepRecordId", sleepRecord.getId());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "processing");
                response.put("message", "분석이 진행 중입니다.");
                response.put("analysisType", "INTEGRATED");
                response.put("sleepRecordId", sleepRecord.getId());
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            }
        } else {
            // 수면 기록이 없는 경우 - 일지 단독 분석 결과 확인
            // 현재 SleepAnalysis는 SleepRecord와 ManyToOne 관계이므로 일지 단독 분석 결과는 별도 처리 필요
            response.put("status", "no_record");
            response.put("message", "수면 기록이 없어 통합 분석을 수행할 수 없습니다. 일지 단독 분석을 요청해주세요.");
            response.put("availableAnalysis", "DIARY_ONLY");
            return ResponseEntity.ok(response);
        }
    }
} 