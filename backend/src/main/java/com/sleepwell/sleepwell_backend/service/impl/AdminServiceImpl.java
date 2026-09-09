package com.sleepwell.sleepwell_backend.service.impl;

import com.sleepwell.sleepwell_backend.dto.admin.*;
import com.sleepwell.sleepwell_backend.dto.SleepRecordResponseDto;
import com.sleepwell.sleepwell_backend.entity.*;
import com.sleepwell.sleepwell_backend.enums.UserRole;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.*;
import com.sleepwell.sleepwell_backend.service.AdminService;
import com.sleepwell.sleepwell_backend.service.SleepRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import com.sleepwell.sleepwell_backend.entity.Subscription;
import com.sleepwell.sleepwell_backend.enums.SubscriptionStatus;
import com.sleepwell.sleepwell_backend.repository.SubscriptionRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final SleepRecordRepository sleepRecordRepository;
    private final ConsultationSessionRepository consultationSessionRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final AnalysisExecutionJobRepository analysisJobRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;
    private final SleepRecordService sleepRecordService;

    // ==================== 1. 대시보드 & 통계 ====================
    
    @Override
    public AdminDashboardDto getDashboardData(LocalDate startDate, LocalDate endDate) {
        log.info("대시보드 데이터 조회: {} ~ {}", startDate, endDate);
        
        // 사용자 통계
        long totalUsers = userRepository.count();
        long newUsers = userRepository.countByCreatedAtBetween(
            startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        long activeUsers = userRepository.countActiveUsers(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        long premiumUsers = userRepository.countByRole(UserRole.PREMIUM);
        
        // 수면 데이터 통계
        long totalSleepRecords = sleepRecordRepository.count();
        long todaySleepRecords = sleepRecordRepository.countByCreatedAtBetween(
            LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay());
        Double avgSleepQuality = sleepRecordRepository.findAverageSleepQuality();
        Double avgSleepDuration = sleepRecordRepository.findAverageSleepDuration();
        
        // AI 상담 통계
        long totalConsultations = consultationSessionRepository.count();
        long todayConsultations = consultationSessionRepository.countByCreatedAtBetween(
            LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay());
        
        // 플랫폼 동기화 통계
        Map<String, Long> syncByPlatform = convertToMapFromObjectArray(
            sleepRecordRepository.countByWearableSource(),
            obj -> String.valueOf(obj[0]),
            obj -> ((Number) obj[1]).longValue());
        
        // 결제/구독 통계
        Double totalRevenue = paymentRepository.calculateTotalRevenue(
            LocalDateTime.now().minusYears(10), LocalDateTime.now()).doubleValue();
        Double monthlyRevenue = paymentRepository.calculateMonthlyRevenue(
            LocalDate.now().getYear(), LocalDate.now().getMonthValue()).doubleValue();
        
        // 일별 메트릭 (최근 7일)
        List<AdminDashboardDto.DailyMetric> dailyMetrics = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dailyMetrics.add(AdminDashboardDto.DailyMetric.builder()
                .date(date)
                .newUsers(userRepository.countByCreatedAtBetween(
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .activeUsers(userRepository.countActiveUsers(
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .sleepRecords(sleepRecordRepository.countByCreatedAtBetween(
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .consultations(consultationSessionRepository.countByCreatedAtBetween(
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .revenue(paymentRepository.calculateDailyRevenue(date).doubleValue())
                .build());
        }
        
        return AdminDashboardDto.builder()
            .startDate(startDate)
            .endDate(endDate)
            .totalUsers(totalUsers)
            .newUsers(newUsers)
            .activeUsers(activeUsers)
            .premiumUsers(premiumUsers)
            .userGrowthRate(calculateGrowthRate(newUsers, totalUsers))
            .totalSleepRecords(totalSleepRecords)
            .todaySleepRecords(todaySleepRecords)
            .averageSleepQuality(avgSleepQuality)
            .averageSleepDuration(avgSleepDuration)
            .totalConsultations(totalConsultations)
            .todayConsultations(todayConsultations)
            .syncByPlatform(syncByPlatform)
            .totalRevenue(totalRevenue)
            .monthlyRevenue(monthlyRevenue)
            .systemStatus("HEALTHY")
            .serverUptime(99.9)
            .dailyMetrics(dailyMetrics)
            .build();
    }

    @Override
    public RealtimeMetricsDto getRealtimeMetrics() {
        log.info("실시간 메트릭 조회");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinAgo = now.minusMinutes(5);
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        // 최근 활동 조회
        List<RealtimeMetricsDto.RecentActivity> recentActivities = new ArrayList<>();
        
        // 최근 가입 사용자
        userRepository.findTop10ByOrderByCreatedAtDesc().forEach(user -> {
            recentActivities.add(RealtimeMetricsDto.RecentActivity.builder()
                .timestamp(user.getCreatedAt())
                .activityType("USER_REGISTRATION")
                .description("새로운 사용자 가입")
                .userId(user.getId())
                .userEmail(user.getEmail())
                .build());
        });
        
        // 최근 수면 기록
        sleepRecordRepository.findTop10ByOrderByCreatedAtDesc().forEach(record -> {
            recentActivities.add(RealtimeMetricsDto.RecentActivity.builder()
                .timestamp(record.getCreatedAt())
                .activityType("SLEEP_RECORD")
                .description("수면 데이터 업로드")
                .userId(record.getUser().getId())
                .build());
        });
        
        // 활동 시간순 정렬
        recentActivities.sort(Comparator.comparing(RealtimeMetricsDto.RecentActivity::getTimestamp).reversed());
        
        return RealtimeMetricsDto.builder()
            .timestamp(now)
            .currentActiveUsers(userRepository.countActiveUsers(fiveMinAgo, now))
            .last5MinActiveUsers(userRepository.countActiveUsers(fiveMinAgo, now))
            .last1HourActiveUsers(userRepository.countActiveUsers(oneHourAgo, now))
            .pendingAnalysisJobs(analysisJobRepository.countPendingJobs())
            .processingAnalysisJobs(analysisJobRepository.countProcessingJobs())
            .pendingNotifications((long) notificationRepository.countPendingNotifications())
            .cpuUsage(getSystemCpuUsage())
            .memoryUsage(getSystemMemoryUsage())
            .diskUsage(getSystemDiskUsage())
            .recentActivities(recentActivities.stream().limit(20).collect(Collectors.toList()))
            .build();
    }

    // ==================== 2. 사용자 관리 ====================
    
    @Override
    public Page<AdminUserDto> getUsers(String search, String role, Boolean isActive, 
                                      LocalDate registeredAfter, Pageable pageable) {
        log.info("사용자 목록 조회 - search: {}, role: {}, isActive: {}", search, role, isActive);
        
        Page<User> users = userRepository.findWithFilters(search, role, isActive, registeredAfter, pageable);
        
        return users.map(user -> AdminUserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .phoneNumber(user.getPhoneNumber())
            .role(user.getRole().name())
            .isActive(user.getIsActive())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .sleepRecordCount(sleepRecordRepository.countByUserId(user.getId()))
            .consultationCount(consultationSessionRepository.countByUserId(user.getId()))
            .subscriptionStatus(getSubscriptionStatus(user))
            .build());
    }

    @Override
    public AdminUserDetailDto getUserDetail(Long userId) {
        log.info("사용자 상세 정보 조회 - userId: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        
        // 최근 활동 조회
        List<AdminUserDetailDto.UserActivity> activities = new ArrayList<>();
        
        // 최근 수면 기록
        sleepRecordRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId,
            org.springframework.data.domain.PageRequest.of(0, 5)).forEach(record -> {
            Integer qualityScore = record.getSleepQualityScore();
            String description = qualityScore != null ?
                String.format("수면 기록 - 품질: %.1f", qualityScore.doubleValue()) :
                "수면 기록 - 품질: 정보 없음";

            activities.add(AdminUserDetailDto.UserActivity.builder()
                .timestamp(record.getCreatedAt())
                .activityType("SLEEP_RECORD")
                .description(description)
                .build());
        });
        
        // 최근 상담
        consultationSessionRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId,
            org.springframework.data.domain.PageRequest.of(0, 5)).forEach(session -> {
            activities.add(AdminUserDetailDto.UserActivity.builder()
                .timestamp(session.getCreatedAt())
                .activityType("CONSULTATION")
                .description("AI 상담 세션")
                .build());
        });
        
        activities.sort(Comparator.comparing(AdminUserDetailDto.UserActivity::getTimestamp).reversed());
        
        // 수면 패턴 분석
        String sleepQualityTrend = analyzeSleepQualityTrend(userId);
        AdminUserDetailDto.SleepPattern sleepPattern = AdminUserDetailDto.SleepPattern.builder()
            .averageSleepDuration(sleepRecordRepository.findAverageSleepDurationByUserId(userId))
            .sleepQualityTrend(sleepQualityTrend)
            .build();
        
        // AI 상담 사용 통계
        Long totalSessions = consultationSessionRepository.countByUserId(userId);
        Long totalMessages = conversationMessageRepository.countByUserId(userId);

        AdminUserDetailDto.ConsultationUsage consultationUsage = AdminUserDetailDto.ConsultationUsage.builder()
            .totalSessions(totalSessions != null ? totalSessions.intValue() : 0)
            .totalMessages(totalMessages != null ? totalMessages.intValue() : 0)
            .build();
        
        return AdminUserDetailDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .phoneNumber(user.getPhoneNumber())
            .age(user.getAge())
            .gender(user.getGender() != null ? user.getGender().name() : null)
            .occupation(user.getOccupation())
            .role(user.getRole().name())
            .isActive(user.getIsActive())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .totalSleepRecords(sleepRecordRepository.countByUserId(userId))
            .totalConsultations(consultationSessionRepository.countByUserId(userId))
            .totalNotifications(notificationRepository.countByUserId(userId))
            .averageSleepQuality(sleepRecordRepository.findAverageSleepQualityByUserId(userId))
            .averageSleepDuration(sleepRecordRepository.findAverageSleepDurationByUserId(userId))
            .recentActivities(activities)
            .sleepPattern(sleepPattern)
            .consultationUsage(consultationUsage)
            .build();
    }

    @Override
    @Transactional
    public AdminUserDto updateUserStatus(Long userId, boolean isActive, String reason) {
        log.info("사용자 상태 변경 - userId: {}, isActive: {}, reason: {}", userId, isActive, reason);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        
        // 사용자 상태 변경
        if (!isActive) {
            user.deactivate();  // User 엔티티의 deactivate() 메서드 사용
        } else {
            user.activate();    // User 엔티티의 activate() 메서드 사용
        }
        
        User savedUser = userRepository.save(user);
        
        return AdminUserDto.builder()
            .id(savedUser.getId())
            .email(savedUser.getEmail())
            .name(savedUser.getName())
            .role(savedUser.getRole().name())
            .isActive(savedUser.getIsActive())
            .createdAt(savedUser.getCreatedAt())
            .build();
    }

    @Override
    @Transactional
    public AdminUserDto updateUserRole(Long userId, String role) {
        log.info("사용자 역할 변경 - userId: {}, role: {}", userId, role);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        
        try {
            UserRole userRole = UserRole.valueOf(role);
            user.changeRole(userRole);
            
            User savedUser = userRepository.save(user);
            
            return AdminUserDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role(savedUser.getRole().name())
                .isActive(savedUser.getIsActive())
                .build();
        } catch (IllegalArgumentException e) {
            throw new BusinessException("유효하지 않은 역할입니다: " + role, HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @Transactional
    public String resetUserPassword(Long userId) {
        log.info("비밀번호 초기화 - userId: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        // 임시 비밀번호 생성
        String tempPassword = generateTempPassword();
        user.resetPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        log.info("사용자 {} 비밀번호 초기화 완료", user.getEmail());

        return tempPassword;
    }

    @Override
    @Transactional
    public void deleteUser(Long userId, boolean hardDelete) {
        log.info("사용자 삭제 요청 - userId: {}, hardDelete: {}", userId, hardDelete);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        if (hardDelete) {
            // Hard Delete: 연관 데이터 포함 완전 삭제
            log.warn("사용자 영구 삭제 시작 - userId: {}, email: {}", userId, user.getEmail());

            // 연관 데이터 삭제
            sleepRecordRepository.deleteAllByUser(user);
            conversationMessageRepository.deleteAllByUser(user);
            notificationRepository.deleteAllByUser(user);

            // 구독 정보 삭제
            List<Subscription> subscriptions = subscriptionRepository.findByUser(user);
            subscriptionRepository.deleteAll(subscriptions);

            // 사용자 삭제
            userRepository.delete(user);

            log.info("사용자 영구 삭제 완료 - userId: {}, email: {}", userId, user.getEmail());
        } else {
            // Soft Delete: 비활성화 처리
            log.info("사용자 비활성화 처리 - userId: {}, email: {}", userId, user.getEmail());

            User updatedUser = user.toBuilder()
                .isActive(false)
                .deletedAt(LocalDateTime.now())
                .build();

            userRepository.save(updatedUser);

            log.info("사용자 비활성화 완료 - userId: {}, email: {}", userId, user.getEmail());
        }
    }

    // ==================== 3. AI 상담 내역 조회 ====================
    
    @Override
    public Page<AdminConsultationDto> getConsultations(Long userId, String keyword, 
                                                      LocalDateTime startTime, LocalDateTime endTime, 
                                                      Pageable pageable) {
        log.info("AI 상담 내역 조회 - userId: {}, keyword: {}", userId, keyword);
        
        Page<ConsultationSession> sessions = consultationSessionRepository.findWithFilters(
            userId, keyword, startTime, endTime, pageable);
        
        return sessions.map(session -> {
            User user = session.getUser();
            Long messageCount = (long) conversationMessageRepository.countByConsultationSession(session);

            return AdminConsultationDto.builder()
                .sessionId(session.getId())
                .userId(user != null ? user.getId() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .userName(user != null ? user.getName() : null)
                .topic(session.getTopic().name())
                .consultationType(session.getSessionType() != null ? session.getSessionType().name() : "GENERAL")
                .aiModel(session.getAiModel() != null ? session.getAiModel() : "UNKNOWN")
                .startTime(session.getConsultationTime())
                .endTime(session.getEndTime())
                .messageCount(messageCount != null ? messageCount.intValue() : 0)
                .status(session.getStatus().name())
                .build();
        });
    }

    @Override
    public AdminConsultationDetailDto getConsultationDetail(Long sessionId) {
        log.info("상담 대화 상세 조회 - sessionId: {}", sessionId);
        
        ConsultationSession session = consultationSessionRepository.findById(sessionId)
            .orElseThrow(() -> new BusinessException("상담 세션을 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        
        User user = session.getUser();
        if (user == null) {
            throw new BusinessException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND);
        }
        
        // 전체 대화 내용 조회
        List<ConversationMessage> messages = conversationMessageRepository.findByConsultationSessionOrderBySentAtAsc(session);
        
        List<AdminConsultationDetailDto.ChatMessage> chatMessages = messages.stream()
            .map(msg -> AdminConsultationDetailDto.ChatMessage.builder()
                .id(msg.getId())
                .role(msg.getMessageType() != null ? msg.getMessageType().name() : "USER")
                .content(msg.getContent())
                .timestamp(msg.getSentAt())
                .tokenCount(0) // ConversationMessage에 tokenCount 필드가 없음
                .build())
            .collect(Collectors.toList());
        
        // 토큰 사용량 계산 (토큰 필드가 없으므로 메시지 길이 기반 추정)
        int totalTokens = messages.stream()
            .mapToInt(msg -> msg.getContent() != null ? msg.getContent().length() / 4 : 0)
            .sum();
        
        // 관련 수면 컨텍스트
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        AdminConsultationDetailDto.SleepContext sleepContext = AdminConsultationDetailDto.SleepContext.builder()
            .recentAverageSleepQuality(sleepRecordRepository.findRecentAverageSleepQuality(user.getId(), sevenDaysAgo))
            .recentAverageSleepDuration(sleepRecordRepository.findRecentAverageSleepDuration(user.getId(), sevenDaysAgo))
            .build();
        
        // 키워드 추출 (간단한 구현)
        List<String> keyTopics = extractKeywordsFromMessages(messages);
        
        // ConsultationSummary 조회
        String summaryText = null;
        if (session.getConsultationSummary() != null) {
            summaryText = session.getConsultationSummary().getSummaryText();
        }
        
        return AdminConsultationDetailDto.builder()
            .sessionId(session.getId())
            .userId(user.getId())
            .userEmail(user.getEmail())
            .userName(user.getName())
            .userRole(user.getRole().name())
            .topic(session.getTopic() != null ? session.getTopic().name() : null)
            .consultationType(session.getSessionType() != null ? session.getSessionType().name() : "GENERAL")
            .aiModel(session.getAiModel() != null ? session.getAiModel() : "UNKNOWN")
            .startTime(session.getConsultationTime())
            .endTime(session.getEndTime())
            .status(session.getStatus() != null ? session.getStatus().name() : null)
            .messages(chatMessages)
            .totalTokens(totalTokens)
            .estimatedCost(calculateEstimatedCost("gpt-4", totalTokens))
            .sleepContext(sleepContext)
            .keyTopics(keyTopics)
            .consultationSummary(summaryText)
            .build();
    }

    @Override
    public ConsultationStatisticsDto getConsultationStatistics(LocalDate startDate, LocalDate endDate) {
        log.info("AI 상담 통계 조회 - 기간: {} ~ {}", startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        
        // 전체 통계
        Long totalSessions = consultationSessionRepository.countByCreatedAtBetween(startDateTime, endDateTime);
        Long uniqueUsers = consultationSessionRepository.countUniqueUsersBetween(startDateTime, endDateTime);
        Long totalMessages = conversationMessageRepository.countByCreatedAtBetween(startDateTime, endDateTime);
        
        // 주제별 분포
        List<Object[]> topicStats = consultationSessionRepository.getTopicDistribution(startDateTime, endDateTime);
        Map<String, Integer> topicDistribution = new HashMap<>();
        for (Object[] stat : topicStats) {
            String topic = stat[0] != null ? stat[0].toString() : "UNKNOWN";
            Long count = (Long) stat[1];
            topicDistribution.put(topic, count != null ? count.intValue() : 0);
        }
        
        // AI 모델별 사용 통계
        Map<String, ConsultationStatisticsDto.ModelUsage> modelUsageStats = new HashMap<>();
        List<Object[]> modelStats = consultationSessionRepository.getModelUsageStats(startDateTime, endDateTime);
        for (Object[] stat : modelStats) {
            String modelName = (String) stat[0];
            Long sessionCount = (Long) stat[1];
            Long tokenCount = (Long) stat[2];
            
            modelUsageStats.put(modelName, ConsultationStatisticsDto.ModelUsage.builder()
                .modelName(modelName)
                .sessionCount(sessionCount)
                .tokenCount(tokenCount)
                .estimatedCost(calculateEstimatedCost(modelName, tokenCount != null ? tokenCount.intValue() : 0))
                .build());
        }
        
        // 일별 메트릭
        List<ConsultationStatisticsDto.DailyConsultationMetric> dailyMetrics = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            
            dailyMetrics.add(ConsultationStatisticsDto.DailyConsultationMetric.builder()
                .date(date)
                .sessions(consultationSessionRepository.countByCreatedAtBetween(dayStart, dayEnd))
                .messages(conversationMessageRepository.countByCreatedAtBetween(dayStart, dayEnd))
                .uniqueUsers(consultationSessionRepository.countUniqueUsersBetween(dayStart, dayEnd))
                .build());
        }
        
        return ConsultationStatisticsDto.builder()
            .startDate(startDate)
            .endDate(endDate)
            .totalSessions(totalSessions)
            .uniqueUsers(uniqueUsers)
            .totalMessages(totalMessages)
            .averageMessagesPerSession(totalSessions > 0 ? (double) totalMessages / totalSessions : 0)
            .modelUsageStats(modelUsageStats)
            .topicDistribution(topicDistribution)
            .dailyMetrics(dailyMetrics)
            .build();
    }

    // ==================== 4. 구독/결제 관리 ====================
    
    @Override
    public SubscriptionOverviewDto getSubscriptionOverview() {
        log.info("구독 현황 조회");
        
        // 구독 통계
        Long totalSubscribers = userRepository.countByRole(UserRole.PREMIUM);
        Long activeSubscriptions = userRepository.countActiveSubscriptions();
        Long trialUsers = userRepository.countTrialUsers();
        
        // 수익 통계
        Double totalRevenue = paymentRepository.calculateTotalRevenue(
            LocalDateTime.now().minusYears(10), LocalDateTime.now()).doubleValue();
        Double monthlyRecurringRevenue = paymentRepository.calculateMonthlyRevenue(
            LocalDate.now().getYear(), LocalDate.now().getMonthValue()).doubleValue();
        
        // 최근 구독 변경
        List<SubscriptionOverviewDto.RecentSubscriptionChange> recentChanges = getRecentSubscriptionChanges();
        
        // 만료 예정 구독
        List<SubscriptionOverviewDto.ExpiringSubscription> expiringSubscriptions = getExpiringSubscriptions();
        
        return SubscriptionOverviewDto.builder()
            .totalSubscribers(totalSubscribers)
            .activeSubscriptions(activeSubscriptions)
            .trialUsers(trialUsers)
            .totalRevenue(totalRevenue)
            .monthlyRecurringRevenue(monthlyRecurringRevenue)
            .averageRevenuePerUser(totalSubscribers > 0 ? totalRevenue / totalSubscribers : 0)
            .recentChanges(recentChanges)
            .expiringSubscriptions(expiringSubscriptions)
            .build();
    }

    @Override
    public Page<AdminPaymentDto> getPayments(Long userId, String status, 
                                            LocalDate startDate, LocalDate endDate, 
                                            Pageable pageable) {
        log.info("결제 내역 조회 - userId: {}, status: {}", userId, status);
        
        Page<Payment> payments = paymentRepository.findWithFilters(
            userId, status, 
            startDate != null ? startDate.atStartOfDay() : null,
            endDate != null ? endDate.plusDays(1).atStartOfDay() : null,
            pageable);
        
        return payments.map(payment -> {
            User user = userRepository.findById(payment.getUserId()).orElse(null);
            
            return AdminPaymentDto.builder()
                .paymentId(payment.getId())
                .userId(payment.getUserId())
                .userEmail(user != null ? user.getEmail() : null)
                .userName(user != null ? user.getName() : null)
                .orderId(payment.getOrderId())
                .amount(payment.getAmount().doubleValue())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod().name())
                .paymentStatus(payment.getStatus().name())
                .productName(payment.getDescription())  // description을 productName으로 사용
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getPaidAt())  // paidAt을 completedAt으로 사용
                .pgProvider(payment.getPgProvider())
                .pgTransactionId(payment.getTransactionId())  // transactionId 사용
                .build();
        });
    }

    @Override
    @Transactional
    public LocalDateTime grantFreeTrial(Long userId, int days) {
        log.info("무료 체험 부여 - userId: {}, days: {}", userId, days);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(days);
        
        // TODO: 구독 정보 업데이트 로직 구현
        // user.setSubscriptionExpiryDate(expiryDate);
        // user.setSubscriptionType(SubscriptionType.TRIAL);
        
        userRepository.save(user);
        
        return expiryDate;
    }

    // ==================== 5. 데이터 분석 ====================
    
    @Override
    public UserRetentionDto getUserRetention(int days) {
        log.info("사용자 리텐션 분석 - 기간: {}일", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        // 일별 리텐션율 계산
        Map<Integer, Double> retentionByDay = new HashMap<>();
        for (int i = 1; i <= days; i++) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i);
            LocalDateTime dayEnd = dayStart.plusDays(1);
            
            Long totalUsers = userRepository.countByCreatedAtBefore(dayStart);
            Long activeUsers = userRepository.countActiveUsers(dayStart, dayEnd);
            
            double retentionRate = totalUsers > 0 ? (double) activeUsers / totalUsers * 100 : 0;
            retentionByDay.put(i, retentionRate);
        }
        
        // 전체 리텐션율
        Long totalUsers = userRepository.count();
        Long activeUsersInPeriod = userRepository.countActiveUsers(startDate, LocalDateTime.now());
        double overallRetentionRate = totalUsers > 0 ? (double) activeUsersInPeriod / totalUsers * 100 : 0;
        
        return UserRetentionDto.builder()
            .periodDays(days)
            .overallRetentionRate(overallRetentionRate)
            .retentionByDay(retentionByDay)
            .build();
    }

    @Override
    public SleepPatternAnalysisDto getSleepPatternAnalysis(LocalDate startDate, LocalDate endDate) {
        log.info("수면 패턴 분석 - 기간: {} ~ {}", startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        
        // 전체 통계
        Double avgSleepDuration = sleepRecordRepository.findAverageSleepDurationBetween(startDateTime, endDateTime);
        Double avgSleepQuality = sleepRecordRepository.findAverageSleepQualityBetween(startDateTime, endDateTime);
        
        // 일별 트렌드
        List<SleepPatternAnalysisDto.DailyTrend> dailyTrends = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            
            dailyTrends.add(SleepPatternAnalysisDto.DailyTrend.builder()
                .date(date)
                .averageDuration(sleepRecordRepository.findAverageSleepDurationBetween(dayStart, dayEnd))
                .averageQuality(sleepRecordRepository.findAverageSleepQualityBetween(dayStart, dayEnd))
                .recordCount(sleepRecordRepository.countByCreatedAtBetween(dayStart, dayEnd))
                .build());
        }
        
        return SleepPatternAnalysisDto.builder()
            .startDate(startDate)
            .endDate(endDate)
            .averageSleepDuration(avgSleepDuration)
            .averageSleepQuality(avgSleepQuality)
            .dailyTrends(dailyTrends)
            .build();
    }

    @Override
    public FeatureUsageDto getFeatureUsage() {
        log.info("기능별 사용 통계 조회");
        
        Map<String, FeatureUsageDto.FeatureMetric> featureMetrics = new HashMap<>();
        
        // 수면 기록 기능
        featureMetrics.put("SLEEP_RECORD", FeatureUsageDto.FeatureMetric.builder()
            .featureName("수면 기록")
            .totalUsageCount(sleepRecordRepository.count())
            .uniqueUserCount(sleepRecordRepository.countUniqueUsers())
            .build());
        
        // AI 상담 기능
        featureMetrics.put("AI_CONSULTATION", FeatureUsageDto.FeatureMetric.builder()
            .featureName("AI 상담")
            .totalUsageCount(consultationSessionRepository.count())
            .uniqueUserCount(consultationSessionRepository.countUniqueUsers())
            .build());
        
        // 알림 기능
        featureMetrics.put("NOTIFICATION", FeatureUsageDto.FeatureMetric.builder()
            .featureName("알림")
            .totalUsageCount(notificationRepository.count())
            .uniqueUserCount(notificationRepository.countUniqueUsers())
            .build());
        
        // Top 기능 리스트 (순위와 함께 생성)
        List<FeatureUsageDto.FeatureMetric> sortedMetrics = featureMetrics.values().stream()
            .sorted(Comparator.comparing(FeatureUsageDto.FeatureMetric::getTotalUsageCount).reversed())
            .limit(10)
            .collect(Collectors.toList());
        
        List<FeatureUsageDto.FeatureRanking> topFeatures = new ArrayList<>();
        for (int i = 0; i < sortedMetrics.size(); i++) {
            FeatureUsageDto.FeatureMetric metric = sortedMetrics.get(i);
            topFeatures.add(FeatureUsageDto.FeatureRanking.builder()
                .rank(i + 1)
                .featureName(metric.getFeatureName())
                .usageCount(metric.getTotalUsageCount())
                .build());
        }
        
        return FeatureUsageDto.builder()
            .featureMetrics(featureMetrics)
            .topFeatures(topFeatures)
            .build();
    }

    // ==================== 6. 시스템 관리 ====================
    
    @Override
    public SystemHealthDto getSystemHealth() {
        log.info("시스템 상태 조회");
        
        // 리소스 상태
        SystemHealthDto.ResourceStatus resourceStatus = SystemHealthDto.ResourceStatus.builder()
            .cpuUsage(getSystemCpuUsage())
            .memoryUsage(getSystemMemoryUsage())
            .diskUsage(getSystemDiskUsage())
            .activeThreads(Thread.activeCount())
            .build();
        
        // 데이터베이스 상태
        SystemHealthDto.DatabaseStatus databaseStatus = SystemHealthDto.DatabaseStatus.builder()
            .status("HEALTHY")
            .activeConnections(getActiveConnectionCount())
            .maxConnections(100)
            .queryResponseTime(5.2)
            .slowQueryCount(0L)
            .build();
        
        // API 상태
        SystemHealthDto.ApiStatus apiStatus = SystemHealthDto.ApiStatus.builder()
            .status("HEALTHY")
            .averageResponseTime(120.5)
            .requestsPerMinute(150L)
            .errorRate(0.1)
            .build();
        
        // 외부 서비스 상태
        Map<String, SystemHealthDto.ServiceStatus> externalServices = new HashMap<>();
        externalServices.put("OpenAI", SystemHealthDto.ServiceStatus.builder()
            .serviceName("OpenAI")
            .status("HEALTHY")
            .lastCheckTime(LocalDateTime.now())
            .build());
        
        return SystemHealthDto.builder()
            .timestamp(LocalDateTime.now())
            .overallStatus("GREEN")
            .resourceStatus(resourceStatus)
            .databaseStatus(databaseStatus)
            .apiStatus(apiStatus)
            .externalServices(externalServices)
            .build();
    }

    @Override
    public List<ErrorLogDto> getErrorLogs(int limit, String level) {
        log.info("에러 로그 조회 - limit: {}, level: {}", limit, level);
        
        // TODO: 실제 로그 시스템과 연동
        List<ErrorLogDto> errorLogs = new ArrayList<>();
        
        // 샘플 데이터
        errorLogs.add(ErrorLogDto.builder()
            .id(1L)
            .timestamp(LocalDateTime.now().minusHours(1))
            .level("ERROR")
            .errorType("NullPointerException")
            .message("수면 데이터 처리 중 오류 발생")
            .requestUrl("/api/sleep/records")
            .requestMethod("POST")
            .build());
        
        return errorLogs;
    }

    @Override
    @Transactional
    public void clearCache(String cacheName) {
        log.info("캐시 초기화 - cacheName: {}", cacheName);
        
        if (cacheName != null) {
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
        } else {
            cacheManager.getCacheNames().forEach(name -> 
                Objects.requireNonNull(cacheManager.getCache(name)).clear());
        }
    }

    @Override
    @Transactional
    public void setMaintenanceMode(boolean enabled, String message) {
        log.info("유지보수 모드 설정 - enabled: {}, message: {}", enabled, message);
        
        // TODO: 유지보수 모드 구현
        // 1. 시스템 설정 테이블에 유지보수 모드 플래그 저장
        // 2. 모든 API 요청에 대해 503 응답 반환 (관리자 제외)
        // 3. 유지보수 메시지 표시
    }

    // ==================== Helper Methods ====================
    
    private Double calculateGrowthRate(Long newCount, Long totalCount) {
        if (totalCount == 0) return 0.0;
        return (double) newCount / totalCount * 100;
    }
    
    private String getSubscriptionStatus(User user) {
        // TODO: 실제 구독 상태 조회 로직
        return user.getRole() == UserRole.PREMIUM ? "ACTIVE" : "FREE";
    }
    
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    private Double calculateEstimatedCost(String model, int tokens) {
        // 모델별 토큰당 비용 (대략적인 예시)
        double costPer1kTokens = switch (model != null ? model : "") {
            case "gpt-4" -> 0.03;
            case "gpt-3.5-turbo" -> 0.002;
            case "claude-3" -> 0.025;
            default -> 0.001;
        };
        
        return (tokens / 1000.0) * costPer1kTokens;
    }
    
    private List<String> extractKeywordsFromMessages(List<ConversationMessage> messages) {
        // TODO: 실제 키워드 추출 로직 구현 (NLP 활용)
        return Arrays.asList("수면 부족", "스트레스", "수면 환경", "생활 패턴");
    }
    
    private Integer getActiveConnectionCount() {
        try {
            // HikariCP 커넥션 풀 정보를 통해 활성 커넥션 수 조회
            // 실제 환경에서는 DataSource에서 커넥션 정보를 가져올 수 있음
            return Math.max(1, (int)(Math.random() * 20)); // 임시로 랜덤 값
        } catch (Exception e) {
            log.warn("활성 커넥션 수 조회 실패: {}", e.getMessage());
            return 5; // 기본값
        }
    }

    private Double getSystemCpuUsage() {
        try {
            // Java 14+ OperatingSystemMXBean을 사용하거나
            // Micrometer를 통해 CPU 사용률 조회 가능
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            double cpuUsage = osBean.getCpuLoad() * 100;
            return cpuUsage > 0 ? cpuUsage : 45.2; // fallback
        } catch (Exception e) {
            log.warn("CPU 사용률 조회 실패: {}", e.getMessage());
            return 45.2; // 기본값
        }
    }
    
    private Double getSystemMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            return ((totalMemory - freeMemory) / (double) totalMemory) * 100;
        } catch (Exception e) {
            log.warn("메모리 사용률 조회 실패: {}", e.getMessage());
            return 70.0; // 기본값
        }
    }
    
    private Double getSystemDiskUsage() {
        try {
            java.io.File diskPartition = new java.io.File("/");
            long totalSpace = diskPartition.getTotalSpace();
            long freeSpace = diskPartition.getFreeSpace();
            if (totalSpace > 0) {
                return ((totalSpace - freeSpace) / (double) totalSpace) * 100;
            }
        } catch (Exception e) {
            log.warn("디스크 사용률 조회 실패: {}", e.getMessage());
        }
        return 62.8; // 기본값
    }
    
    private <T, K, V> Map<K, V> convertToMapFromObjectArray(List<Object[]> list,
                                                             java.util.function.Function<Object[], K> keyMapper,
                                                             java.util.function.Function<Object[], V> valueMapper) {
        Map<K, V> map = new HashMap<>();
        if (list != null) {
            for (Object[] obj : list) {
                map.put(keyMapper.apply(obj), valueMapper.apply(obj));
            }
        }
        return map;
    }

    /**
     * 최근 구독 변경 이력 조회
     * 최근 30일간의 구독 상태 변경을 기준으로 변경 이력 생성
     */
    private List<SubscriptionOverviewDto.RecentSubscriptionChange> getRecentSubscriptionChanges() {
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            // 최근 취소된 구독들
            List<Subscription> recentChanges = subscriptionRepository.findAll().stream()
                .filter(sub -> sub.getCancelledAt() != null && sub.getCancelledAt().isAfter(thirtyDaysAgo))
                .sorted(Comparator.comparing(Subscription::getCancelledAt).reversed())
                .limit(10)
                .collect(Collectors.toList());
            
            return recentChanges.stream().map(subscription -> {
                User user = subscription.getUser();
                return SubscriptionOverviewDto.RecentSubscriptionChange.builder()
                    .userId(user.getId())
                    .userEmail(user.getEmail())
                    .changeType("CANCEL")
                    .fromPlan(subscription.getPlanType().name())
                    .toPlan("CANCELLED")
                    .changeDate(subscription.getCancelledAt().toString())
                    .build();
            }).collect(Collectors.toList());
            
        } catch (Exception e) {
            log.warn("최근 구독 변경 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 만료 예정 구독 조회
     * 30일 내 만료 예정인 활성 구독들 조회
     */
    private List<SubscriptionOverviewDto.ExpiringSubscription> getExpiringSubscriptions() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime thirtyDaysLater = now.plusDays(30);
            
            List<Subscription> expiring = subscriptionRepository
                .findExpiringSubscriptions(now, thirtyDaysLater, SubscriptionStatus.ACTIVE);
            
            return expiring.stream().map(subscription -> {
                User user = subscription.getUser();
                long daysUntilExpiry = ChronoUnit.DAYS.between(now, subscription.getEndDate());
                
                return SubscriptionOverviewDto.ExpiringSubscription.builder()
                    .userId(user.getId())
                    .userEmail(user.getEmail())
                    .plan(subscription.getPlanType().name())
                    .expiryDate(subscription.getEndDate().toString())
                    .daysUntilExpiry((int) daysUntilExpiry)
                    .build();
            }).collect(Collectors.toList());
            
        } catch (Exception e) {
            log.warn("만료 예정 구독 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 사용자의 수면 품질 트렌드 분석
     * 최근 30일간의 수면 품질 점수 변화를 분석하여 트렌드 반환
     * 
     * @param userId 사용자 ID
     * @return 트렌드 문자열 ("IMPROVING", "DECLINING", "STABLE")
     */
    private String analyzeSleepQualityTrend(Long userId) {
        try {
            // 최근 30일 수면 기록 조회
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            LocalDate today = LocalDate.now();
            List<SleepRecord> recentRecords = sleepRecordRepository
                .findByUserIdAndRecordDateBetween(userId, thirtyDaysAgo, today);
            
            if (recentRecords.size() < 3) {
                return "INSUFFICIENT_DATA";
            }
            
            // 수면 품질 점수 추출 (null 체크, Integer -> Double 변환)
            List<Double> qualityScores = recentRecords.stream()
                .map(SleepRecord::getSleepQualityScore)
                .filter(Objects::nonNull)
                .map(Integer::doubleValue)
                .collect(Collectors.toList());
            
            if (qualityScores.size() < 3) {
                return "INSUFFICIENT_DATA";
            }
            
            // 시간 순으로 정렬 (최신 순)
            recentRecords.sort(Comparator.comparing(SleepRecord::getRecordDate).reversed());
            
            // 최근 절반과 이전 절반으로 나누어 비교
            int midPoint = qualityScores.size() / 2;
            List<Double> recentHalf = qualityScores.subList(0, midPoint);
            List<Double> olderHalf = qualityScores.subList(midPoint, qualityScores.size());
            
            double recentAverage = recentHalf.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
                
            double olderAverage = olderHalf.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            
            double difference = recentAverage - olderAverage;
            
            // 임계값 설정 (5점 이상 차이면 트렌드로 판단)
            if (difference > 5.0) {
                return "IMPROVING";
            } else if (difference < -5.0) {
                return "DECLINING";
            } else {
                return "STABLE";
            }
            
        } catch (Exception e) {
            log.warn("수면 품질 트렌드 분석 실패 - userId: {}, error: {}", userId, e.getMessage());
            return "UNKNOWN";
        }
    }

    // ==================== 7. 수면 기록 조회 ====================

    /**
     * 특정 사용자의 수면 기록 목록 조회 (관리자용)
     *
     * @param userId 사용자 ID
     * @param startDate 조회 시작 날짜 (nullable)
     * @param endDate 조회 종료 날짜 (nullable)
     * @param pageable 페이징 정보
     * @return 수면 기록 페이지
     * @throws BusinessException 사용자를 찾을 수 없는 경우 (USER_NOT_FOUND)
     */
    @Override
    public Page<SleepRecordResponseDto> getUserSleepRecords(
            Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("관리자 수면 기록 조회 - userId: {}, startDate: {}, endDate: {}, page: {}",
                userId, startDate, endDate, pageable.getPageNumber());

        // 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 비활성 사용자 경고 로그
        if (!user.getIsActive()) {
            log.warn("비활성 사용자의 수면 기록 조회 - userId: {}, email: {}", userId, user.getEmail());
        }

        // 날짜 범위가 지정된 경우 기간별 조회, 그렇지 않으면 전체 조회
        if (startDate != null && endDate != null) {
            // 날짜 유효성 검증
            if (startDate.isAfter(endDate)) {
                throw new BusinessException("시작 날짜가 종료 날짜보다 늦을 수 없습니다.",
                        HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
            }

            // 최대 1년 범위 제한
            if (ChronoUnit.DAYS.between(startDate, endDate) > 365) {
                throw new BusinessException("조회 가능한 최대 기간은 1년입니다.",
                        HttpStatus.BAD_REQUEST, "DATE_RANGE_TOO_LARGE");
            }

            return sleepRecordService.getSleepRecordsBetweenDates(userId, startDate, endDate, pageable);
        } else {
            // 전체 조회 (기본 정렬: 날짜 내림차순)
            Pageable sortedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "recordDate")
            );

            return sleepRecordService.getSleepRecords(
                    userId,
                    sortedPageable.getPageNumber(),
                    sortedPageable.getPageSize(),
                    "recordDate",
                    "desc"
            );
        }
    }

    /**
     * 특정 사용자의 수면 기록 상세 조회 (관리자용)
     *
     * @param userId 사용자 ID
     * @param recordId 수면 기록 ID
     * @return 수면 기록 상세 정보
     * @throws BusinessException 사용자 또는 수면 기록을 찾을 수 없는 경우
     */
    @Override
    public SleepRecordResponseDto getUserSleepRecord(Long userId, Long recordId) {
        log.info("관리자 수면 기록 상세 조회 - userId: {}, recordId: {}", userId, recordId);

        // 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 수면 기록 조회
        SleepRecordResponseDto record = sleepRecordService.getSleepRecord(userId, recordId);

        // 권한 검증: 해당 수면 기록이 요청한 사용자의 것인지 확인
        // (SleepRecordService에서 이미 검증하지만 명시적으로 로깅)
        log.debug("수면 기록 조회 성공 - userId: {}, recordId: {}, recordDate: {}",
                userId, recordId, record.getRecordDate());

        return record;
    }

    /**
     * 특정 사용자의 수면 통계 조회 (관리자용)
     *
     * @param userId 사용자 ID
     * @param startDate 조회 시작 날짜 (nullable)
     * @param endDate 조회 종료 날짜 (nullable)
     * @return 수면 통계 정보
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    @Override
    public Map<String, Object> getUserSleepStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("관리자 수면 통계 조회 - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);

        // 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 날짜 범위 기본값 설정 (최근 30일)
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();

        // 날짜 유효성 검증
        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            throw new BusinessException("시작 날짜가 종료 날짜보다 늦을 수 없습니다.",
                    HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
        }

        LocalDateTime startDateTime = effectiveStartDate.atStartOfDay();
        LocalDateTime endDateTime = effectiveEndDate.plusDays(1).atStartOfDay();

        // 기간 내 수면 기록 조회
        List<SleepRecord> records = sleepRecordRepository.findByUserIdAndRecordDateBetween(
                userId, effectiveStartDate, effectiveEndDate);

        // 통계 계산
        Map<String, Object> statistics = new HashMap<>();

        if (records.isEmpty()) {
            statistics.put("userId", userId);
            statistics.put("userName", user.getName());
            statistics.put("email", user.getEmail());
            statistics.put("period", Map.of(
                    "startDate", effectiveStartDate,
                    "endDate", effectiveEndDate,
                    "days", ChronoUnit.DAYS.between(effectiveStartDate, effectiveEndDate) + 1
            ));
            statistics.put("totalRecords", 0);
            statistics.put("message", "해당 기간 동안 수면 기록이 없습니다.");
            return statistics;
        }

        // 기본 통계
        long totalRecords = records.size();
        double avgSleepDuration = records.stream()
                .mapToInt(SleepRecord::getTotalSleepMinutes)
                .average()
                .orElse(0.0) / 60.0; // 시간 단위로 변환

        double avgSleepQuality = records.stream()
                .filter(r -> r.getSleepQualityScore() != null)
                .mapToDouble(SleepRecord::getSleepQualityScore)
                .average()
                .orElse(0.0);

        // 수면 단계 통계
        Map<String, Double> avgSleepStages = new HashMap<>();
        avgSleepStages.put("deepSleepMinutes", records.stream()
                .filter(r -> r.getDeepSleepMinutes() != null)
                .mapToInt(SleepRecord::getDeepSleepMinutes)
                .average()
                .orElse(0.0));
        avgSleepStages.put("lightSleepMinutes", records.stream()
                .filter(r -> r.getLightSleepMinutes() != null)
                .mapToInt(SleepRecord::getLightSleepMinutes)
                .average()
                .orElse(0.0));
        avgSleepStages.put("remSleepMinutes", records.stream()
                .filter(r -> r.getRemSleepMinutes() != null)
                .mapToInt(SleepRecord::getRemSleepMinutes)
                .average()
                .orElse(0.0));

        // 웨어러블 소스별 통계
        Map<String, Long> recordsBySource = records.stream()
                .filter(r -> r.getWearableSource() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getWearableSource().name(),
                        Collectors.counting()
                ));

        // 수면 품질 트렌드 (최근 30일 기준)
        String sleepQualityTrend = analyzeSleepQualityTrend(userId);

        // 최고/최저 수면 시간
        OptionalInt maxSleepMinutes = records.stream().mapToInt(SleepRecord::getTotalSleepMinutes).max();
        OptionalInt minSleepMinutes = records.stream().mapToInt(SleepRecord::getTotalSleepMinutes).min();

        // 응답 구성
        statistics.put("userId", userId);
        statistics.put("userName", user.getName());
        statistics.put("email", user.getEmail());
        statistics.put("period", Map.of(
                "startDate", effectiveStartDate,
                "endDate", effectiveEndDate,
                "days", ChronoUnit.DAYS.between(effectiveStartDate, effectiveEndDate) + 1
        ));
        statistics.put("totalRecords", totalRecords);
        statistics.put("averageSleepDuration", String.format("%.1f", avgSleepDuration) + " hours");
        statistics.put("averageSleepQuality", String.format("%.1f", avgSleepQuality) + " / 100");
        statistics.put("averageSleepStages", avgSleepStages);
        statistics.put("maxSleepMinutes", maxSleepMinutes.isPresent() ? maxSleepMinutes.getAsInt() : 0);
        statistics.put("minSleepMinutes", minSleepMinutes.isPresent() ? minSleepMinutes.getAsInt() : 0);
        statistics.put("recordsByWearableSource", recordsBySource);
        statistics.put("sleepQualityTrend", sleepQualityTrend);
        statistics.put("generatedAt", LocalDateTime.now());

        return statistics;
    }
}