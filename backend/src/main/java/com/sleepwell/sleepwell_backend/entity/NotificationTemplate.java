package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.dto.notification.NotificationTemplateDto;
import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.enums.Priority;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "NOTIFICATION_TEMPLATES", indexes = {
        @Index(name = "idx_template_type_locale", columnList = "type, locale"),
        @Index(name = "idx_template_active", columnList = "isActive"),
        @Index(name = "idx_template_category", columnList = "category")
})
public class NotificationTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 템플릿 이름 (식별용)
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * 템플릿 설명
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 알림 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /**
     * 템플릿 카테고리 (수면, 구독, 일반 등)
     */
    @Column(nullable = false)
    private String category;

    /**
     * 언어/로케일 (ko, en, ja 등)
     */
    @Builder.Default
    @Column(nullable = false)
    private String locale = "ko";

    /**
     * 제목 템플릿
     * 변수는 {{variableName}} 형식으로 표현
     */
    @Column(nullable = false)
    private String titleTemplate;

    /**
     * 메시지 템플릿
     * 변수는 {{variableName}} 형식으로 표현
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String messageTemplate;

    /**
     * 기본 우선순위
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority defaultPriority = Priority.NORMAL;

    /**
     * 기본 만료 시간 (시간 단위)
     * null인 경우 만료되지 않음
     */
    private Integer defaultExpiryHours;

    /**
     * 템플릿 활성화 여부
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    /**
     * 템플릿 버전
     */
    @Builder.Default
    @Column(nullable = false)
    private String version = "1.0";



    /**
     * 사용 횟수 (통계용)
     */
    @Builder.Default
    @Column(nullable = false)
    private Long usageCount = 0L;

    /**
     * 마지막 사용 시간
     */
    private LocalDateTime lastUsedAt;



    /**
     * 템플릿에 변수를 치환하여 제목을 생성합니다.
     *
     * @param variables 치환할 변수 맵
     * @return 치환된 제목
     */
    public String generateTitle(Map<String, Object> variables) {
        return substituteVariables(titleTemplate, variables);
    }

    /**
     * 템플릿에 변수를 치환하여 메시지를 생성합니다.
     *
     * @param variables 치환할 변수 맵
     * @return 치환된 메시지
     */
    public String generateMessage(Map<String, Object> variables) {
        return substituteVariables(messageTemplate, variables);
    }

    /**
     * 템플릿 문자열에서 변수를 치환합니다.
     * 변수 형식: {{variableName}}
     *
     * @param template  템플릿 문자열
     * @param variables 치환할 변수 맵
     * @return 치환된 문자열
     */
    private String substituteVariables(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }

        String result = template;
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            Object value = variables.get(variableName);
            
            if (value != null) {
                result = result.replace("{{" + variableName + "}}", value.toString());
            } else {
                // 변수가 없는 경우 빈 문자열로 치환하거나 원본 유지
                result = result.replace("{{" + variableName + "}}", "");
            }
        }

        return result;
    }

    /**
     * 템플릿 사용 횟수를 증가시킵니다.
     */
    public void incrementUsageCount() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * 템플릿을 비활성화합니다.
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 템플릿을 활성화합니다.
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 템플릿을 업데이트합니다.
     *
     * @param titleTemplate     새로운 제목 템플릿
     * @param messageTemplate   새로운 메시지 템플릿
     * @param defaultPriority   새로운 기본 우선순위
     * @param defaultExpiryHours 새로운 기본 만료 시간
     */
    public void updateTemplate(String titleTemplate, String messageTemplate, 
                              Priority defaultPriority, Integer defaultExpiryHours) {
        this.titleTemplate = titleTemplate;
        this.messageTemplate = messageTemplate;
        this.defaultPriority = defaultPriority;
        this.defaultExpiryHours = defaultExpiryHours;
    }

    /**
     * DTO로부터 템플릿을 업데이트합니다.
     *
     * @param dto 업데이트할 데이터가 담긴 DTO
     */
    public void updateFromDto(NotificationTemplateDto dto) {
        if (dto.getName() != null) this.name = dto.getName();
        if (dto.getDescription() != null) this.description = dto.getDescription();
        if (dto.getType() != null) this.type = dto.getType();
        if (dto.getCategory() != null) this.category = dto.getCategory();
        if (dto.getLocale() != null) this.locale = dto.getLocale();
        if (dto.getTitleTemplate() != null) this.titleTemplate = dto.getTitleTemplate();
        if (dto.getMessageTemplate() != null) this.messageTemplate = dto.getMessageTemplate();
        if (dto.getDefaultPriority() != null) this.defaultPriority = dto.getDefaultPriority();
        if (dto.getDefaultExpiryHours() != null) this.defaultExpiryHours = dto.getDefaultExpiryHours();
    }

    /**
     * 템플릿 버전을 업데이트합니다.
     *
     * @param newVersion 새로운 버전
     */
    public void updateVersion(String newVersion) {
        this.version = newVersion;
    }

    /**
     * 기본 만료 시간을 기준으로 만료 시간을 계산합니다.
     *
     * @return 계산된 만료 시간 (null인 경우 만료되지 않음)
     */
    public LocalDateTime calculateExpiryTime() {
        if (defaultExpiryHours == null) {
            return null;
        }
        return LocalDateTime.now().plusHours(defaultExpiryHours);
    }
} 