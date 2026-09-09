package com.sleepwell.sleepwell_backend.dto.notification;

import com.sleepwell.sleepwell_backend.entity.NotificationTemplate;
import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.enums.Priority;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateDto {

    private Long id;

    @NotBlank(message = "템플릿 이름은 필수입니다")
    @Size(max = 100, message = "템플릿 이름은 100자를 초과할 수 없습니다")
    @Schema(description = "템플릿 이름", example = "수면 알림 템플릿", required = true, maxLength = 100)
    private String name;

    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;

    @NotNull(message = "알림 타입은 필수입니다")
    private NotificationType type;

    @NotBlank(message = "카테고리는 필수입니다")
    @Size(max = 50, message = "카테고리는 50자를 초과할 수 없습니다")
    private String category;

    @Size(max = 10, message = "로케일은 10자를 초과할 수 없습니다")
    private String locale;

    @NotBlank(message = "제목 템플릿은 필수입니다")
    @Size(max = 500, message = "제목 템플릿은 500자를 초과할 수 없습니다")
    @Schema(description = "알림 제목 템플릿", example = "{{userName}}님의 수면 시간입니다", required = true, maxLength = 500)
    private String titleTemplate;

    @NotBlank(message = "메시지 템플릿은 필수입니다")
    @Size(max = 2000, message = "메시지 템플릿은 2000자를 초과할 수 없습니다")
    @Schema(description = "알림 메시지 템플릿", example = "좋은 수면을 위해 {{sleepTime}}에 잠자리에 드세요", required = true, maxLength = 2000)
    private String messageTemplate;

    private Priority defaultPriority;

    @Min(value = 0, message = "만료 시간은 0보다 작을 수 없습니다")
    @Max(value = 8760, message = "만료 시간은 8760시간(1년)을 초과할 수 없습니다")
    private Integer defaultExpiryHours;
    private boolean isActive;
    private String version;
    private String createdBy;
    private String modifiedBy;
    private Long usageCount;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NotificationTemplateDto(NotificationTemplate template) {
        this.id = template.getId();
        this.name = template.getName();
        this.description = template.getDescription();
        this.type = template.getType();
        this.category = template.getCategory();
        this.locale = template.getLocale();
        this.titleTemplate = template.getTitleTemplate();
        this.messageTemplate = template.getMessageTemplate();
        this.defaultPriority = template.getDefaultPriority();
        this.defaultExpiryHours = template.getDefaultExpiryHours();
        this.isActive = template.isActive();
        this.version = template.getVersion();
        this.createdBy = template.getCreatedBy();
        this.modifiedBy = template.getModifiedBy();
        this.usageCount = template.getUsageCount();
        this.lastUsedAt = template.getLastUsedAt();
        this.createdAt = template.getCreatedAt();
        this.updatedAt = template.getUpdatedAt();
    }
} 