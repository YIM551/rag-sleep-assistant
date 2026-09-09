package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.aspect.ASMRRateLimitingAspect.RateLimit;
import com.sleepwell.sleepwell_backend.dto.ASMRSessionStatusDto;
import com.sleepwell.sleepwell_backend.dto.ASMRTimerRequestDto;
import com.sleepwell.sleepwell_backend.dto.ASMRTimerResponseDto;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import com.sleepwell.sleepwell_backend.service.ASMRService;
import com.sleepwell.sleepwell_backend.service.ASMRStreamingService;
import com.sleepwell.sleepwell_backend.service.ASMRTimerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ASMR 스트리밍 컨트롤러
 * 오디오 스트리밍 및 적응형 품질 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/asmr/stream")
@RequiredArgsConstructor
@Tag(name = "ASMR 스트리밍", description = "ASMR 오디오 스트리밍 및 품질 관리")
public class ASMRStreamingController {

    private final ASMRStreamingService streamingService;
    private final ASMRService asmrService;
    private final ASMRTimerService timerService;

    @Operation(
        summary = "ASMR 오디오 스트리밍",
        description = "지정된 품질로 ASMR 오디오를 스트리밍합니다. Range 헤더를 지원하여 구간 재생이 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "스트리밍 성공"),
        @ApiResponse(responseCode = "206", description = "부분 콘텐츠 스트리밍"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "콘텐츠를 찾을 수 없음"),
        @ApiResponse(responseCode = "416", description = "요청 범위가 유효하지 않음")
    })
    @GetMapping("/{contentId}")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<Resource>> streamAudio(
            @Parameter(description = "콘텐츠 ID", required = true)
            @PathVariable Long contentId,

            @Parameter(description = "품질 (high, medium, low)")
            @RequestParam(value = "quality", defaultValue = "medium") String quality,

            @Parameter(description = "Range 헤더 (구간 재생)")
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        log.info("ASMR 스트리밍 요청: contentId={}, quality={}, range={}", contentId, quality, rangeHeader);

        // 재생 통계 업데이트
        return streamingService.getStreamingResource(contentId, quality, rangeHeader)
                .flatMap(streamingResponse -> {
                    // 스트리밍 시작 시 재생 기록
                    recordPlayStart(contentId);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
                    headers.set("Accept-Ranges", "bytes");
                    headers.set("Cache-Control", "public, max-age=3600");

                    if (streamingResponse.isPartialContent()) {
                        headers.set("Content-Range", streamingResponse.getContentRange());
                        headers.setContentLength(streamingResponse.getContentLength());
                        return Mono.just(ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                                .headers(headers)
                                .body(streamingResponse.getResource()));
                    } else {
                        headers.setContentLength(streamingResponse.getContentLength());
                        return Mono.just(ResponseEntity.ok()
                                .headers(headers)
                                .body(streamingResponse.getResource()));
                    }
                })
                .doOnError(error -> log.error("스트리밍 실패: contentId={}, error={}", contentId, error.getMessage()));
    }

    @Operation(
        summary = "적응형 품질 추천",
        description = "사용자의 네트워크 상태를 기반으로 최적의 오디오 품질을 추천합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "품질 추천 성공")
    })
    @GetMapping("/quality/recommend")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> recommendQuality(
            @Parameter(description = "예상 대역폭 (kbps)")
            @RequestParam(value = "bandwidth", required = false) Integer bandwidth,

            @Parameter(description = "네트워크 타입 (wifi, mobile)")
            @RequestParam(value = "networkType", required = false) String networkType,

            @Parameter(description = "배터리 상태 (low, normal, high)")
            @RequestParam(value = "batteryLevel", required = false) String batteryLevel) {

        var recommendation = streamingService.recommendQualitySync(bandwidth, networkType, batteryLevel);
        return ResponseEntity.ok(Map.of(
            "recommendedQuality", recommendation.getQuality(),
            "bitrate", recommendation.getBitrate(),
            "reason", recommendation.getReason(),
            "networkOptimized", recommendation.isNetworkOptimized()
        ));
    }

    @Operation(
        summary = "오디오 미리보기",
        description = "ASMR 오디오의 30초 미리보기를 스트리밍합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "미리보기 스트리밍 성공"),
        @ApiResponse(responseCode = "404", description = "콘텐츠를 찾을 수 없음")
    })
    @GetMapping("/{contentId}/preview")
    public Mono<ResponseEntity<Resource>> streamPreview(
            @Parameter(description = "콘텐츠 ID", required = true)
            @PathVariable Long contentId) {

        log.info("ASMR 미리보기 요청: contentId={}", contentId);

        return streamingService.getPreviewResource(contentId)
                .map(resource -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
                    headers.set("Cache-Control", "public, max-age=1800");

                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(resource);
                });
    }

    @Operation(
        summary = "재생 완료 처리",
        description = "ASMR 오디오 재생 완료를 기록하고 통계를 업데이트합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "재생 완료 기록 성공")
    })
    @PostMapping("/{contentId}/complete")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<Map<String, String>>> completePlayback(
            @Parameter(description = "콘텐츠 ID", required = true)
            @PathVariable Long contentId,

            @Parameter(description = "재생 시간 (초)")
            @RequestParam(value = "playedSeconds", required = false) Integer playedSeconds,

            @Parameter(description = "완전 재생 여부")
            @RequestParam(value = "completed", defaultValue = "true") Boolean completed) {

        log.info("ASMR 재생 완료: contentId={}, playedSeconds={}, completed={}", contentId, playedSeconds, completed);

        return Mono.fromRunnable(() -> asmrService.recordPlay(contentId, completed))
                .then(Mono.just(ResponseEntity.ok(Map.of(
                    "message", "재생 완료가 기록되었습니다",
                    "contentId", contentId.toString(),
                    "completed", completed.toString()
                ))));
    }

    @Operation(
        summary = "스트리밍 상태 확인",
        description = "현재 스트리밍 서비스의 상태와 통계를 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상태 확인 성공")
    })
    @GetMapping("/status")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<Map<String, Object>>> getStreamingStatus() {
        return streamingService.getStreamingStats()
                .map(stats -> ResponseEntity.ok(Map.of(
                    "activeStreams", stats.getActiveStreams(),
                    "totalBandwidth", stats.getTotalBandwidth(),
                    "averageQuality", stats.getAverageQuality(),
                    "cacheHitRate", stats.getCacheHitRate(),
                    "uptime", stats.getUptimeSeconds()
                )));
    }

    @Operation(
        summary = "품질별 URL 조회",
        description = "특정 콘텐츠의 모든 품질별 스트리밍 URL을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL 조회 성공"),
        @ApiResponse(responseCode = "404", description = "콘텐츠를 찾을 수 없음")
    })
    @GetMapping("/{contentId}/urls")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> getStreamingUrls(
            @Parameter(description = "콘텐츠 ID", required = true)
            @PathVariable Long contentId) {

        Map<String, String> urls = streamingService.getStreamingUrlsSync(contentId);
        return ResponseEntity.ok(urls);
    }

    // ===============================================
    // ASMR Sleep Timer API
    // ===============================================

    @Operation(
        summary = "타이머와 함께 ASMR 재생 세션 시작",
        description = "수면 타이머를 설정하여 ASMR 재생 세션을 시작합니다. 기존 활성 세션은 자동으로 종료됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "타이머 세션 시작 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 타이머 설정"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "콘텐츠를 찾을 수 없음")
    })
    @PostMapping("/{contentId}/timer")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(name = "asmr-api-requests")
    public ResponseEntity<ASMRTimerResponseDto> startSessionWithTimer(
            @Parameter(description = "콘텐츠 ID", required = true)
            @PathVariable Long contentId,

            @Parameter(description = "타이머 설정 정보", required = true)
            @Valid @RequestBody ASMRTimerRequestDto request,

            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("타이머 세션 시작 요청: userId={}, contentId={}, timer={}분",
                userDetails.getUser().getId(), contentId, request.getTimerMinutes());

        ASMRTimerResponseDto response = timerService.startSessionWithTimer(
                userDetails.getUser().getId(), contentId, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "기존 세션에 타이머 설정",
        description = "진행 중인 ASMR 세션에 수면 타이머를 설정하거나 수정합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "타이머 설정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 타이머 설정"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @PutMapping("/sessions/{sessionId}/timer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ASMRTimerResponseDto> setTimer(
            @Parameter(description = "세션 ID", required = true)
            @PathVariable Long sessionId,

            @Parameter(description = "타이머 설정 정보", required = true)
            @Valid @RequestBody ASMRTimerRequestDto request,

            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("타이머 설정 요청: userId={}, sessionId={}, timer={}분",
                userDetails.getUser().getId(), sessionId, request.getTimerMinutes());

        ASMRTimerResponseDto response = timerService.setTimer(
                userDetails.getUser().getId(), sessionId, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "타이머 연장",
        description = "현재 설정된 타이머를 추가 시간만큼 연장합니다. (최대 60분까지 한 번에 연장 가능)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "타이머 연장 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 연장 시간"),
        @ApiResponse(responseCode = "404", description = "세션 또는 타이머를 찾을 수 없음")
    })
    @PutMapping("/sessions/{sessionId}/timer/extend")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ASMRTimerResponseDto> extendTimer(
            @Parameter(description = "세션 ID", required = true)
            @PathVariable Long sessionId,

            @Parameter(description = "연장할 시간 (분 단위)", required = true, example = "30")
            @RequestParam Integer additionalMinutes,

            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("타이머 연장 요청: userId={}, sessionId={}, 추가시간={}분",
                userDetails.getUser().getId(), sessionId, additionalMinutes);

        ASMRTimerResponseDto response = timerService.extendTimer(
                userDetails.getUser().getId(), sessionId, additionalMinutes);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "타이머 취소",
        description = "현재 설정된 타이머를 취소합니다. 세션은 계속 진행됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "타이머 취소 성공"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @DeleteMapping("/sessions/{sessionId}/timer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ASMRTimerResponseDto> cancelTimer(
            @Parameter(description = "세션 ID", required = true)
            @PathVariable Long sessionId,

            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("타이머 취소 요청: userId={}, sessionId={}", userDetails.getUser().getId(), sessionId);

        ASMRTimerResponseDto response = timerService.cancelTimer(
                userDetails.getUser().getId(), sessionId);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "세션 일시 정지",
        description = "ASMR 재생 세션을 일시 정지합니다. 타이머는 계속 진행됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "세션 일시 정지 성공"),
        @ApiResponse(responseCode = "400", description = "일시 정지할 수 없는 상태"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @PostMapping("/sessions/{sessionId}/pause")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ASMRTimerResponseDto> pauseSession(
            @Parameter(description = "세션 ID", required = true)
            @PathVariable Long sessionId,

            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("세션 일시 정지 요청: userId={}, sessionId={}", userDetails.getUser().getId(), sessionId);

        ASMRTimerResponseDto response = timerService.pauseSession(
                userDetails.getUser().getId(), sessionId);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "세션 재개",
        description = "일시 정지된 ASMR 재생 세션을 재개합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "세션 재개 성공"),
        @ApiResponse(responseCode = "400", description = "재개할 수 없는 상태"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @PostMapping("/sessions/{sessionId}/resume")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ASMRTimerResponseDto> resumeSession(
            @Parameter(description = "세션 ID", required = true)
            @PathVariable Long sessionId,

            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("세션 재개 요청: userId={}, sessionId={}", userDetails.getUser().getId(), sessionId);

        ASMRTimerResponseDto response = timerService.resumeSession(
                userDetails.getUser().getId(), sessionId);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "세션 중지",
        description = "ASMR 재생 세션을 완전히 중지합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "세션 중지 성공"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @PostMapping("/sessions/{sessionId}/stop")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> stopSession(
            @Parameter(description = "세션 ID", required = true)
            @PathVariable Long sessionId,

            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("세션 중지 요청: userId={}, sessionId={}", userDetails.getUser().getId(), sessionId);

        timerService.stopSession(userDetails.getUser().getId(), sessionId);

        return ResponseEntity.ok(Map.of(
            "message", "세션이 성공적으로 중지되었습니다",
            "sessionId", sessionId.toString()
        ));
    }

    @Operation(
        summary = "현재 활성 세션 조회",
        description = "사용자의 현재 진행 중인 ASMR 세션 정보와 타이머 상태를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "세션 조회 성공"),
        @ApiResponse(responseCode = "404", description = "활성 세션 없음")
    })
    @GetMapping("/sessions/current")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ASMRTimerResponseDto> getCurrentSession(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("현재 세션 조회 요청: userId={}", userDetails.getUser().getId());

        Optional<ASMRTimerResponseDto> currentSession = timerService.getCurrentSession(userDetails.getUser().getId());

        return currentSession
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "모든 활성 세션 조회",
        description = "사용자의 모든 진행 중인 ASMR 세션 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "세션 목록 조회 성공")
    })
    @GetMapping("/sessions/active")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ASMRSessionStatusDto>> getAllActiveSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("모든 활성 세션 조회 요청: userId={}", userDetails.getUser().getId());

        List<ASMRSessionStatusDto> activeSessions = timerService.getAllActiveSessions(userDetails.getUser().getId());

        return ResponseEntity.ok(activeSessions);
    }

    @Operation(
        summary = "재생 시간 업데이트",
        description = "클라이언트에서 주기적으로 호출하여 실제 재생 시간을 서버에 동기화합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "재생 시간 업데이트 성공"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @PutMapping("/sessions/{sessionId}/playtime")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> updatePlayedTime(
            @Parameter(description = "세션 ID", required = true)
            @PathVariable Long sessionId,

            @Parameter(description = "추가 재생 시간 (초 단위)", required = true, example = "30")
            @RequestParam Integer playedSeconds,

            @AuthenticationPrincipal CustomUserDetails userDetails) {

        timerService.updatePlayedTime(userDetails.getUser().getId(), sessionId, playedSeconds);

        return ResponseEntity.ok(Map.of(
            "message", "재생 시간이 업데이트되었습니다",
            "sessionId", sessionId.toString(),
            "addedSeconds", playedSeconds.toString()
        ));
    }

    /**
     * 재생 시작 기록 (비동기)
     */
    private void recordPlayStart(Long contentId) {
        try {
            // 비동기로 재생 시작 기록
            asmrService.recordPlay(contentId, false);
        } catch (Exception e) {
            log.warn("재생 시작 기록 실패: contentId={}, error={}", contentId, e.getMessage());
        }
    }
}