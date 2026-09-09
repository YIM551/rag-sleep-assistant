package com.sleepwell.sleepwell_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 건강 알림 데이터 DTO
 * 
 * 수면 무호흡, 호흡 장애 등 건강 관련 알림 데이터를 담는 DTO입니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthAlertDto {

    /**
     * 알림 유형 (SLEEP_APNEA, BREATHING_DISTURBANCE, HEART_RATE_ANOMALY 등)
     */
    private String alertType;

    /**
     * 심각도 (LOW, MEDIUM, HIGH, CRITICAL)
     */
    private String severity;

    /**
     * 알림 제목
     */
    private String title;

    /**
     * 알림 설명
     */
    private String description;

    /**
     * 알림 감지 시간
     */
    private LocalDateTime detectedAt;

    /**
     * 신뢰도 점수 (0.0 - 1.0)
     */
    private Double confidence;

    /**
     * 권장 조치사항
     */
    private String recommendedAction;

    /**
     * 의료진 상담 필요 여부
     */
    private Boolean requiresMedicalConsultation;

    /**
     * 알림 관련 상세 데이터
     */
    private Map<String, Object> alertDetails;

    /**
     * 관련 지표 값
     */
    private Map<String, Object> relatedMetrics;
} 