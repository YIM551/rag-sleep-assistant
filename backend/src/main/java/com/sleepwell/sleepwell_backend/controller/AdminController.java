package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.admin.*;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "관리자", description = "관리자 전용 API")
public class AdminController {

    private final AdminService adminService;
    private final SleepRecordRepository sleepRecordRepository;

    // ==================== 1. 대시보드 & 통계 ====================
    
    @GetMapping("/dashboard")
    @Operation(summary = "관리자 대시보드", description = "핵심 지표와 통계를 한눈에 확인합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "대시보드 데이터 조회 성공"),
        @ApiResponse(responseCode = "403", description = "권한 부족")
    })
    public ResponseEntity<AdminDashboardDto> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        
        log.info("관리자 대시보드 조회 - 기간: {} ~ {}", startDate, endDate);
        return ResponseEntity.ok(adminService.getDashboardData(startDate, endDate));
    }

    @GetMapping("/dashboard/realtime")
    @Operation(summary = "실시간 모니터링", description = "실시간 서비스 상태를 모니터링합니다")
    public ResponseEntity<RealtimeMetricsDto> getRealtimeMetrics() {
        log.info("실시간 메트릭 조회");
        return ResponseEntity.ok(adminService.getRealtimeMetrics());
    }

    // ==================== 2. 사용자 관리 ====================
    
    @GetMapping("/users")
    @Operation(summary = "사용자 목록 조회", description = "필터링과 페이징을 지원하는 사용자 목록을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "사용자 목록 조회 성공"),
        @ApiResponse(responseCode = "403", description = "권한 부족")
    })
    public ResponseEntity<Page<AdminUserDto>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registeredAfter,
            @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        
        log.info("사용자 목록 조회 - 검색어: {}, 역할: {}, 활성: {}", search, role, isActive);
        return ResponseEntity.ok(adminService.getUsers(search, role, isActive, registeredAfter, pageable));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "사용자 상세 정보", description = "특정 사용자의 상세 정보와 활동 내역을 조회합니다")
    public ResponseEntity<AdminUserDetailDto> getUserDetail(@PathVariable Long userId) {
        log.info("사용자 상세 정보 조회 - userId: {}", userId);
        return ResponseEntity.ok(adminService.getUserDetail(userId));
    }

    @PutMapping("/users/{userId}/status")
    @Operation(summary = "사용자 상태 변경", description = "사용자 계정을 활성화/비활성화합니다")
    public ResponseEntity<AdminUserDto> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam boolean isActive,
            @RequestParam(required = false) String reason) {
        
        log.info("사용자 상태 변경 - userId: {}, isActive: {}, reason: {}", userId, isActive, reason);
        return ResponseEntity.ok(adminService.updateUserStatus(userId, isActive, reason));
    }

    @PutMapping("/users/{userId}/role")
    @Operation(summary = "사용자 역할 변경", description = "사용자의 역할을 변경합니다 (USER/PREMIUM/ADMIN)")
    public ResponseEntity<AdminUserDto> updateUserRole(
            @PathVariable Long userId,
            @RequestParam String role) {
        
        log.info("사용자 역할 변경 - userId: {}, role: {}", userId, role);
        return ResponseEntity.ok(adminService.updateUserRole(userId, role));
    }

    @PostMapping("/users/{userId}/reset-password")
    @Operation(summary = "비밀번호 초기화", description = "사용자의 비밀번호를 초기화하고 임시 비밀번호를 발급합니다")
    public ResponseEntity<Map<String, String>> resetUserPassword(@PathVariable Long userId) {
        log.info("비밀번호 초기화 - userId: {}", userId);
        String tempPassword = adminService.resetUserPassword(userId);
        return ResponseEntity.ok(Map.of(
            "message", "비밀번호가 초기화되었습니다",
            "tempPassword", tempPassword
        ));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "사용자 삭제", description = "사용자를 삭제합니다. hardDelete=true시 완전 삭제, false시 비활성화 처리")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "사용자 삭제 성공"),
        @ApiResponse(responseCode = "403", description = "권한 부족"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean hardDelete) {

        log.info("사용자 삭제 - userId: {}, hardDelete: {}", userId, hardDelete);
        adminService.deleteUser(userId, hardDelete);

        String message = hardDelete
            ? "사용자가 영구적으로 삭제되었습니다"
            : "사용자가 비활성화 처리되었습니다";

        return ResponseEntity.ok(Map.of("message", message));
    }

    // ==================== 3. AI 대화 내용 조회 ====================
    
    @GetMapping("/consultations")
    @Operation(summary = "AI 상담 내역 조회", description = "모든 사용자의 AI 상담 내역을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상담 내역 조회 성공"),
        @ApiResponse(responseCode = "403", description = "권한 부족")
    })
    public ResponseEntity<Page<AdminConsultationDto>> getConsultations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        
        log.info("AI 상담 내역 조회 - userId: {}, keyword: {}", userId, keyword);
        return ResponseEntity.ok(adminService.getConsultations(userId, keyword, startTime, endTime, pageable));
    }

    @GetMapping("/consultations/{sessionId}")
    @Operation(summary = "상담 대화 상세 조회", description = "특정 상담 세션의 전체 대화 내용을 조회합니다")
    public ResponseEntity<AdminConsultationDetailDto> getConsultationDetail(@PathVariable Long sessionId) {
        log.info("상담 대화 상세 조회 - sessionId: {}", sessionId);
        return ResponseEntity.ok(adminService.getConsultationDetail(sessionId));
    }

    @GetMapping("/consultations/statistics")
    @Operation(summary = "AI 상담 통계", description = "AI 상담 사용 통계와 주요 키워드를 분석합니다")
    public ResponseEntity<ConsultationStatisticsDto> getConsultationStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        
        log.info("AI 상담 통계 조회 - 기간: {} ~ {}", startDate, endDate);
        return ResponseEntity.ok(adminService.getConsultationStatistics(startDate, endDate));
    }
    
    // 추가: 수면 통계 엔드포인트
    @GetMapping("/sleep/statistics")
    @Operation(summary = "수면 통계", description = "전체 사용자의 수면 통계를 조회합니다")
    public ResponseEntity<Map<String, Object>> getSleepStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        
        log.info("수면 통계 조회 - 기간: {} ~ {}", startDate, endDate);
        
        // 임시 구현 (실제 통계 계산 필요)
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", sleepRecordRepository.count());
        stats.put("averageSleepHours", 7.5);
        stats.put("period", Map.of("startDate", startDate, "endDate", endDate));
        stats.put("message", "통계 기능 구현 예정");
        
        return ResponseEntity.ok(stats);
    }
    
    // 추가: 분석 작업 목록 엔드포인트
    @GetMapping("/analysis/jobs")
    @Operation(summary = "분석 작업 목록", description = "AI 분석 작업 목록을 조회합니다")
    public ResponseEntity<Page<Map<String, Object>>> getAnalysisJobs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        
        log.info("분석 작업 목록 조회 - 상태: {}, 사용자: {}", status, userId);
        
        // 임시 구현
        List<Map<String, Object>> jobs = new ArrayList<>();
        Map<String, Object> sampleJob = new HashMap<>();
        sampleJob.put("id", 1L);
        sampleJob.put("status", "COMPLETED");
        sampleJob.put("analysisType", "COMPREHENSIVE");
        sampleJob.put("createdAt", LocalDateTime.now());
        sampleJob.put("message", "분석 작업 기능 구현 예정");
        jobs.add(sampleJob);
        
        return ResponseEntity.ok(new PageImpl<>(jobs, pageable, jobs.size()));
    }

    // ==================== 4. 구독/결제 관리 ====================
    
    @GetMapping("/subscriptions")
    @Operation(summary = "구독 현황 조회", description = "전체 구독 현황과 통계를 조회합니다")
    public ResponseEntity<SubscriptionOverviewDto> getSubscriptionOverview() {
        log.info("구독 현황 조회");
        return ResponseEntity.ok(adminService.getSubscriptionOverview());
    }

    @GetMapping("/payments")
    @Operation(summary = "결제 내역 조회", description = "전체 결제 내역을 조회합니다")
    public ResponseEntity<Page<AdminPaymentDto>> getPayments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        
        log.info("결제 내역 조회 - userId: {}, status: {}", userId, status);
        return ResponseEntity.ok(adminService.getPayments(userId, status, startDate, endDate, pageable));
    }

    @PostMapping("/subscriptions/{userId}/free-trial")
    @Operation(summary = "무료 체험 부여", description = "특정 사용자에게 무료 체험 기간을 부여합니다")
    public ResponseEntity<Map<String, Object>> grantFreeTrial(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "7") int days) {
        
        log.info("무료 체험 부여 - userId: {}, days: {}", userId, days);
        LocalDateTime expiryDate = adminService.grantFreeTrial(userId, days);
        return ResponseEntity.ok(Map.of(
            "message", "무료 체험이 부여되었습니다",
            "expiryDate", expiryDate
        ));
    }

    // ==================== 5. 데이터 분석 ====================
    
    @GetMapping("/analytics/user-retention")
    @Operation(summary = "사용자 리텐션 분석", description = "사용자 리텐션율을 분석합니다")
    public ResponseEntity<UserRetentionDto> getUserRetention(
            @RequestParam(required = false, defaultValue = "30") int days) {
        
        log.info("사용자 리텐션 분석 - 기간: {}일", days);
        return ResponseEntity.ok(adminService.getUserRetention(days));
    }

    @GetMapping("/analytics/sleep-patterns")
    @Operation(summary = "수면 패턴 분석", description = "전체 사용자의 수면 패턴 트렌드를 분석합니다")
    public ResponseEntity<SleepPatternAnalysisDto> getSleepPatternAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        
        log.info("수면 패턴 분석 - 기간: {} ~ {}", startDate, endDate);
        return ResponseEntity.ok(adminService.getSleepPatternAnalysis(startDate, endDate));
    }

    @GetMapping("/analytics/feature-usage")
    @Operation(summary = "기능별 사용 통계", description = "각 기능의 사용 빈도와 패턴을 분석합니다")
    public ResponseEntity<FeatureUsageDto> getFeatureUsage() {
        log.info("기능별 사용 통계 조회");
        return ResponseEntity.ok(adminService.getFeatureUsage());
    }

    // ==================== 6. 시스템 관리 ====================
    
    @GetMapping("/system/health")
    @Operation(summary = "시스템 상태", description = "시스템 health 상태를 확인합니다")
    public ResponseEntity<SystemHealthDto> getSystemHealth() {
        log.info("시스템 상태 조회");
        return ResponseEntity.ok(adminService.getSystemHealth());
    }

    @GetMapping("/system/errors")
    @Operation(summary = "에러 로그 조회", description = "최근 발생한 에러 로그를 조회합니다")
    public ResponseEntity<List<ErrorLogDto>> getErrorLogs(
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false) String level) {
        
        log.info("에러 로그 조회 - limit: {}, level: {}", limit, level);
        return ResponseEntity.ok(adminService.getErrorLogs(limit, level));
    }

    @PostMapping("/system/cache/clear")
    @Operation(summary = "캐시 초기화", description = "시스템 캐시를 초기화합니다")
    public ResponseEntity<Map<String, String>> clearCache(
            @RequestParam(required = false) String cacheName) {
        
        log.info("캐시 초기화 - cacheName: {}", cacheName);
        adminService.clearCache(cacheName);
        return ResponseEntity.ok(Map.of("message", "캐시가 초기화되었습니다"));
    }

    @PostMapping("/system/maintenance")
    @Operation(summary = "유지보수 모드", description = "유지보수 모드를 활성화/비활성화합니다")
    public ResponseEntity<Map<String, Object>> setMaintenanceMode(
            @RequestParam boolean enabled,
            @RequestParam(required = false) String message) {

        log.info("유지보수 모드 설정 - enabled: {}, message: {}", enabled, message);
        adminService.setMaintenanceMode(enabled, message);
        return ResponseEntity.ok(Map.of(
            "maintenanceMode", enabled,
            "message", message != null ? message : "시스템 점검 중입니다"
        ));
    }

    // ==================== 7. 사용자 수면 기록 조회 ====================

    @GetMapping("/users/{userId}/sleep-records")
    @Operation(summary = "사용자 수면 기록 조회",
               description = "특정 사용자의 수면 기록을 조회합니다. 날짜 범위를 지정하지 않으면 전체 기록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수면 기록 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 날짜 범위"),
        @ApiResponse(responseCode = "403", description = "권한 부족"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<Page<com.sleepwell.sleepwell_backend.dto.SleepRecordResponseDto>> getUserSleepRecords(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @org.springdoc.core.annotations.ParameterObject Pageable pageable) {

        log.info("관리자 수면 기록 조회 - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);
        return ResponseEntity.ok(adminService.getUserSleepRecords(userId, startDate, endDate, pageable));
    }

    @GetMapping("/users/{userId}/sleep-records/{recordId}")
    @Operation(summary = "사용자 수면 기록 상세 조회",
               description = "특정 사용자의 수면 기록 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수면 기록 상세 조회 성공"),
        @ApiResponse(responseCode = "403", description = "권한 부족"),
        @ApiResponse(responseCode = "404", description = "사용자 또는 수면 기록을 찾을 수 없음")
    })
    public ResponseEntity<com.sleepwell.sleepwell_backend.dto.SleepRecordResponseDto> getUserSleepRecord(
            @PathVariable Long userId,
            @PathVariable Long recordId) {

        log.info("관리자 수면 기록 상세 조회 - userId: {}, recordId: {}", userId, recordId);
        return ResponseEntity.ok(adminService.getUserSleepRecord(userId, recordId));
    }

    @GetMapping("/users/{userId}/sleep-statistics")
    @Operation(summary = "사용자 수면 통계 조회",
               description = "특정 사용자의 수면 통계를 조회합니다. 날짜 범위를 지정하지 않으면 최근 30일 통계를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수면 통계 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 날짜 범위"),
        @ApiResponse(responseCode = "403", description = "권한 부족"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<Map<String, Object>> getUserSleepStatistics(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("관리자 수면 통계 조회 - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);
        return ResponseEntity.ok(adminService.getUserSleepStatistics(userId, startDate, endDate));
    }
}