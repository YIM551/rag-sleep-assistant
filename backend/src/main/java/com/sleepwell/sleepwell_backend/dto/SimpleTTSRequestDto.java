package com.sleepwell.sleepwell_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 간단한 TTS 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTTSRequestDto {
    
    @NotBlank(message = "텍스트는 필수입니다")
    private String text;
    
    @Builder.Default
    private String voice = "alloy";
    
    @Builder.Default
    private String language = "ko";
}