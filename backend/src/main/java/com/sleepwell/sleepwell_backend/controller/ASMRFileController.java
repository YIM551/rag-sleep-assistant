package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.ASMRContentRequestDto;
import com.sleepwell.sleepwell_backend.dto.ASMRContentResponseDto;
import com.sleepwell.sleepwell_backend.service.ASMRFileStorageService;
import com.sleepwell.sleepwell_backend.service.ASMRService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * ASMR 파일 관리 컨트롤러
 * 파일 업로드, 다운로드, 스트리밍 기능
 */
@Slf4j
@RestController
@RequestMapping("/api/asmr/files")
@RequiredArgsConstructor
@Tag(name = "ASMR 파일 관리", description = "ASMR 오디오 파일 업로드 및 관리")
public class ASMRFileController {

    private final ASMRFileStorageService fileStorageService;
    private final ASMRService asmrService;

    @Operation(
        summary = "ASMR 오디오 파일 업로드",
        description = "ASMR 오디오 파일을 S3에 업로드하고 콘텐츠를 생성합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "파일 업로드 및 콘텐츠 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 파일 형식 또는 크기 초과"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ASMRContentResponseDto> uploadASMRFile(
            @Parameter(description = "오디오 파일", required = true)
            @RequestPart("audioFile") MultipartFile audioFile,

            @Parameter(description = "콘텐츠 제목", required = true)
            @RequestParam("title") String title,

            @Parameter(description = "카테고리", required = true)
            @RequestParam("category") String category,

            @Parameter(description = "설명")
            @RequestParam(value = "description", required = false) String description,

            @Parameter(description = "길이(분)")
            @RequestParam(value = "durationMinutes", required = false) Integer durationMinutes,

            @Parameter(description = "프리미엄 여부")
            @RequestParam(value = "isPremium", defaultValue = "false") Boolean isPremium) {

        log.info("ASMR 파일 업로드 요청: 제목={}, 카테고리={}", title, category);

        String audioUrl = fileStorageService.uploadAudioFile(audioFile, category).block();
        ASMRContentResponseDto content = createContentFromUpload(audioUrl, title, category, description, durationMinutes, isPremium);
        return ResponseEntity.ok(content);
    }

    @Operation(
        summary = "다중 품질 ASMR 파일 업로드",
        description = "원본 파일을 업로드하여 고품질, 중품질, 저품질로 자동 변환합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "다중 품질 파일 업로드 성공"),
        @ApiResponse(responseCode = "400", description = "파일 처리 실패"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @PostMapping(value = "/upload/multi-quality", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ASMRContentResponseDto> uploadMultiQualityFile(
            @Parameter(description = "원본 오디오 파일", required = true)
            @RequestPart("audioFile") MultipartFile audioFile,

            @Parameter(description = "콘텐츠 제목", required = true)
            @RequestParam("title") String title,

            @Parameter(description = "카테고리", required = true)
            @RequestParam("category") String category,

            @Parameter(description = "설명")
            @RequestParam(value = "description", required = false) String description,

            @Parameter(description = "길이(분)")
            @RequestParam(value = "durationMinutes", required = false) Integer durationMinutes,

            @Parameter(description = "프리미엄 여부")
            @RequestParam(value = "isPremium", defaultValue = "false") Boolean isPremium) {

        log.info("다중 품질 ASMR 파일 업로드 요청: 제목={}, 카테고리={}", title, category);

        Map<String, String> urls = fileStorageService.uploadMultiQualityFiles(audioFile, category).block();
        ASMRContentResponseDto content = createMultiQualityContent(urls, title, category, description, durationMinutes, isPremium);
        return ResponseEntity.ok(content);
    }

    @Operation(
        summary = "ASMR 파일 삭제",
        description = "S3에서 ASMR 파일을 삭제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "파일 삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 파일 URL"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteFile(
            @Parameter(description = "삭제할 파일 URL", required = true)
            @RequestParam("fileUrl") String fileUrl) {

        log.info("ASMR 파일 삭제 요청: {}", fileUrl);

        fileStorageService.deleteFile(fileUrl).block();
        return ResponseEntity.ok(Map.of("message", "파일이 성공적으로 삭제되었습니다", "deletedUrl", fileUrl));
    }

    @Operation(
        summary = "CDN URL 생성",
        description = "S3 키로부터 CDN URL을 생성합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "CDN URL 생성 성공")
    })
    @GetMapping("/cdn-url")
    public ResponseEntity<Map<String, String>> generateCdnUrl(
            @Parameter(description = "S3 키", required = true)
            @RequestParam("s3Key") String s3Key) {

        String cdnUrl = fileStorageService.generateCdnUrl(s3Key);

        return ResponseEntity.ok(Map.of(
            "s3Key", s3Key,
            "cdnUrl", cdnUrl
        ));
    }

    /**
     * 업로드된 파일로부터 ASMR 콘텐츠 생성
     */
    private ASMRContentResponseDto createContentFromUpload(String audioUrl, String title, String category,
                                                          String description, Integer durationMinutes, Boolean isPremium) {
        int minutes = durationMinutes != null ? durationMinutes : 1;

        ASMRContentRequestDto request = ASMRContentRequestDto.builder()
                .title(title)
                .description(description)
                .category(parseCategory(category))
                .durationMinutes(minutes)
                .durationSeconds(0) // 나머지 초 (분의 일부)
                .audioQuality(320) // 고품질: 320kbps
                .intensityLevel(5) // 기본 강도: 중간 (1-10)
                .highQualityUrl(audioUrl)
                .isPremium(isPremium)
                .build();

        return asmrService.createContent(request);
    }

    /**
     * 다중 품질 파일로부터 ASMR 콘텐츠 생성
     */
    private ASMRContentResponseDto createMultiQualityContent(Map<String, String> urls, String title, String category,
                                                           String description, Integer durationMinutes, Boolean isPremium) {
        int minutes = durationMinutes != null ? durationMinutes : 1;

        ASMRContentRequestDto request = ASMRContentRequestDto.builder()
                .title(title)
                .description(description)
                .category(parseCategory(category))
                .durationMinutes(minutes)
                .durationSeconds(0) // 나머지 초 (분의 일부)
                .audioQuality(320) // 고품질 기준: 320kbps
                .intensityLevel(5) // 기본 강도: 중간 (1-10)
                .highQualityUrl(urls.get("high"))
                .mediumQualityUrl(urls.get("medium"))
                .lowQualityUrl(urls.get("low"))
                .previewUrl(urls.get("preview"))
                .isPremium(isPremium)
                .build();

        return asmrService.createContent(request);
    }

    /**
     * 카테고리 문자열을 enum으로 변환
     */
    private com.sleepwell.sleepwell_backend.enums.ASMRCategory parseCategory(String category) {
        try {
            return com.sleepwell.sleepwell_backend.enums.ASMRCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("유효하지 않은 카테고리: {}, 기본값 NATURE 사용", category);
            return com.sleepwell.sleepwell_backend.enums.ASMRCategory.NATURE;
        }
    }
}