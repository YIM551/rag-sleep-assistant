package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.ASMRContentRequestDto;
import com.sleepwell.sleepwell_backend.dto.ASMRContentResponseDto;
import com.sleepwell.sleepwell_backend.enums.ASMRCategory;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import com.sleepwell.sleepwell_backend.service.ASMRService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ASMR 콘텐츠 관리 API
 */
@RestController
@RequestMapping("/api/asmr")
@Tag(name = "ASMR", description = "ASMR 콘텐츠 관리 API")
@RequiredArgsConstructor
@Slf4j
public class ASMRController {

    private final ASMRService asmrService;

    @GetMapping("/contents")
    @Operation(summary = "ASMR 콘텐츠 목록 조회", description = "활성 상태의 ASMR 콘텐츠 목록을 페이징으로 조회합니다")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Page<ASMRContentResponseDto>> getContents(
            @PageableDefault(size = 20, sort = "totalPlayCount", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<ASMRContentResponseDto> contents = asmrService.getActiveContents(pageable);
        return ResponseEntity.ok(contents);
    }

    @GetMapping("/contents/category/{category}")
    @Operation(summary = "카테고리별 ASMR 콘텐츠 조회", description = "특정 카테고리의 ASMR 콘텐츠를 조회합니다")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Page<ASMRContentResponseDto>> getContentsByCategory(
            @PathVariable ASMRCategory category,
            @PageableDefault(size = 20, sort = "totalPlayCount", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<ASMRContentResponseDto> contents = asmrService.getContentsByCategory(category, pageable);
        return ResponseEntity.ok(contents);
    }

    @GetMapping("/contents/{contentId}")
    @Operation(summary = "ASMR 콘텐츠 상세 조회", description = "특정 ASMR 콘텐츠의 상세 정보를 조회합니다")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ASMRContentResponseDto> getContent(
            @PathVariable Long contentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ASMRContentResponseDto content = asmrService.getContent(contentId);
        return ResponseEntity.ok(content);
    }

    @GetMapping("/contents/popular")
    @Operation(summary = "인기 ASMR 콘텐츠 조회", description = "재생 횟수가 높은 인기 ASMR 콘텐츠를 조회합니다")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ASMRContentResponseDto>> getPopularContents(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<ASMRContentResponseDto> contents = asmrService.getPopularContents();
        return ResponseEntity.ok(contents);
    }

    @PostMapping("/contents/{contentId}/play")
    @Operation(summary = "ASMR 재생 기록", description = "ASMR 콘텐츠 재생을 기록하고 통계를 업데이트합니다")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> recordPlay(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "false") boolean completed,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        asmrService.recordPlay(contentId, completed);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/contents")
    @Operation(summary = "ASMR 콘텐츠 생성", description = "새로운 ASMR 콘텐츠를 생성합니다 (관리자 전용)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ASMRContentResponseDto> createContent(
            @Valid @RequestBody ASMRContentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ASMRContentResponseDto content = asmrService.createContent(requestDto);
        return ResponseEntity.ok(content);
    }

    @GetMapping("/categories")
    @Operation(summary = "ASMR 카테고리 목록 조회", description = "사용 가능한 ASMR 카테고리 목록을 조회합니다")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<Map<String, Object>>> getCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<Map<String, Object>> categories = Arrays.stream(ASMRCategory.values())
                .map(category -> {
                    Map<String, Object> categoryInfo = new HashMap<>();
                    categoryInfo.put("name", category.name());
                    categoryInfo.put("displayName", getCategoryDisplayName(category));
                    categoryInfo.put("description", getCategoryDescription(category));
                    return categoryInfo;
                })
                .toList();

        return ResponseEntity.ok(categories);
    }

    @DeleteMapping("/contents/{contentId}")
    @Operation(summary = "ASMR 콘텐츠 삭제", description = "ASMR 콘텐츠를 삭제합니다 (관리자 전용). DB 레코드만 삭제하며 S3 파일은 유지됩니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteContent(
            @PathVariable Long contentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("ASMR 콘텐츠 삭제 요청 - contentId: {}, admin: {}", contentId, userDetails.getUsername());
        asmrService.deleteContent(contentId);

        return ResponseEntity.ok(Map.of(
            "message", "ASMR 콘텐츠가 성공적으로 삭제되었습니다",
            "contentId", contentId.toString()
        ));
    }

    @PostMapping("/{contentId}/timer")
    @Operation(summary = "ASMR 타이머 설정", description = "ASMR 콘텐츠에 재생 타이머를 설정합니다")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> setTimer(
            @PathVariable Long contentId,
            @RequestBody Map<String, Object> timerRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Integer durationMinutes = (Integer) timerRequest.get("durationMinutes");
        Integer fadeOutDuration = (Integer) timerRequest.getOrDefault("fadeOutDuration", 5);

        // Mock 타이머 설정 응답
        Map<String, Object> response = new HashMap<>();
        response.put("contentId", contentId);
        response.put("durationMinutes", durationMinutes);
        response.put("fadeOutDuration", fadeOutDuration);
        response.put("status", "TIMER_SET");
        response.put("message", "타이머가 설정되었습니다 (Mock)");
        response.put("estimatedEndTime", java.time.LocalDateTime.now().plusMinutes(durationMinutes));

        log.info("ASMR 타이머 설정 - contentId: {}, duration: {}분", contentId, durationMinutes);

        return ResponseEntity.ok(response);
    }

    private String getCategoryDisplayName(ASMRCategory category) {
        return switch (category) {
            case NATURE -> "자연 소리";
            case WHITE_NOISE -> "백색 소음";
            case GUIDED -> "가이드 명상";
            case BINAURAL -> "바이노럴 비트";
            default -> category.name();
        };
    }

    private String getCategoryDescription(ASMRCategory category) {
        return switch (category) {
            case NATURE -> "빗소리, 바다소리, 새소리 등 자연의 소리";
            case WHITE_NOISE -> "집중력 향상과 숙면을 돕는 백색 소음";
            case GUIDED -> "전문가가 안내하는 명상과 이완 가이드";
            case BINAURAL -> "뇌파를 조절하는 바이노럴 비트";
            default -> "기타 ASMR 콘텐츠";
        };
    }
}