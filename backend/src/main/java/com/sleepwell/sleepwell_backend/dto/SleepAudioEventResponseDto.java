package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 수면 오디오 이벤트 응답 DTO
 * 
 * 저장된 수면 오디오 이벤트 메타데이터를 클라이언트에게 전달하기 위한 DTO입니다.
 * 분석 결과와 인사이트를 포함하여 사용자에게 유의미한 정보를 제공합니다.
 * 
 * 포함되는 정보:
 * - 기본 이벤트 정보 (유형, 시간, 강도 등)
 * - 분석 결과 (심각도, 의료적 중요도 등)
 * - 수면 품질에 미치는 영향
 * - 개선 제안 및 권장사항
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepAudioEvent
 * @see AudioEventType
 * @see WearableSource
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SleepAudioEventResponseDto {

    /**
     * 오디오 이벤트 ID
     */
    private Long id;

    /**
     * 수면 기록 ID
     */
    private Long sleepRecordId;

    /**
     * 오디오 이벤트 유형
     */
    private AudioEventType eventType;

    /**
     * 이벤트 유형 표시명
     */
    private String eventTypeDisplayName;

    /**
     * 이벤트 발생 일자
     */
    private LocalDateTime eventDate;

    /**
     * 이벤트 시작 시간
     */
    private LocalDateTime eventStartTime;

    /**
     * 이벤트 종료 시간
     */
    private LocalDateTime eventEndTime;

    /**
     * 이벤트 지속 시간 (초)
     */
    private Integer durationSeconds;

    /**
     * 이벤트 지속 시간 (분:초 형식)
     */
    private String durationFormatted;

    // === 강도 및 품질 메트릭 ===

    /**
     * 이벤트 강도 레벨 (1-10 척도)
     */
    private Integer intensityLevel;

    /**
     * 소음 레벨 (dB)
     */
    private BigDecimal decibelLevel;

    /**
     * 이벤트 신뢰도 점수 (0.0-1.0)
     */
    private BigDecimal confidenceScore;

    /**
     * 이벤트 품질 점수 (1-10)
     */
    private Integer qualityScore;

    // === 빈도 및 패턴 분석 ===

    /**
     * 시간당 발생 빈도
     */
    private BigDecimal frequencyPerHour;

    /**
     * 수면 단계와의 연관성
     */
    private String sleepStageContext;

    /**
     * 연속 발생 여부
     */
    private Boolean isContinuous;

    /**
     * 반복 패턴 여부
     */
    private Boolean hasPattern;

    // === 분석 결과 ===

    /**
     * 고강도 이벤트 여부
     */
    private Boolean isHighIntensity;

    /**
     * 의료진 상담 필요 여부
     */
    private Boolean requiresMedicalAttention;

    /**
     * 이벤트 심각도 레벨 (MILD, MODERATE, SEVERE, CRITICAL)
     */
    private String severityLevel;

    /**
     * 분당 평균 강도
     */
    private BigDecimal averageIntensityPerMinute;

    /**
     * 수면 품질에 미치는 영향 점수 (0-100)
     */
    private Double sleepQualityImpact;

    // === 이벤트별 특화 데이터 ===

    /**
     * 코골이 특화 데이터 (JSON 형태)
     */
    private String snoringMetadata;

    /**
     * 이갈이 특화 데이터 (JSON 형태)
     */
    private String bruxismMetadata;

    /**
     * 잠꼬대 특화 데이터 (JSON 형태)
     */
    private String sleepTalkMetadata;

    /**
     * 환경 소음 특화 데이터 (JSON 형태)
     */
    private String environmentalMetadata;

    // === 데이터 소스 및 메타데이터 ===

    /**
     * 데이터 소스
     */
    private WearableSource dataSource;

    /**
     * 데이터 소스 표시명
     */
    private String dataSourceDisplayName;

    /**
     * 플랫폼 고유 이벤트 ID
     */
    private String platformEventId;

    /**
     * 데이터 수집 시간
     */
    private LocalDateTime collectedAt;

    /**
     * 처리 상태
     */
    private String processingStatus;

    /**
     * 추가 메타데이터 (JSON 형태)
     */
    private String additionalMetadata;

    // === 권장사항 및 인사이트 ===

    /**
     * 개선 권장사항
     */
    private String recommendations;

    /**
     * 의료진 상담 메시지
     */
    private String medicalConsultationMessage;

    /**
     * 수면 환경 개선 제안
     */
    private String environmentalImprovements;

    /**
     * 생활습관 개선 제안
     */
    private String lifestyleRecommendations;

    // === 시스템 정보 ===

    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    private LocalDateTime updatedAt;

    // === 편의 메서드 ===

    /**
     * 이벤트 지속 시간을 분:초 형식으로 포맷팅
     */
    public String getFormattedDuration() {
        if (durationSeconds == null) return "00:00";
        
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 강도 레벨에 따른 색상 코드 반환 (UI용)
     */
    public String getIntensityColorCode() {
        if (intensityLevel == null) return "#CCCCCC";
        
        if (intensityLevel <= 3) return "#4CAF50"; // 녹색 (경미)
        if (intensityLevel <= 6) return "#FF9800"; // 주황 (보통)
        if (intensityLevel <= 8) return "#F44336"; // 빨강 (심각)
        return "#9C27B0"; // 보라 (위험)
    }

    /**
     * 신뢰도 점수를 퍼센트로 변환
     */
    public Integer getConfidencePercentage() {
        if (confidenceScore == null) return null;
        return confidenceScore.multiply(BigDecimal.valueOf(100)).intValue();
    }

    /**
     * 이벤트 요약 정보 생성
     */
    public String getEventSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(eventTypeDisplayName);
        
        if (intensityLevel != null) {
            summary.append(" (강도: ").append(intensityLevel).append("/10)");
        }
        
        if (durationSeconds != null) {
            summary.append(" - ").append(getFormattedDuration());
        }
        
        if (severityLevel != null) {
            summary.append(" [").append(severityLevel).append("]");
        }
        
        return summary.toString();
    }

    /**
     * 위험도 레벨 반환 (0-4: 안전, 낮음, 보통, 높음, 위험)
     */
    public Integer getRiskLevel() {
        if (intensityLevel == null) return 0;
        
        if (intensityLevel <= 2) return 0; // 안전
        if (intensityLevel <= 4) return 1; // 낮음
        if (intensityLevel <= 6) return 2; // 보통
        if (intensityLevel <= 8) return 3; // 높음
        return 4; // 위험
    }

    /**
     * 다음 권장 조치 반환
     */
    public String getNextRecommendedAction() {
        if (requiresMedicalAttention != null && requiresMedicalAttention) {
            return "의료진 상담을 권장합니다";
        }
        
        if (isHighIntensity != null && isHighIntensity) {
            return "수면 환경 개선을 고려해보세요";
        }
        
        if (hasPattern != null && hasPattern) {
            return "패턴 분석을 위해 지속적인 모니터링이 필요합니다";
        }
        
        return "현재 수준을 유지하세요";
    }
} 