package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.notification.NotificationTemplateDto;
import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface NotificationTemplateService {

    // === 템플릿 CRUD 메서드들 ===

    /**
     * 새로운 알림 템플릿을 생성합니다.
     *
     * @param name               템플릿 이름
     * @param description        템플릿 설명
     * @param type               알림 타입
     * @param category           템플릿 카테고리
     * @param locale             언어/로케일
     * @param titleTemplate      제목 템플릿
     * @param messageTemplate    메시지 템플릿
     * @param defaultPriority    기본 우선순위
     * @param defaultExpiryHours 기본 만료 시간
     * @return 생성된 템플릿 DTO
     */
    NotificationTemplateDto createTemplate(NotificationTemplateDto dto);

    /**
     * 템플릿을 업데이트합니다.
     *
     * @param templateId         템플릿 ID
     * @param dto                업데이트할 데이터가 담긴 DTO
     * @return 업데이트된 템플릿 DTO
     */
    NotificationTemplateDto updateTemplate(Long templateId, NotificationTemplateDto dto);

    /**
     * 템플릿을 삭제합니다 (비활성화).
     *
     * @param templateId 템플릿 ID
     */
    void deleteTemplate(Long templateId);

    /**
     * 템플릿을 활성화합니다.
     *
     * @param templateId 템플릿 ID
     * @return 활성화된 템플릿 DTO
     */
    NotificationTemplateDto activateTemplate(Long templateId);

    /**
     * 템플릿을 비활성화합니다.
     *
     * @param templateId 템플릿 ID
     * @return 비활성화된 템플릿 DTO
     */
    NotificationTemplateDto deactivateTemplate(Long templateId);

    // === 템플릿 조회 메서드들 ===

    /**
     * 특정 템플릿을 조회합니다.
     *
     * @param templateId 템플릿 ID
     * @return 템플릿 DTO
     */
    NotificationTemplateDto getTemplate(Long templateId);

    /**
     * 모든 템플릿을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 페이징된 템플릿 DTO 목록
     */
    Page<NotificationTemplateDto> getAllTemplates(Pageable pageable);

    /**
     * 템플릿 이름으로 조회합니다.
     *
     * @param name 템플릿 이름
     * @return 템플릿 DTO
     */
    NotificationTemplateDto getTemplateByName(String name);

    /**
     * 모든 활성화된 템플릿을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 페이징된 템플릿 DTO 목록
     */
    Page<NotificationTemplateDto> getActiveTemplates(Pageable pageable);

    /**
     * 특정 타입의 활성화된 템플릿들을 조회합니다.
     *
     * @param type 알림 타입
     * @return 템플릿 DTO 목록
     */
    List<NotificationTemplateDto> getActiveTemplatesByType(NotificationType type);

    /**
     * 특정 타입과 로케일의 활성화된 템플릿들을 조회합니다.
     *
     * @param type   알림 타입
     * @param locale 언어/로케일
     * @return 템플릿 DTO 목록
     */
    List<NotificationTemplateDto> getActiveTemplatesByTypeAndLocale(NotificationType type, String locale);

    /**
     * 가장 많이 사용된 템플릿을 조회합니다.
     *
     * @param type   알림 타입
     * @param locale 언어/로케일
     * @return 템플릿 DTO (없으면 null)
     */
    NotificationTemplateDto getMostUsedTemplate(NotificationType type, String locale);

    /**
     * 카테고리별 활성화된 템플릿들을 조회합니다.
     *
     * @param category 템플릿 카테고리
     * @return 템플릿 DTO 목록
     */
    List<NotificationTemplateDto> getActiveTemplatesByCategory(String category);

    /**
     * 템플릿을 검색합니다.
     *
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 페이징된 템플릿 DTO 목록
     */
    Page<NotificationTemplateDto> searchTemplates(String keyword, Pageable pageable);

    // === 템플릿 사용 메서드들 ===

    /**
     * 템플릿을 사용하여 알림 제목과 메시지를 생성합니다.
     *
     * @param templateId 템플릿 ID
     * @param variables  치환할 변수 맵
     * @return 생성된 제목과 메시지를 담은 맵 (title, message 키)
     */
    Map<String, String> generateNotificationContent(Long templateId, Map<String, Object> variables);

    /**
     * 템플릿 이름을 사용하여 알림 제목과 메시지를 생성합니다.
     *
     * @param templateName 템플릿 이름
     * @param variables    치환할 변수 맵
     * @return 생성된 제목과 메시지를 담은 맵 (title, message 키)
     */
    Map<String, String> generateNotificationContentByName(String templateName, Map<String, Object> variables);

    /**
     * 최적의 템플릿을 자동으로 선택하여 알림 제목과 메시지를 생성합니다.
     *
     * @param type      알림 타입
     * @param locale    언어/로케일
     * @param variables 치환할 변수 맵
     * @return 생성된 제목과 메시지를 담은 맵 (title, message 키)
     */
    Map<String, String> generateNotificationContentAuto(NotificationType type, String locale, Map<String, Object> variables);

    // === 통계 및 관리 메서드들 ===

    /**
     * 템플릿 사용 통계를 조회합니다.
     *
     * @return 통계 정보 맵
     */
    Map<String, Object> getTemplateUsageStatistics();

    /**
     * 사용되지 않은 템플릿들을 조회합니다.
     *
     * @return 템플릿 DTO 목록
     */
    List<NotificationTemplateDto> getUnusedTemplates();

    /**
     * 인기있는 템플릿들을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 페이징된 템플릿 DTO 목록
     */
    Page<NotificationTemplateDto> getPopularTemplates(Pageable pageable);

    /**
     * 최근에 사용된 템플릿들을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 페이징된 템플릿 DTO 목록
     */
    Page<NotificationTemplateDto> getRecentlyUsedTemplates(Pageable pageable);

    /**
     * 템플릿 이름 중복을 확인합니다.
     *
     * @param name       템플릿 이름
     * @param excludeId  제외할 템플릿 ID (수정 시 자신 제외)
     * @return 중복 여부
     */
    boolean isTemplateNameDuplicate(String name, Long excludeId);

    /**
     * 기본 템플릿들을 초기화합니다.
     * 시스템 시작 시 호출되어 기본 템플릿들을 생성합니다.
     */
    void initializeDefaultTemplates();
} 