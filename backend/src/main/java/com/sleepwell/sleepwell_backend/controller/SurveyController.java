package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.*;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import com.sleepwell.sleepwell_backend.service.BDIService;
import com.sleepwell.sleepwell_backend.service.ESSService;
import com.sleepwell.sleepwell_backend.service.ISIService;
import com.sleepwell.sleepwell_backend.service.PSQIService;
import com.sleepwell.sleepwell_backend.service.SISService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 수면 설문 컨트롤러
 * ISI, ESS, PSQI, BDI-II, SIS 설문 응답을 처리
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Survey", description = "수면 설문 API")
@SecurityRequirement(name = "bearerAuth")
public class SurveyController {

    private final ISIService isiService;
    private final ESSService essService;
    private final PSQIService psqiService;
    private final BDIService bdiService;
    private final SISService sisService;

    // ==================== ISI (Insomnia Severity Index) ====================

    @PostMapping("/isi")
    @Operation(summary = "ISI 설문 제출", description = "불면증 심각도 지수(ISI) 설문 응답을 제출합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "설문 응답이 성공적으로 저장되었습니다",
                    content = @Content(schema = @Schema(implementation = ISIResponseDto.class))),
        @ApiResponse(responseCode = "409", description = "이미 응답한 설문입니다"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    public ResponseEntity<ISIResponseDto> submitISI(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ISIResponseRequestDto requestDto) {

        Long userId = userDetails.getId();
        log.info("ISI 설문 제출 요청: 사용자 ID {}", userId);

        ISIResponseDto response = isiService.submitResponse(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/isi/latest")
    @Operation(summary = "최신 ISI 응답 조회", description = "사용자의 최신 ISI 설문 응답을 조회합니다")
    public ResponseEntity<ISIResponseDto> getLatestISI(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        ISIResponseDto response = isiService.getLatestResponse(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/isi/history")
    @Operation(summary = "ISI 응답 이력 조회", description = "사용자의 모든 ISI 설문 응답 이력을 조회합니다")
    public ResponseEntity<List<ISIResponseDto>> getISIHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        List<ISIResponseDto> responses = isiService.getAllResponses(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/isi/recent")
    @Operation(summary = "최근 N개 ISI 응답 조회", description = "사용자의 최근 N개 ISI 설문 응답을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "최근 응답 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 limit 값")
    })
    public ResponseEntity<List<ISIResponseDto>> getRecentISI(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "5") int limit) {

        Long userId = userDetails.getId();
        List<ISIResponseDto> responses = isiService.getRecentResponses(userId, limit);
        return ResponseEntity.ok(responses);
    }

    // ==================== ESS (Epworth Sleepiness Scale) ====================

    @PostMapping("/ess")
    @Operation(summary = "ESS 설문 제출", description = "엡워스 졸림 척도(ESS) 설문 응답을 제출합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "설문 응답이 성공적으로 저장되었습니다",
                    content = @Content(schema = @Schema(implementation = ESSResponseDto.class))),
        @ApiResponse(responseCode = "409", description = "이미 응답한 설문입니다"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    public ResponseEntity<ESSResponseDto> submitESS(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ESSResponseRequestDto requestDto) {

        Long userId = userDetails.getId();
        log.info("ESS 설문 제출 요청: 사용자 ID {}", userId);

        ESSResponseDto response = essService.submitResponse(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/ess/latest")
    @Operation(summary = "최신 ESS 응답 조회", description = "사용자의 최신 ESS 설문 응답을 조회합니다")
    public ResponseEntity<ESSResponseDto> getLatestESS(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        ESSResponseDto response = essService.getLatestResponse(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ess/history")
    @Operation(summary = "ESS 응답 이력 조회", description = "사용자의 모든 ESS 설문 응답 이력을 조회합니다")
    public ResponseEntity<List<ESSResponseDto>> getESSHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        List<ESSResponseDto> responses = essService.getAllResponses(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/ess/recent")
    @Operation(summary = "최근 N개 ESS 응답 조회", description = "사용자의 최근 N개 ESS 설문 응답을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "최근 응답 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 limit 값")
    })
    public ResponseEntity<List<ESSResponseDto>> getRecentESS(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "5") int limit) {

        Long userId = userDetails.getId();
        List<ESSResponseDto> responses = essService.getRecentResponses(userId, limit);
        return ResponseEntity.ok(responses);
    }

    // ==================== PSQI (Pittsburgh Sleep Quality Index) ====================

    @PostMapping("/psqi")
    @Operation(summary = "PSQI 설문 제출", description = "피츠버그 수면의 질 지수(PSQI) 설문 응답을 제출합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "설문 응답이 성공적으로 저장되었습니다",
                    content = @Content(schema = @Schema(implementation = PSQIResponseDto.class))),
        @ApiResponse(responseCode = "409", description = "이미 응답한 설문입니다"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    public ResponseEntity<PSQIResponseDto> submitPSQI(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PSQIResponseRequestDto requestDto) {

        Long userId = userDetails.getId();
        log.info("PSQI 설문 제출 요청: 사용자 ID {}", userId);

        PSQIResponseDto response = psqiService.submitResponse(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/psqi/latest")
    @Operation(summary = "최신 PSQI 응답 조회", description = "사용자의 최신 PSQI 설문 응답을 조회합니다")
    public ResponseEntity<PSQIResponseDto> getLatestPSQI(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        PSQIResponseDto response = psqiService.getLatestResponse(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/psqi/history")
    @Operation(summary = "PSQI 응답 이력 조회", description = "사용자의 모든 PSQI 설문 응답 이력을 조회합니다")
    public ResponseEntity<List<PSQIResponseDto>> getPSQIHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        List<PSQIResponseDto> responses = psqiService.getAllResponses(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/psqi/recent")
    @Operation(summary = "최근 N개 PSQI 응답 조회", description = "사용자의 최근 N개 PSQI 설문 응답을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "최근 응답 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 limit 값")
    })
    public ResponseEntity<List<PSQIResponseDto>> getRecentPSQI(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "5") int limit) {

        Long userId = userDetails.getId();
        List<PSQIResponseDto> responses = psqiService.getRecentResponses(userId, limit);
        return ResponseEntity.ok(responses);
    }

    // ==================== BDI-II (Beck Depression Inventory-II) ====================

    @PostMapping("/bdi")
    @Operation(summary = "BDI-II 설문 제출", description = "벡 우울 척도-II(BDI-II) 설문 응답을 제출합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "설문 응답이 성공적으로 저장되었습니다",
                    content = @Content(schema = @Schema(implementation = BDIResponseDto.class))),
        @ApiResponse(responseCode = "409", description = "이미 응답한 설문입니다"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    public ResponseEntity<BDIResponseDto> submitBDI(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BDIResponseRequestDto requestDto) {

        Long userId = userDetails.getId();
        log.info("BDI-II 설문 제출 요청: 사용자 ID {}", userId);

        BDIResponseDto response = bdiService.submitResponse(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bdi/latest")
    @Operation(summary = "최신 BDI-II 응답 조회", description = "사용자의 최신 BDI-II 설문 응답을 조회합니다")
    public ResponseEntity<BDIResponseDto> getLatestBDI(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        BDIResponseDto response = bdiService.getLatestResponse(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bdi/history")
    @Operation(summary = "BDI-II 응답 이력 조회", description = "사용자의 모든 BDI-II 설문 응답 이력을 조회합니다")
    public ResponseEntity<List<BDIResponseDto>> getBDIHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        List<BDIResponseDto> responses = bdiService.getAllResponses(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/bdi/recent")
    @Operation(summary = "최근 N개 BDI-II 응답 조회", description = "사용자의 최근 N개 BDI-II 설문 응답을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "최근 응답 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 limit 값")
    })
    public ResponseEntity<List<BDIResponseDto>> getRecentBDI(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "5") int limit) {

        Long userId = userDetails.getId();
        List<BDIResponseDto> responses = bdiService.getRecentResponses(userId, limit);
        return ResponseEntity.ok(responses);
    }

    // ==================== 관리자 전용: 자살 위험 모니터링 ====================

    @GetMapping("/admin/bdi/suicide-risk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[관리자] 자살 위험 응답 조회",
               description = "자살 위험이 있는 모든 BDI-II 응답을 조회합니다 (q9 >= 2)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "자살 위험 응답 목록 조회 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<List<BDIResponseDto>> getSuicideRiskResponses() {
        log.info("[관리자] 자살 위험 응답 목록 조회 요청");

        List<BDIResponseDto> responses = bdiService.getSuicideRiskResponses();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/admin/bdi/suicide-risk/count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[관리자] 자살 위험 응답 개수",
               description = "자살 위험이 있는 BDI-II 응답의 총 개수를 반환합니다")
    public ResponseEntity<Long> getSuicideRiskCount() {
        log.info("[관리자] 자살 위험 응답 개수 조회 요청");

        long count = bdiService.getSuicideRiskCount();
        return ResponseEntity.ok(count);
    }

    // ==================== SIS (Sleep Impact Scale) ====================

    @PostMapping("/sis")
    @Operation(summary = "SIS 설문 제출", description = "수면 심각도 점수(SIS) 설문 응답을 제출합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "설문 응답이 성공적으로 저장되었습니다",
                    content = @Content(schema = @Schema(implementation = SISResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    public ResponseEntity<SISResponseDto> submitSIS(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SISResponseRequestDto requestDto) {

        Long userId = userDetails.getId();
        log.info("SIS 설문 제출 요청: 사용자 ID {}", userId);

        SISResponseDto response = sisService.submitResponse(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sis/latest")
    @Operation(summary = "최신 SIS 응답 조회", description = "사용자의 최신 SIS 설문 응답을 조회합니다")
    public ResponseEntity<SISResponseDto> getLatestSIS(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        SISResponseDto response = sisService.getLatestResponse(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sis/history")
    @Operation(summary = "SIS 응답 이력 조회", description = "사용자의 모든 SIS 설문 응답 이력을 조회합니다")
    public ResponseEntity<List<SISResponseDto>> getSISHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        List<SISResponseDto> responses = sisService.getAllResponses(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/sis/recent")
    @Operation(summary = "최근 N개 SIS 응답 조회", description = "사용자의 최근 N개 SIS 설문 응답을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "최근 응답 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 limit 값")
    })
    public ResponseEntity<List<SISResponseDto>> getRecentSIS(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "5") int limit) {

        Long userId = userDetails.getId();
        List<SISResponseDto> responses = sisService.getRecentResponses(userId, limit);
        return ResponseEntity.ok(responses);
    }
}
