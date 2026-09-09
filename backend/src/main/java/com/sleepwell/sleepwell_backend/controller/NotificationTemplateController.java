package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.notification.NotificationTemplateDto;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification-templates")
@Tag(name = "알림 템플릿", description = "알림 템플릿 관리 API")
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    @PostMapping
    @Operation(summary = "알림 템플릿 생성", description = "새로운 알림 템플릿을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "템플릿 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<NotificationTemplateDto> createTemplate(@Valid @RequestBody NotificationTemplateDto dto) {
        // 수동 입력 검증 추가 (XSS, SQL Injection 방어)
        validateTemplateInput(dto);

        // XSS 방어 - HTML 태그 제거
        sanitizeTemplateContent(dto);

        return new ResponseEntity<>(notificationTemplateService.createTemplate(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "알림 템플릿 조회", description = "ID로 특정 알림 템플릿을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "템플릿 조회 성공"),
            @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음")
    })
    public ResponseEntity<NotificationTemplateDto> getTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(notificationTemplateService.getTemplate(templateId));
    }

    @GetMapping
    @Operation(summary = "모든 알림 템플릿 조회", description = "페이징 처리하여 모든 알림 템플릿을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "템플릿 목록 조회 성공")
    })
    public ResponseEntity<Page<NotificationTemplateDto>> getAllTemplates(
            @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        return ResponseEntity.ok(notificationTemplateService.getAllTemplates(pageable));
    }

    @PutMapping("/{templateId}")
    @Operation(summary = "알림 템플릿 수정", description = "기존 알림 템플릿을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "템플릿 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음")
    })
    public ResponseEntity<NotificationTemplateDto> updateTemplate(@PathVariable Long templateId, @Valid @RequestBody NotificationTemplateDto dto) {
        // 수동 입력 검증 추가 (XSS, SQL Injection 방어)
        validateTemplateInput(dto);

        // XSS 방어 - HTML 태그 제거
        sanitizeTemplateContent(dto);

        return ResponseEntity.ok(notificationTemplateService.updateTemplate(templateId, dto));
    }

    @DeleteMapping("/{templateId}")
    @Operation(summary = "알림 템플릿 삭제", description = "ID로 특정 알림 템플릿을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "템플릿 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음")
    })
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId) {
        notificationTemplateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{templateId}/activate")
    @Operation(summary = "알림 템플릿 활성화", description = "템플릿을 활성화합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "템플릿 활성화 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음")
    })
    public ResponseEntity<NotificationTemplateDto> activateTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(notificationTemplateService.activateTemplate(templateId));
    }

    @PatchMapping("/{templateId}/deactivate")
    @Operation(summary = "알림 템플릿 비활성화", description = "템플릿을 비활성화합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "템플릿 비활성화 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음")
    })
    public ResponseEntity<NotificationTemplateDto> deactivateTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(notificationTemplateService.deactivateTemplate(templateId));
    }

    @GetMapping("/statistics")
    @Operation(summary = "템플릿 사용 통계 조회", description = "알림 템플릿 사용 통계를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "통계 조회 성공")
    })
    public ResponseEntity<Map<String, Object>> getTemplateStatistics() {
        return ResponseEntity.ok(notificationTemplateService.getTemplateUsageStatistics());
    }

    @GetMapping("/popular")
    @Operation(summary = "인기 템플릿 조회", description = "가장 많이 사용된 템플릿 목록을 페이징 처리하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인기 템플릿 조회 성공")
    })
    public ResponseEntity<Page<NotificationTemplateDto>> getPopularTemplates(
            @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        return ResponseEntity.ok(notificationTemplateService.getPopularTemplates(pageable));
    }

    /**
     * 추가 보안 검증 (Bean Validation에 없는 부분)
     */
    private void validateTemplateInput(NotificationTemplateDto dto) {
        // 특수문자 검증 (SQL Injection 방어)
        if (containsDangerousPatterns(dto.getName())) {
            throw new BusinessException("템플릿 이름에 허용되지 않는 문자가 포함되어 있습니다", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * XSS 방어를 위한 HTML 태그 제거
     */
    private void sanitizeTemplateContent(NotificationTemplateDto dto) {
        if (StringUtils.hasText(dto.getTitleTemplate())) {
            dto.setTitleTemplate(Jsoup.clean(dto.getTitleTemplate(), Safelist.none()));
        }

        if (StringUtils.hasText(dto.getMessageTemplate())) {
            dto.setMessageTemplate(Jsoup.clean(dto.getMessageTemplate(), Safelist.none()));
        }

        if (StringUtils.hasText(dto.getDescription())) {
            dto.setDescription(Jsoup.clean(dto.getDescription(), Safelist.none()));
        }
    }

    /**
     * SQL Injection 패턴 검사
     */
    private boolean containsDangerousPatterns(String input) {
        if (input == null) return false;

        String[] dangerousPatterns = {
            "drop table", "delete from", "insert into", "update set",
            "union select", "script>", "<script", "javascript:",
            "--", "/*", "*/", "xp_", "sp_"
        };

        String lowerInput = input.toLowerCase();
        for (String pattern : dangerousPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }

        return false;
    }
} 