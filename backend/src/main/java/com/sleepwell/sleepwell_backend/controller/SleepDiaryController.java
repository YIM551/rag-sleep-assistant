package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.SleepDiaryRequestDto;
import com.sleepwell.sleepwell_backend.dto.SleepDiaryResponseDto;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorResponse;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.service.SleepDiaryService;
import com.sleepwell.sleepwell_backend.service.AISleepAnalysisService;
import com.sleepwell.sleepwell_backend.entity.SleepAnalysis;
import com.sleepwell.sleepwell_backend.entity.SleepDiary;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 수면일지 관리 REST API 컨트롤러
 *
 * 사용자의 주관적인 수면 일지 작성, 조회, 수정, 삭제 기능을 제공합니다.
 * 객관적인 수면 데이터(SleepRecord)와 함께 AI 분석에 활용되어
 * 개인화된 수면 개선 인사이트를 생성하는 데 사용됩니다.
 *
 * 주요 기능:
 * - 수면일지 CRUD 작업 (생성, 조회, 수정, 삭제)
 * - 기간별 수면일지 조회 및 통계
 * - 생활습관과 수면의 상관관계 분석
 * - 수면 패턴 트렌드 분석
 *
 * 보안:
 * - JWT 토큰 기반 인증 필수
 * - 사용자별 데이터 접근 제어
 * - 요청 데이터 유효성 검증
 *
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepDiaryService
 * @see SleepDiaryRequestDto
 * @see SleepDiaryResponseDto
 */
@RestController
@RequestMapping("/api/sleep/diaries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "수면일지", description = "수면일지 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class SleepDiaryController {

    private final SleepDiaryService sleepDiaryService;
    private final UserRepository userRepository;
    private final AISleepAnalysisService aiSleepAnalysisService;

    /**
     * 수면일지 생성
     */
    @PostMapping
    @Operation(summary = "수면일지 생성", description = "새로운 수면일지를 작성합니다. 하루에 하나의 수면일지만 작성 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "수면일지 생성 성공",
                    content = @Content(schema = @Schema(implementation = SleepDiaryResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "해당 날짜의 수면일지가 이미 존재",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SleepDiaryResponseDto> createSleepDiary(
            @Valid @RequestBody SleepDiaryRequestDto requestDto,
            Authentication authentication) {

        log.info("Creating sleep diary for user: {}, date: {}",
                authentication.getName(), requestDto.getDiaryDate());

        Long userId = extractUserIdFromAuthentication(authentication);
        SleepDiaryResponseDto responseDto = sleepDiaryService.createSleepDiary(userId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * 특정 수면일지 조회
     */
    @GetMapping("/{diaryId}")
    @Operation(summary = "특정 수면일지 조회", description = "ID로 특정 수면일지를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = SleepDiaryResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "수면일지를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<SleepDiaryResponseDto> getSleepDiary(
            @Parameter(description = "수면일지 ID") @PathVariable Long diaryId,
            Authentication authentication) {

        log.info("Getting sleep diary: {} for user: {}", diaryId, authentication.getName());

        Long userId = extractUserIdFromAuthentication(authentication);
        SleepDiaryResponseDto responseDto = sleepDiaryService.getSleepDiary(userId, diaryId);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 특정 날짜의 수면일지 조회
     */
    @GetMapping("/date/{date}")
    @Operation(summary = "특정 날짜 수면일지 조회", description = "특정 날짜의 수면일지를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = SleepDiaryResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "해당 날짜의 수면일지를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<SleepDiaryResponseDto> getSleepDiaryByDate(
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd 형식)")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        log.info("Getting sleep diary by date: {} for user: {}", date, authentication.getName());

        Long userId = extractUserIdFromAuthentication(authentication);
        SleepDiaryResponseDto responseDto = sleepDiaryService.getSleepDiaryByDate(userId, date);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 수면일지 목록 조회 (페이징)
     */
    @GetMapping
    @Operation(summary = "수면일지 목록 조회", description = "사용자의 수면일지 목록을 페이징으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<Page<SleepDiaryResponseDto>> getSleepDiaries(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준 필드") @RequestParam(defaultValue = "diaryDate") String sortBy,
            @Parameter(description = "정렬 방향 (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        log.info("Getting sleep diaries for user: {}, page: {}, size: {}",
                authentication.getName(), page, size);

        Long userId = extractUserIdFromAuthentication(authentication);
        Page<SleepDiaryResponseDto> responseDto = sleepDiaryService
                .getSleepDiaries(userId, page, size, sortBy, sortDir);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 기간별 수면일지 조회
     */
    @GetMapping("/range")
    @Operation(summary = "기간별 수면일지 조회", description = "지정된 기간 내의 수면일지를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 범위"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<Page<SleepDiaryResponseDto>> getSleepDiariesBetweenDates(
            @Parameter(description = "시작 날짜 (yyyy-MM-dd 형식)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd 형식)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(sort = "diaryDate", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        log.info("Getting sleep diaries for user: {} between {} and {}",
                authentication.getName(), startDate, endDate);

        Long userId = extractUserIdFromAuthentication(authentication);
        Page<SleepDiaryResponseDto> responseDto = sleepDiaryService
                .getSleepDiariesBetweenDates(userId, startDate, endDate, pageable);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 최근 30일 수면일지 조회
     */
    @GetMapping("/recent")
    @Operation(summary = "최근 30일 수면일지 조회", description = "최근 30일간의 수면일지를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<SleepDiaryResponseDto>> getRecentSleepDiaries(
            Authentication authentication) {

        log.info("Getting recent sleep diaries for user: {}", authentication.getName());

        Long userId = extractUserIdFromAuthentication(authentication);
        List<SleepDiaryResponseDto> responseDto = sleepDiaryService.getRecentSleepDiaries(userId);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 수면일지 수정
     */
    @PutMapping("/{diaryId}")
    @Operation(summary = "수면일지 수정", description = "기존 수면일지를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = SleepDiaryResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "수면일지를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<SleepDiaryResponseDto> updateSleepDiary(
            @Parameter(description = "수면일지 ID") @PathVariable Long diaryId,
            @RequestBody SleepDiaryRequestDto requestDto,  // 부분 업데이트이므로 @Valid 제거
            Authentication authentication) {

        log.info("Updating sleep diary: {} for user: {}", diaryId, authentication.getName());

        Long userId = extractUserIdFromAuthentication(authentication);
        SleepDiaryResponseDto responseDto = sleepDiaryService
                .updateSleepDiary(userId, diaryId, requestDto);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 수면일지 삭제
     */
    @DeleteMapping("/{diaryId}")
    @Operation(summary = "수면일지 삭제", description = "특정 수면일지를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "수면일지를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<Void> deleteSleepDiary(
            @Parameter(description = "삭제할 수면일지 ID") @PathVariable Long diaryId,
            Authentication authentication) {

        log.info("Deleting sleep diary: {} for user: {}", diaryId, authentication.getName());

        Long userId = extractUserIdFromAuthentication(authentication);
        sleepDiaryService.deleteSleepDiary(userId, diaryId);

        return ResponseEntity.noContent().build();
    }

    // ==================== 통계 및 분석 API ====================

    /**
     * 수면 품질 통계 조회
     */
    @GetMapping("/statistics/sleep-quality")
    @Operation(summary = "수면 품질 통계 조회",
               description = "지정된 기간 내 수면 품질 점수별 분포를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "통계 조회 성공")
    public ResponseEntity<Map<Integer, Long>> getSleepQualityStatistics(
            @Parameter(description = "시작 날짜 (yyyy-MM-dd 형식)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd 형식)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        log.info("Getting sleep quality statistics for user: {} between {} and {}",
                authentication.getName(), startDate, endDate);

        Long userId = extractUserIdFromAuthentication(authentication);
        Map<Integer, Long> statistics = sleepDiaryService
                .getSleepQualityStatistics(userId, startDate, endDate);

        return ResponseEntity.ok(statistics);
    }

    /**
     * 생활습관 영향 분석
     */
    @GetMapping("/statistics/lifestyle-impact")
    @Operation(summary = "생활습관이 수면에 미치는 영향 분석",
               description = "운동, 카페인, 알코올 등 생활습관이 수면 품질에 미치는 영향을 분석합니다.")
    @ApiResponse(responseCode = "200", description = "분석 조회 성공")
    public ResponseEntity<Map<String, Object>> getLifestyleImpactAnalysis(
            @Parameter(description = "시작 날짜 (yyyy-MM-dd 형식)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd 형식)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        log.info("Getting lifestyle impact analysis for user: {} between {} and {}",
                authentication.getName(), startDate, endDate);

        Long userId = extractUserIdFromAuthentication(authentication);
        Map<String, Object> analysis = sleepDiaryService
                .getLifestyleImpactAnalysis(userId, startDate, endDate);

        return ResponseEntity.ok(analysis);
    }

    // ==================== AI 분석 API ====================

    /**
     * 수면일지 통합 AI 분석
     */
    @PostMapping("/{diaryId}/analysis")
    @Operation(summary = "수면일지 통합 AI 분석",
               description = "수면일지와 수면 기록을 통합하여 AI 기반 맞춤형 수면 분석을 수행합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "분석 성공",
                    content = @Content(schema = @Schema(implementation = SleepAnalysis.class))),
            @ApiResponse(responseCode = "404", description = "수면일지를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "AI 분석 실행 중 오류 발생")
    })
    public CompletableFuture<ResponseEntity<SleepAnalysis>> analyzeIntegratedSleepDiary(
            @Parameter(description = "수면일지 ID") @PathVariable Long diaryId,
            Authentication authentication) {

        log.info("Starting integrated AI analysis for sleep diary: {} by user: {}",
                diaryId, authentication.getName());

        Long userId = extractUserIdFromAuthentication(authentication);

        // 수면일지 조회 및 권한 검증
        SleepDiaryResponseDto diaryResponse = sleepDiaryService.getSleepDiary(userId, diaryId);

        // DTO에서 Entity로 변환을 위한 간단한 빌더 패턴 사용
        // TODO: 실제 구현시에는 SleepDiaryService에서 Entity를 직접 반환하는 메서드 추가 검토

        return aiSleepAnalysisService.performIntegratedAnalysis(convertToEntity(diaryResponse))
                .thenApply(analysis -> {
                    log.info("Integrated AI analysis completed for diary: {}", diaryId);
                    return ResponseEntity.ok(analysis);
                })
                .exceptionally(throwable -> {
                    log.error("AI analysis failed for diary: {}", diaryId, throwable);
                    throw new BusinessException(
                            "AI 분석 중 오류가 발생했습니다: " + throwable.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "AI_ANALYSIS_FAILED");
                });
    }

    /**
     * 수면일지 기반 맞춤형 상담
     */
    @PostMapping("/consultation")
    @Operation(summary = "수면일지 기반 맞춤형 상담",
               description = "최근 수면일지 데이터를 기반으로 사용자 질문에 대한 맞춤형 상담을 제공합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상담 응답 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "상담 생성 중 오류 발생")
    })
    public ResponseEntity<Map<String, String>> generateDiaryBasedConsultation(
            @Parameter(description = "사용자 질문") @RequestParam String userMessage,
            @Parameter(description = "분석할 최근 일지 수 (기본값: 7일)")
            @RequestParam(defaultValue = "7") int recentDays,
            Authentication authentication) {

        log.info("Generating diary-based consultation for user: {} with message: {}",
                authentication.getName(), userMessage);

        Long userId = extractUserIdFromAuthentication(authentication);

        // 최근 수면일지 조회
        List<SleepDiaryResponseDto> recentDiaries = sleepDiaryService.getRecentSleepDiaries(userId);

        if (recentDiaries.isEmpty()) {
            throw new BusinessException(
                    "상담을 위한 수면일지 데이터가 없습니다. 수면일지를 먼저 작성해 주세요.",
                    HttpStatus.BAD_REQUEST,
                    "NO_DIARY_DATA");
        }

        // 가장 최근 일지와 주간 일지 분리
        SleepDiaryResponseDto recentDiary = recentDiaries.get(0);
        List<SleepDiaryResponseDto> weeklyDiaries = recentDiaries.stream()
                .limit(Math.min(recentDays, recentDiaries.size()))
                .toList();

        try {
            String consultation = aiSleepAnalysisService.generateDiaryBasedConsultation(
                    userMessage,
                    convertToEntity(recentDiary),
                    weeklyDiaries.stream().map(this::convertToEntity).toList());

            Map<String, String> response = new HashMap<>();
            response.put("consultation", consultation);
            response.put("basedOnDiaries", String.valueOf(weeklyDiaries.size()));
            response.put("analysisDate", LocalDate.now().toString());

            log.info("Diary-based consultation generated successfully for user: {}",
                    authentication.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to generate diary-based consultation for user: {}",
                    authentication.getName(), e);
            throw new BusinessException(
                    "상담 생성 중 오류가 발생했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "CONSULTATION_GENERATION_FAILED");
        }
    }

    /**
     * 생활습관 트렌드 AI 분석
     */
    @GetMapping("/trends/lifestyle")
    @Operation(summary = "생활습관 트렌드 AI 분석",
               description = "지정된 기간의 수면일지를 분석하여 생활습관 트렌드와 수면에 미치는 영향을 AI로 분석합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "트렌드 분석 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 범위"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "트렌드 분석 중 오류 발생")
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> analyzeLifestyleTrends(
            @Parameter(description = "시작 날짜 (yyyy-MM-dd 형식)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd 형식)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        log.info("Starting lifestyle trends analysis for user: {} between {} and {}",
                authentication.getName(), startDate, endDate);

        Long userId = extractUserIdFromAuthentication(authentication);

        // 날짜 범위 검증
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(
                    "시작 날짜는 종료 날짜보다 이전이어야 합니다",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE");
        }

        if (startDate.isBefore(LocalDate.now().minusMonths(6))) {
            throw new BusinessException(
                    "분석 기간은 최근 6개월 이내로 제한됩니다",
                    HttpStatus.BAD_REQUEST,
                    "DATE_RANGE_TOO_OLD");
        }

        // 기간별 수면일지 조회
        Page<SleepDiaryResponseDto> diaryPage = sleepDiaryService
                .getSleepDiariesBetweenDates(userId, startDate, endDate,
                        org.springframework.data.domain.PageRequest.of(0, 100));

        List<SleepDiaryResponseDto> diaries = diaryPage.getContent();

        if (diaries.isEmpty()) {
            throw new BusinessException(
                    "해당 기간에 수면일지 데이터가 없습니다",
                    HttpStatus.BAD_REQUEST,
                    "NO_DIARY_DATA_IN_PERIOD");
        }

        return aiSleepAnalysisService.analyzeLifestyleTrends(
                diaries.stream().map(this::convertToEntity).toList())
                .thenApply(trends -> {
                    log.info("Lifestyle trends analysis completed for user: {} with {} diaries",
                            authentication.getName(), diaries.size());

                    // 메타데이터 추가
                    trends.put("analysisMetadata", Map.of(
                            "startDate", startDate.toString(),
                            "endDate", endDate.toString(),
                            "totalDiaries", diaries.size(),
                            "analysisDate", LocalDate.now().toString()
                    ));

                    return ResponseEntity.ok(trends);
                })
                .exceptionally(throwable -> {
                    log.error("Lifestyle trends analysis failed for user: {}",
                            authentication.getName(), throwable);
                    throw new BusinessException(
                            "생활습관 트렌드 분석 중 오류가 발생했습니다: " + throwable.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "TRENDS_ANALYSIS_FAILED");
                });
    }

    // ==================== 헬퍼 메서드 ====================

    /**
     * Authentication에서 사용자 ID 추출
     */
    private Long extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("인증 정보가 존재하지 않습니다",
                    HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED");
        }

        String userEmail = authentication.getName();
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new BusinessException("사용자 이메일이 존재하지 않습니다",
                    HttpStatus.UNAUTHORIZED, "USER_EMAIL_MISSING");
        }

        // 이메일로 사용자 조회
        return userRepository.findByEmail(userEmail)
                .map(User::getId)
                .orElseThrow(() -> new BusinessException(
                        "사용자를 찾을 수 없습니다: " + userEmail,
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
    }

    /**
     * SleepDiaryResponseDto를 SleepDiary Entity로 변환
     * AI 분석 서비스 호출을 위한 임시 변환 메서드
     * TODO: 향후 SleepDiaryService에서 Entity를 직접 반환하는 메서드 추가 고려
     */
    private SleepDiary convertToEntity(SleepDiaryResponseDto dto) {
        if (dto == null) {
            throw new BusinessException(
                    "변환할 수면일지 데이터가 없습니다",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "CONVERSION_ERROR");
        }

        return SleepDiary.builder()
                .id(dto.getId())
                .diaryDate(dto.getDiaryDate())
                .napMinutes(dto.getNapMinutes())
                .bedTime(dto.getBedTime())
                .perceivedSleepMinutes(dto.getPerceivedSleepMinutes())
                .awakeningCount(dto.getAwakeningCount())
                .totalAwakeMinutes(dto.getTotalAwakeMinutes())
                .wakeUpTime(dto.getWakeUpTime())
                .caffeineMg(dto.getCaffeineMg())
                .alcoholMl(dto.getAlcoholMl())
                .medicationMg(dto.getMedicationMg())
                .didExercise(dto.getDidExercise())
                .diaryNotes(dto.getDiaryNotes())
                .subjectiveSleepQuality(dto.getSubjectiveSleepQuality())
                .morningConditionScore(dto.getMorningConditionScore())
                .stressLevel(dto.getStressLevel())
                .build();
    }
}