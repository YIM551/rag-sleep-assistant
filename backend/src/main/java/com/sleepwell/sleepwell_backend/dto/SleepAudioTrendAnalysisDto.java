package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 수면 오디오 이벤트 트렌드 분석 응답 DTO
 * 
 * Spring Boot 베스트 프랙티스를 따라 간단하고 실용적인 집계 데이터 제공
 * 향후 Python 분석 시스템과의 연동을 위한 기본 데이터 구조 포함
 * 
 * Context7 베스트 프랙티스 적용:
 * - 명확한 책임 분리
 * - 간단하고 이해하기 쉬운 구조
 * - 확장 가능한 설계
 */
@Getter
@Builder
public class SleepAudioTrendAnalysisDto {

    /**
     * 분석 기간 정보
     */
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Integer totalDays;

    /**
     * 기본 집계 데이터
     */
    private final BasicAggregation basicAggregation;

    /**
     * 이벤트 타입별 집계 (List로 변경하여 정렬 가능)
     */
    private final List<EventTypeAggregation> eventTypeAggregations;

    /**
     * 간단한 트렌드 지표
     */
    private final TrendIndicators trendIndicators;

    /**
     * 외부 분석 시스템 연동을 위한 원시 데이터 참조
     */
    private final ExternalAnalysisReference externalAnalysisReference;

    /**
     * 기본 집계 데이터
     * Context7 베스트 프랙티스: 중첩 클래스로 응집도 향상
     */
    @Getter
    @Builder
    public static class BasicAggregation {
        private final Long totalEvents;
        private final Integer totalDays;
        private final Long daysWithEvents;
        private final Double averageEventsPerDay;
        private final Integer maxEventsInDay;
        private final Integer minEventsInDay;
        private final Double averageDurationSeconds;
        private final Double averageIntensity;
    }

    /**
     * 이벤트 타입별 집계
     */
    @Getter
    @Builder
    public static class EventTypeAggregation {
        private final AudioEventType eventType;
        private final Long count;
        private final Double averageIntensity;
        private final Double averageDurationSeconds;
        private final Integer maxIntensity;
    }

    /**
     * 간단한 트렌드 지표
     * 복잡한 통계 분석 대신 기본적인 증감 추세만 제공
     */
    @Getter
    @Builder
    public static class TrendIndicators {
        private final String overallTrendDirection;
        private final Double changePercentage;
        private final Double firstHalfAverage;
        private final Double secondHalfAverage;
        private final String trendConfidence;
    }

    /**
     * 외부 분석 시스템 연동을 위한 메타데이터
     * 향후 Python/ML 시스템과의 연동 준비
     */
    @Getter
    @Builder
    public static class ExternalAnalysisReference {
        private final Long userId;
        private final Integer dataSize;
        private final LocalDateTime analysisTimestamp;
        private final String pythonAnalysisEndpoint;
        private final String mlModelVersion;
        private final String dataHash;
        private final String exportFormat;
        private final String recommendedAnalysisType;
    }
} 