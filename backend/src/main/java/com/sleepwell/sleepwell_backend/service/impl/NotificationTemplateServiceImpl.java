package com.sleepwell.sleepwell_backend.service.impl;

import com.sleepwell.sleepwell_backend.dto.notification.NotificationTemplateDto;
import com.sleepwell.sleepwell_backend.entity.NotificationTemplate;
import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.enums.Priority;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.repository.NotificationTemplateRepository;
import com.sleepwell.sleepwell_backend.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;

    // === 템플릿 CRUD 메서드들 ===

    @Override
    @Transactional
    public NotificationTemplateDto createTemplate(NotificationTemplateDto dto) {
        // 템플릿 이름 중복 확인 (컨트롤러에서 기본 검증 완료됨)
        if (templateRepository.existsByName(dto.getName())) {
            throw new BusinessException("이미 존재하는 템플릿 이름입니다: " + dto.getName(), HttpStatus.CONFLICT);
        }

        NotificationTemplate template = NotificationTemplate.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .category(dto.getCategory())
                .locale(dto.getLocale() != null ? dto.getLocale() : "ko")
                .titleTemplate(dto.getTitleTemplate())
                .messageTemplate(dto.getMessageTemplate())
                .defaultPriority(dto.getDefaultPriority() != null ? dto.getDefaultPriority() : Priority.NORMAL)
                .defaultExpiryHours(dto.getDefaultExpiryHours())
                .isActive(true)
                .version("1.0")
                .build();

        templateRepository.save(template);
        log.info("새로운 알림 템플릿 생성 완료: {} (ID: {})", dto.getName(), template.getId());

        return new NotificationTemplateDto(template);
    }

    @Override
    @Transactional
    public NotificationTemplateDto updateTemplate(Long templateId, NotificationTemplateDto dto) {
        NotificationTemplate template = findTemplateById(templateId);

        // 이름 변경 시 중복 확인 (컨트롤러에서 기본 검증 완료됨)
        if (dto.getName() != null && !dto.getName().equals(template.getName())) {
            if (templateRepository.existsByName(dto.getName())) {
                throw new BusinessException("이미 존재하는 템플릿 이름입니다: " + dto.getName(), HttpStatus.CONFLICT);
            }
        }

        template.updateFromDto(dto);

        log.info("알림 템플릿 업데이트 완료: {} (ID: {})", template.getName(), templateId);

        return new NotificationTemplateDto(template);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId) {
        NotificationTemplate template = findTemplateById(templateId);
        template.deactivate();

        log.info("알림 템플릿 비활성화 완료: {} (ID: {})", template.getName(), templateId);
    }

    @Override
    @Transactional
    public NotificationTemplateDto activateTemplate(Long templateId) {
        NotificationTemplate template = findTemplateById(templateId);
        template.activate();
        templateRepository.save(template); // 변경사항 저장

        log.info("알림 템플릿 활성화 완료: {} (ID: {})", template.getName(), templateId);

        return new NotificationTemplateDto(template);
    }

    @Override
    @Transactional
    public NotificationTemplateDto deactivateTemplate(Long templateId) {
        NotificationTemplate template = findTemplateById(templateId);
        template.deactivate();
        templateRepository.save(template); // 변경사항 저장

        log.info("알림 템플릿 비활성화 완료: {} (ID: {})", template.getName(), templateId);

        return new NotificationTemplateDto(template);
    }

    // === 템플릿 조회 메서드들 ===

    @Override
    public NotificationTemplateDto getTemplate(Long templateId) {
        NotificationTemplate template = findTemplateById(templateId);
        return new NotificationTemplateDto(template);
    }

    @Override
    public Page<NotificationTemplateDto> getAllTemplates(Pageable pageable) {
        return templateRepository.findAll(pageable)
                .map(NotificationTemplateDto::new);
    }

    @Override
    public NotificationTemplateDto getTemplateByName(String name) {
        NotificationTemplate template = templateRepository.findByNameAndIsActive(name, true)
                .orElseThrow(() -> new BusinessException("활성화된 템플릿을 찾을 수 없습니다: " + name, HttpStatus.NOT_FOUND));
        return new NotificationTemplateDto(template);
    }

    @Override
    public Page<NotificationTemplateDto> getActiveTemplates(Pageable pageable) {
        return templateRepository.findByIsActiveTrue(pageable)
                .map(NotificationTemplateDto::new);
    }

    @Override
    public List<NotificationTemplateDto> getActiveTemplatesByType(NotificationType type) {
        return templateRepository.findActiveByType(type)
                .stream()
                .map(NotificationTemplateDto::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTemplateDto> getActiveTemplatesByTypeAndLocale(NotificationType type, String locale) {
        return templateRepository.findActiveByTypeAndLocale(type, locale)
                .stream()
                .map(NotificationTemplateDto::new)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationTemplateDto getMostUsedTemplate(NotificationType type, String locale) {
        return templateRepository.findMostUsedByTypeAndLocale(type, locale)
                .map(NotificationTemplateDto::new)
                .orElse(null);
    }

    @Override
    public List<NotificationTemplateDto> getActiveTemplatesByCategory(String category) {
        return templateRepository.findActiveByCategoryOrderByName(category)
                .stream()
                .map(NotificationTemplateDto::new)
                .collect(Collectors.toList());
    }

    @Override
    public Page<NotificationTemplateDto> searchTemplates(String keyword, Pageable pageable) {
        return templateRepository.searchActiveTemplates(keyword, pageable)
                .map(NotificationTemplateDto::new);
    }

    // === 템플릿 사용 메서드들 ===

    @Override
    @Transactional
    public Map<String, String> generateNotificationContent(Long templateId, Map<String, Object> variables) {
        NotificationTemplate template = findTemplateById(templateId);

        if (!template.isActive()) {
            throw new BusinessException("비활성화된 템플릿입니다: " + template.getName(), HttpStatus.BAD_REQUEST);
        }

        // 템플릿 사용 횟수 증가
        template.incrementUsageCount();

        String title = template.generateTitle(variables);
        String message = template.generateMessage(variables);

        log.debug("템플릿 사용하여 알림 내용 생성: {} (ID: {})", template.getName(), templateId);

        Map<String, String> result = new HashMap<>();
        result.put("title", title);
        result.put("message", message);
        return result;
    }

    @Override
    @Transactional
    public Map<String, String> generateNotificationContentByName(String templateName, Map<String, Object> variables) {
        NotificationTemplate template = templateRepository.findByNameAndIsActive(templateName, true)
                .orElseThrow(() -> new BusinessException("활성화된 템플릿을 찾을 수 없습니다: " + templateName, HttpStatus.NOT_FOUND));

        // 템플릿 사용 횟수 증가
        template.incrementUsageCount();

        String title = template.generateTitle(variables);
        String message = template.generateMessage(variables);

        log.debug("템플릿 이름으로 알림 내용 생성: {}", templateName);

        Map<String, String> result = new HashMap<>();
        result.put("title", title);
        result.put("message", message);
        return result;
    }

    @Override
    @Transactional
    public Map<String, String> generateNotificationContentAuto(NotificationType type, String locale, Map<String, Object> variables) {
        // 가장 많이 사용된 템플릿을 우선 선택
        NotificationTemplate template = templateRepository.findMostUsedByTypeAndLocale(type, locale)
                .orElse(null);

        // 없으면 해당 타입의 첫 번째 활성화된 템플릿 선택
        if (template == null) {
            List<NotificationTemplate> templates = templateRepository.findActiveByType(type);
            if (templates.isEmpty()) {
                throw new BusinessException("해당 타입의 활성화된 템플릿이 없습니다: " + type, HttpStatus.NOT_FOUND);
            }
            template = templates.get(0);
        }

        // 템플릿 사용 횟수 증가
        template.incrementUsageCount();

        String title = template.generateTitle(variables);
        String message = template.generateMessage(variables);

        log.debug("자동 선택된 템플릿으로 알림 내용 생성: {} (타입: {}, 로케일: {})", template.getName(), type, locale);

        Map<String, String> result = new HashMap<>();
        result.put("title", title);
        result.put("message", message);
        return result;
    }

    // === 통계 및 관리 메서드들 ===

    @Override
    public Map<String, Object> getTemplateUsageStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 전체 템플릿 개수
        stats.put("totalTemplates", templateRepository.count());
        stats.put("activeTemplates", templateRepository.countByIsActiveTrue());

        // 타입별 통계
        List<Object[]> typeStats = templateRepository.getStatsByType();
        Map<String, Map<String, Object>> typeStatsMap = new HashMap<>();
        for (Object[] stat : typeStats) {
            Map<String, Object> typeStat = new HashMap<>();
            typeStat.put("templateCount", stat[1]);
            typeStat.put("totalUsage", stat[2]);
            typeStatsMap.put(stat[0].toString(), typeStat);
        }
        stats.put("typeStatistics", typeStatsMap);

        // 카테고리별 통계
        List<Object[]> categoryStats = templateRepository.getStatsByCategory();
        Map<String, Map<String, Object>> categoryStatsMap = new HashMap<>();
        for (Object[] stat : categoryStats) {
            Map<String, Object> categoryStat = new HashMap<>();
            categoryStat.put("templateCount", stat[1]);
            categoryStat.put("totalUsage", stat[2]);
            categoryStatsMap.put(stat[0].toString(), categoryStat);
        }
        stats.put("categoryStatistics", categoryStatsMap);

        // 로케일별 통계
        List<Object[]> localeStats = templateRepository.getStatsByLocale();
        Map<String, Map<String, Object>> localeStatsMap = new HashMap<>();
        for (Object[] stat : localeStats) {
            Map<String, Object> localeStat = new HashMap<>();
            localeStat.put("templateCount", stat[1]);
            localeStat.put("totalUsage", stat[2]);
            localeStatsMap.put(stat[0].toString(), localeStat);
        }
        stats.put("localeStatistics", localeStatsMap);

        return stats;
    }

    @Override
    public List<NotificationTemplateDto> getUnusedTemplates() {
        return templateRepository.findUnused()
                .stream()
                .map(NotificationTemplateDto::new)
                .collect(Collectors.toList());
    }

    @Override
    public Page<NotificationTemplateDto> getPopularTemplates(Pageable pageable) {
        return templateRepository.findActiveOrderByUsageCountDesc(pageable)
                .map(NotificationTemplateDto::new);
    }

    @Override
    public Page<NotificationTemplateDto> getRecentlyUsedTemplates(Pageable pageable) {
        return templateRepository.findActiveOrderByLastUsedAtDesc(pageable)
                .map(NotificationTemplateDto::new);
    }

    @Override
    public boolean isTemplateNameDuplicate(String name, Long excludeId) {
        if (excludeId == null) {
            return templateRepository.existsByName(name);
        }
        return templateRepository.existsByNameExcludingId(name, excludeId);
    }

    @Override
    @Transactional
    public void initializeDefaultTemplates() {
        log.info("기본 알림 템플릿 초기화 시작");

        // 수면 기록 알림 템플릿
        createDefaultTemplateIfNotExists(
                "sleep_reminder_default",
                "기본 수면 기록 알림",
                NotificationType.SLEEP_REMINDER,
                "수면",
                "ko",
                "💤 수면 기록 시간입니다",
                "안녕하세요 {{userName}}님! 오늘의 수면을 기록해보세요. 좋은 밤 되세요! 🌙",
                Priority.NORMAL,
                24
        );

        // 수면 분석 완료 알림 템플릿
        createDefaultTemplateIfNotExists(
                "sleep_analysis_complete_default",
                "기본 수면 분석 완료 알림",
                NotificationType.SLEEP_ANALYSIS_COMPLETE,
                "수면",
                "ko",
                "📊 수면 분석이 완료되었습니다",
                "{{userName}}님의 {{date}} 수면 분석이 완료되었습니다. 수면 점수: {{sleepScore}}점 ✨",
                Priority.HIGH,
                72
        );

        // 개인화된 수면 팁 알림 템플릿
        createDefaultTemplateIfNotExists(
                "personalized_sleep_tip_default",
                "기본 개인화 수면 팁 알림",
                NotificationType.PERSONALIZED_SLEEP_TIP,
                "수면",
                "ko",
                "💡 {{userName}}님을 위한 수면 팁",
                "{{tip}} 더 나은 수면을 위해 시도해보세요! 🌟",
                Priority.NORMAL,
                168
        );

        // 구독 만료 임박 알림 템플릿
        createDefaultTemplateIfNotExists(
                "subscription_expiring_soon_default",
                "기본 구독 만료 임박 알림",
                NotificationType.SUBSCRIPTION_EXPIRING_SOON,
                "구독",
                "ko",
                "⚠️ 구독이 {{daysLeft}}일 후 만료됩니다",
                "{{userName}}님, 프리미엄 구독이 {{expiryDate}}에 만료됩니다. 지속적인 서비스 이용을 위해 갱신해주세요! 💎",
                Priority.HIGH,
                24
        );

        // 구독 만료 알림 템플릿
        createDefaultTemplateIfNotExists(
                "subscription_expired_default",
                "기본 구독 만료 알림",
                NotificationType.SUBSCRIPTION_EXPIRED,
                "구독",
                "ko",
                "❌ 구독이 만료되었습니다",
                "{{userName}}님, 프리미엄 구독이 만료되어 일부 기능이 제한됩니다. 다시 구독하여 모든 기능을 이용해보세요! 🔄",
                Priority.URGENT,
                72
        );

        // 결제 성공 알림 템플릿
        createDefaultTemplateIfNotExists(
                "payment_success_default",
                "기본 결제 성공 알림",
                NotificationType.PAYMENT_SUCCESS,
                "결제",
                "ko",
                "✅ 결제가 완료되었습니다",
                "{{userName}}님, {{amount}}원 결제가 성공적으로 완료되었습니다. 프리미엄 서비스를 즐겨보세요! 🎉",
                Priority.HIGH,
                72
        );

        // 결제 실패 알림 템플릿
        createDefaultTemplateIfNotExists(
                "payment_failed_default",
                "기본 결제 실패 알림",
                NotificationType.PAYMENT_FAILED,
                "결제",
                "ko",
                "❌ 결제에 실패했습니다",
                "{{userName}}님, 결제 처리 중 문제가 발생했습니다. 결제 정보를 확인하고 다시 시도해주세요. 💳",
                Priority.URGENT,
                48
        );

        // 새로운 기능 업데이트 알림 템플릿
        createDefaultTemplateIfNotExists(
                "new_feature_update_default",
                "기본 새 기능 업데이트 알림",
                NotificationType.NEW_FEATURE_UPDATE,
                "공지",
                "ko",
                "🚀 새로운 기능이 추가되었습니다",
                "{{userName}}님, {{featureName}} 기능이 새롭게 추가되었습니다! {{featureDescription}} 지금 확인해보세요! ✨",
                Priority.NORMAL,
                168
        );

        // 일반 공지사항 알림 템플릿
        createDefaultTemplateIfNotExists(
                "general_announcement_default",
                "기본 일반 공지사항 알림",
                NotificationType.GENERAL_ANNOUNCEMENT,
                "공지",
                "ko",
                "📢 {{title}}",
                "{{content}}",
                Priority.NORMAL,
                168
        );

        // 시스템 점검 공지 알림 템플릿
        createDefaultTemplateIfNotExists(
                "system_maintenance_default",
                "기본 시스템 점검 공지 알림",
                NotificationType.SYSTEM_MAINTENANCE,
                "공지",
                "ko",
                "🔧 시스템 점검 안내",
                "{{userName}}님, {{maintenanceDate}}에 시스템 점검이 예정되어 있습니다. 점검 시간: {{duration}} ⏰",
                Priority.HIGH,
                48
        );

        log.info("기본 알림 템플릿 초기화 완료");
    }

    private void createDefaultTemplateIfNotExists(String name, String description, NotificationType type,
                                                 String category, String locale, String titleTemplate,
                                                 String messageTemplate, Priority defaultPriority,
                                                 Integer defaultExpiryHours) {
        if (!templateRepository.existsByName(name)) {
            NotificationTemplate template = NotificationTemplate.builder()
                    .name(name)
                    .description(description)
                    .type(type)
                    .category(category)
                    .locale(locale)
                    .titleTemplate(titleTemplate)
                    .messageTemplate(messageTemplate)
                    .defaultPriority(defaultPriority)
                    .defaultExpiryHours(defaultExpiryHours)
                    .isActive(true)
                    .version("1.0")
                    .build();

            templateRepository.save(template);
            log.debug("기본 템플릿 생성: {}", name);
        }
    }

    private NotificationTemplate findTemplateById(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("템플릿을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

} 