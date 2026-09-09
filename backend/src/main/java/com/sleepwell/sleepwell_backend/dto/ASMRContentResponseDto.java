package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.entity.ASMRContent;
import com.sleepwell.sleepwell_backend.enums.ASMRCategory;
import com.sleepwell.sleepwell_backend.enums.ASMRContentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ASMR 콘텐츠 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ASMRContentResponseDto {

    private Long id;
    private String title;
    private String description;
    private ASMRCategory category;
    private ASMRContentStatus status;
    private Integer durationMinutes;
    private String highQualityUrl;
    private String mediumQualityUrl;
    private String lowQualityUrl;
    private String previewUrl;
    private String thumbnailUrl;
    private String targetMood;
    private Long totalPlayCount;
    private BigDecimal rating;
    private Boolean isPremium;
    private LocalDateTime createdAt;

    public static ASMRContentResponseDto from(ASMRContent content) {
        return ASMRContentResponseDto.builder()
                .id(content.getId())
                .title(content.getTitle())
                .description(content.getDescription())
                .category(content.getCategory())
                .status(content.getStatus())
                .durationMinutes(content.getDurationMinutes())
                .highQualityUrl(content.getHighQualityUrl())
                .mediumQualityUrl(content.getMediumQualityUrl())
                .lowQualityUrl(content.getLowQualityUrl())
                .previewUrl(content.getPreviewUrl())
                .thumbnailUrl(content.getThumbnailUrl())
                .targetMood(content.getTargetMood())
                .totalPlayCount(content.getTotalPlayCount())
                .rating(content.getRating())
                .isPremium(content.getIsPremium())
                .createdAt(content.getCreatedAt())
                .build();
    }
}