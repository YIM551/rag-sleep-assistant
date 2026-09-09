package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.SleepStageRequestDto;
import com.sleepwell.sleepwell_backend.dto.SleepStageResponseDto;
import com.sleepwell.sleepwell_backend.enums.SleepStageType;
import com.sleepwell.sleepwell_backend.exception.ErrorResponse;
import com.sleepwell.sleepwell_backend.service.SleepStageService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 수면 단계 관리 REST API 컨트롤러
 *
 * 수면 기록 내의 상세한 수면 단계 정보를 관리합니다.
 * 웨어러블 기기에서 수집된 수면 단계 데이터(DEEP, LIGHT, REM, AWAKE)를
 * 생성, 조회, 수정, 삭제하고 통계를 제공합니다.
 *
 * 주요 기능:
 * - 수면 단계 CRUD 작업
 * - 수면 단계별 통계 및 분석
 * - 수면 주기 분석
 * - 회복성 수면 분석
 *
 * 보안:
 * - JWT 토큰 기반 인증 필수
 * - 수면 기록 소유자만 접근 가능
 *
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepStageService
 */
@RestController
@RequestMapping("/api/sleep/stages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "수면 단계", description = "수면 단계 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class SleepStageController {

    private final SleepStageService sleepStageService;

    /**
     * 수면 단계 생성
     */
    @PostMapping("/records/{sleepRecordId}")
    @Operation(summary = "수면 단계 생성",
               description = "특정 수면 기록에 새로운 수면 단계를 추가합니다. 웨어러블 기기 데이터 입력에 사용됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "수면 단계 생성 성공",
                    content = @Content(schema = @Schema(implementation = SleepStageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "수면 기록을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "시간대가 겹치는 수면 단계 존재",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SleepStageResponseDto> createSleepStage(
            @Parameter(description = "수면 기록 ID", required = true)
            @PathVariable Long sleepRecordId,
            @Valid @RequestBody SleepStageRequestDto requestDto,
            Authentication authentication) {

        log.info("Creating sleep stage for record: {}, type: {}",
                sleepRecordId, requestDto.getStageType());

        SleepStageResponseDto responseDto = sleepStageService.createSleepStage(sleepRecordId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * 특정 수면 기록의 모든 수면 단계 조회
     */
    @GetMapping("/records/{sleepRecordId}")
    @Operation(summary = "수면 단계 목록 조회",
               description = "특정 수면 기록의 모든 수면 단계를 시간 순서대로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "수면 기록을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<SleepStageResponseDto>> getSleepStages(
            @Parameter(description = "수면 기록 ID", required = true)
            @PathVariable Long sleepRecordId,
            Authentication authentication) {

        log.debug("Fetching sleep stages for record: {}", sleepRecordId);

        List<SleepStageResponseDto> stages = sleepStageService.getSleepStagesBySleepRecord(sleepRecordId);

        return ResponseEntity.ok(stages);
    }

    /**
     * 특정 타입의 수면 단계만 조회
     */
    @GetMapping("/records/{sleepRecordId}/type/{stageType}")
    @Operation(summary = "특정 타입 수면 단계 조회",
               description = "특정 수면 기록에서 지정한 타입(DEEP, LIGHT, REM, AWAKE)의 수면 단계만 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 수면 단계 타입",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<SleepStageResponseDto>> getSleepStagesByType(
            @Parameter(description = "수면 기록 ID", required = true)
            @PathVariable Long sleepRecordId,
            @Parameter(description = "수면 단계 타입 (DEEP, LIGHT, REM, AWAKE)", required = true)
            @PathVariable SleepStageType stageType,
            Authentication authentication) {

        log.debug("Fetching {} stages for record: {}", stageType, sleepRecordId);

        List<SleepStageResponseDto> stages = sleepStageService.getSleepStagesByType(sleepRecordId, stageType);

        return ResponseEntity.ok(stages);
    }

    /**
     * 수면 단계 수정
     */
    @PutMapping("/{stageId}")
    @Operation(summary = "수면 단계 수정",
               description = "기존 수면 단계의 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = SleepStageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "수면 단계를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SleepStageResponseDto> updateSleepStage(
            @Parameter(description = "수면 단계 ID", required = true)
            @PathVariable Long stageId,
            @Valid @RequestBody SleepStageRequestDto requestDto,
            Authentication authentication) {

        log.info("Updating sleep stage: {}", stageId);

        SleepStageResponseDto responseDto = sleepStageService.updateSleepStage(stageId, requestDto);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 수면 단계 삭제
     */
    @DeleteMapping("/{stageId}")
    @Operation(summary = "수면 단계 삭제",
               description = "특정 수면 단계를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "수면 단계를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteSleepStage(
            @Parameter(description = "수면 단계 ID", required = true)
            @PathVariable Long stageId,
            Authentication authentication) {

        log.info("Deleting sleep stage: {}", stageId);

        sleepStageService.deleteSleepStage(stageId);

        return ResponseEntity.noContent().build();
    }

    // === 통계 및 분석 API ===

    /**
     * 수면 단계별 통계 조회
     */
    @GetMapping("/records/{sleepRecordId}/statistics")
    @Operation(summary = "수면 단계 통계",
               description = "특정 수면 기록의 단계별 총 시간 통계를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<Map<SleepStageType, Integer>> getSleepStageStatistics(
            @Parameter(description = "수면 기록 ID", required = true)
            @PathVariable Long sleepRecordId,
            Authentication authentication) {

        log.debug("Fetching sleep stage statistics for record: {}", sleepRecordId);

        Map<SleepStageType, Integer> statistics = sleepStageService.getSleepStageStatistics(sleepRecordId);

        return ResponseEntity.ok(statistics);
    }

    /**
     * 회복성 수면 단계 조회 (DEEP + REM)
     */
    @GetMapping("/records/{sleepRecordId}/restorative")
    @Operation(summary = "회복성 수면 단계 조회",
               description = "수면의 질에 긍정적인 영향을 주는 깊은 수면과 REM 수면 단계만 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<SleepStageResponseDto>> getRestorativeSleepStages(
            @Parameter(description = "수면 기록 ID", required = true)
            @PathVariable Long sleepRecordId,
            Authentication authentication) {

        log.debug("Fetching restorative sleep stages for record: {}", sleepRecordId);

        List<SleepStageResponseDto> stages = sleepStageService.getRestorativeSleepStages(sleepRecordId);

        return ResponseEntity.ok(stages);
    }

    /**
     * 각성 단계 조회
     */
    @GetMapping("/records/{sleepRecordId}/awake")
    @Operation(summary = "각성 단계 조회",
               description = "수면 중 깨어있던 시간대를 조회합니다. 수면 방해 패턴 분석에 사용됩니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<SleepStageResponseDto>> getAwakeStages(
            @Parameter(description = "수면 기록 ID", required = true)
            @PathVariable Long sleepRecordId,
            Authentication authentication) {

        log.debug("Fetching awake stages for record: {}", sleepRecordId);

        List<SleepStageResponseDto> stages = sleepStageService.getAwakeStages(sleepRecordId);

        return ResponseEntity.ok(stages);
    }

    /**
     * 수면 주기 개수 조회
     */
    @GetMapping("/records/{sleepRecordId}/cycles")
    @Operation(summary = "수면 주기 개수 조회",
               description = "REM 수면 횟수를 기준으로 수면 주기 개수를 계산합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<Map<String, Object>> getSleepCycles(
            @Parameter(description = "수면 기록 ID", required = true)
            @PathVariable Long sleepRecordId,
            Authentication authentication) {

        log.debug("Counting sleep cycles for record: {}", sleepRecordId);

        long cycleCount = sleepStageService.countSleepCycles(sleepRecordId);
        Integer remLatency = sleepStageService.calculateRemLatency(sleepRecordId);

        Map<String, Object> result = Map.of(
                "cycleCount", cycleCount,
                "remLatencyMinutes", remLatency != null ? remLatency : 0,
                "hasRemSleep", remLatency != null
        );

        return ResponseEntity.ok(result);
    }
}
