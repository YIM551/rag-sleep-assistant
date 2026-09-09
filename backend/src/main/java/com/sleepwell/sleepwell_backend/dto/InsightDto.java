package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "수면 분석 기반 생성된 인사이트 응답 DTO")
public class InsightDto {

    @Schema(description = "인사이트 코드", example = "SNORING_INCREASED_SIGNIFICANTLY")
    private final String code;

    @Schema(description = "인사이트 심각도 레벨 (INFO, WARNING, SEVERE)", example = "WARNING")
    private final Severity severity;

    @Schema(description = "사용자에게 보여질 인사이트 메시지", example = "지난 주에 비해 코골이가 25% 증가했습니다. 수면 자세를 변경해보는 것을 권장합니다.")
    private final String message;
    
    @Schema(description = "관련 오디오 이벤트 타입 목록", example = "[\"SNORING\"]")
    private final List<AudioEventType> relatedEventTypes;

    public enum Severity {
        INFO,       // 일반 정보
        WARNING,    // 주의 필요
        SEVERE      // 심각, 전문가 상담 권장
    }
} 