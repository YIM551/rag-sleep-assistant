package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.admin.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AdminService {
    
    // 대시보드 & 통계
    AdminDashboardDto getDashboardData(LocalDate startDate, LocalDate endDate);
    RealtimeMetricsDto getRealtimeMetrics();
    
    // 사용자 관리
    Page<AdminUserDto> getUsers(String search, String role, Boolean isActive, LocalDate registeredAfter, Pageable pageable);
    AdminUserDetailDto getUserDetail(Long userId);
    AdminUserDto updateUserStatus(Long userId, boolean isActive, String reason);
    AdminUserDto updateUserRole(Long userId, String role);
    String resetUserPassword(Long userId);
    void deleteUser(Long userId, boolean hardDelete);
    
    // AI 상담 내역 조회
    Page<AdminConsultationDto> getConsultations(Long userId, String keyword, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    AdminConsultationDetailDto getConsultationDetail(Long sessionId);
    ConsultationStatisticsDto getConsultationStatistics(LocalDate startDate, LocalDate endDate);
    
    // 구독/결제 관리
    SubscriptionOverviewDto getSubscriptionOverview();
    Page<AdminPaymentDto> getPayments(Long userId, String status, LocalDate startDate, LocalDate endDate, Pageable pageable);
    LocalDateTime grantFreeTrial(Long userId, int days);
    
    // 데이터 분석
    UserRetentionDto getUserRetention(int days);
    SleepPatternAnalysisDto getSleepPatternAnalysis(LocalDate startDate, LocalDate endDate);
    FeatureUsageDto getFeatureUsage();
    
    // 시스템 관리
    SystemHealthDto getSystemHealth();
    List<ErrorLogDto> getErrorLogs(int limit, String level);
    void clearCache(String cacheName);
    void setMaintenanceMode(boolean enabled, String message);

    // 수면 기록 조회
    Page<com.sleepwell.sleepwell_backend.dto.SleepRecordResponseDto> getUserSleepRecords(
            Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);
    com.sleepwell.sleepwell_backend.dto.SleepRecordResponseDto getUserSleepRecord(
            Long userId, Long recordId);
    Map<String, Object> getUserSleepStatistics(Long userId, LocalDate startDate, LocalDate endDate);
}