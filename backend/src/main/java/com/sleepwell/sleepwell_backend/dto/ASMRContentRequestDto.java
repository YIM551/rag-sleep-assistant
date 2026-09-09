package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.ASMRCategory;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * ASMR 콘텐츠 생성/수정 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ASMRContentRequestDto {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    private String description;

    @NotNull(message = "카테고리는 필수입니다")
    private ASMRCategory category;

    @NotNull(message = "재생 시간(분)은 필수입니다")
    @Min(value = 1, message = "재생 시간은 1분 이상이어야 합니다")
    @Max(value = 480, message = "재생 시간은 8시간을 초과할 수 없습니다")
    private Integer durationMinutes;

    @NotNull(message = "재생 시간(초)은 필수입니다")
    @Min(value = 0, message = "초는 0 이상이어야 합니다")
    @Max(value = 59, message = "초는 59 이하여야 합니다")
    private Integer durationSeconds;

    @NotNull(message = "오디오 품질은 필수입니다")
    private Integer audioQuality;

    private String highQualityUrl;
    private String mediumQualityUrl;
    private String lowQualityUrl;
    private String previewUrl;
    private String thumbnailUrl;

    private String tags;
    private String targetMood;

    @Min(value = 1, message = "강도는 1 이상이어야 합니다")
    @Max(value = 10, message = "강도는 10을 초과할 수 없습니다")
    private Integer intensityLevel;

    private String creator;
    private Boolean isPremium;
}