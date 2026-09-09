package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.SleepAudioTrendAnalysisDto;
import com.sleepwell.sleepwell_backend.dto.SleepAudioTrendAnalysisDto.*;
import com.sleepwell.sleepwell_backend.entity.SleepAudioEvent;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.repository.SleepAudioEventRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 수면 오디오 이벤트 트렌드 분석 서비스
 * 
 * Context7 베스트 프랙티스 적용:
 * - 간단하고 실용적인 집계 중심 접근
 * - 향후 Python 분석 시스템 연동을 위한 인터페이스 제공
 * - 단일 책임 원칙과 명확한 메서드 분리
 * 
 * @author SleepWell Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SleepAudioTrendAnalysisService {

    private final SleepAudioEventRepository sleepAudioEventRepository;
    private final UserRepository userRepository;

    /**
     * 수면 오디오 이벤트 트렌드 분석 수행
     * 
     * @param userId 분석 대상 사용자 ID
     * @param startDate 분석 시작 날짜
     * @param endDate 분석 종료 날짜
     * @return 트렌드 분석 결과
     */
    public SleepAudioTrendAnalysisDto analyzeTrend(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Starting audio trend analysis for user: {}, period: {} to {}", userId, startDate, endDate);
        
        // 입력 유효성 검증
        validateAnalysisRequest(userId, startDate, endDate);
        
        // 사용자 조회
        User user = findUserById(userId);
        
        // 분석 기간 내 오디오 이벤트 조회
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<SleepAudioEvent> audioEvents = sleepAudioEventRepository
            .findByUserIdAndEventDateBetween(userId, startDateTime, endDateTime);
        
        log.debug("Found {} audio events for analysis", audioEvents.size());
        
        // 기본 집계 수행
        BasicAggregation basicAggregation = performBasicAggregation(audioEvents, startDate, endDate);
        
        // 이벤트 타입별 집계
        List<EventTypeAggregation> eventTypeAggregations = performEventTypeAggregations(audioEvents);
        
        // 간단한 트렌드 지표 계산
        TrendIndicators trendIndicators = calculateTrendIndicators(audioEvents, startDate, endDate);
        
        // 외부 분석 시스템 연동을 위한 참조 데이터 준비
        ExternalAnalysisReference externalReference = prepareExternalAnalysisReference(
            userId, audioEvents, startDate, endDate
        );
        
        log.info("Audio trend analysis completed successfully for user: {}", userId);
        
        return SleepAudioTrendAnalysisDto.builder()
            .startDate(startDate)
            .endDate(endDate)
            .totalDays(calculateTotalDays(startDate, endDate))
            .basicAggregation(basicAggregation)
            .eventTypeAggregations(eventTypeAggregations)
            .trendIndicators(trendIndicators)
            .externalAnalysisReference(externalReference)
            .build();
    }

    /**
     * 분석 요청 유효성 검증
     * Context7 베스트 프랙티스: 명확한 검증 로직 분리
     */
    private void validateAnalysisRequest(Long userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("유효하지 않은 사용자 ID입니다.", HttpStatus.BAD_REQUEST, "INVALID_USER_ID");
        }
        
        if (startDate == null || endDate == null) {
            throw new BusinessException("시작 날짜와 종료 날짜는 필수입니다.", HttpStatus.BAD_REQUEST, "MISSING_DATE_RANGE");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("시작 날짜는 종료 날짜보다 이후일 수 없습니다.", HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
        }
        
        if (startDate.isAfter(LocalDate.now())) {
            throw new BusinessException("시작 날짜는 미래일 수 없습니다.", HttpStatus.BAD_REQUEST, "FUTURE_START_DATE");
        }
        
        // 최대 분석 기간 제한 (성능 고려)
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 365) {
            throw new BusinessException("분석 기간은 최대 1년까지 가능합니다.", HttpStatus.BAD_REQUEST, "PERIOD_TOO_LONG");
        }
    }

    /**
     * 사용자 조회
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(
                "사용자를 찾을 수 없습니다: " + userId, 
                HttpStatus.NOT_FOUND, 
                "USER_NOT_FOUND"
            ));
    }

    /**
     * 기본 집계 수행
     * Context7 베스트 프랙티스: 단일 책임 원칙
     */
    private BasicAggregation performBasicAggregation(List<SleepAudioEvent> events, 
                                                   LocalDate startDate, LocalDate endDate) {
        if (events.isEmpty()) {
            return createEmptyBasicAggregation();
        }

        int totalDays = calculateTotalDays(startDate, endDate);
        long totalEvents = events.size();
        
        // 일별 이벤트 수 계산
        Map<LocalDate, Long> eventsByDate = events.stream()
            .collect(Collectors.groupingBy(
                event -> event.getEventDate().toLocalDate(),
                Collectors.counting()
            ));
        
        OptionalInt maxEvents = eventsByDate.values().stream().mapToInt(Long::intValue).max();
        OptionalInt minEvents = eventsByDate.values().stream().mapToInt(Long::intValue).min();
        
        // 평균 지속시간과 강도 계산
        double avgDuration = events.stream()
            .filter(event -> event.getDurationSeconds() != null)
            .mapToInt(SleepAudioEvent::getDurationSeconds)
            .average()
            .orElse(0.0);
            
        double avgIntensity = events.stream()
            .filter(event -> event.getIntensityLevel() != null)
            .mapToInt(SleepAudioEvent::getIntensityLevel)
            .average()
            .orElse(0.0);
        
        return BasicAggregation.builder()
            .totalEvents(totalEvents)
            .totalDays(totalDays)
            .daysWithEvents((long) eventsByDate.size())
            .averageEventsPerDay(totalDays > 0 ? (double) totalEvents / totalDays : 0.0)
            .maxEventsInDay(maxEvents.orElse(0))
            .minEventsInDay(minEvents.orElse(0))
            .averageDurationSeconds(avgDuration)
            .averageIntensity(avgIntensity)
            .build();
    }

    /**
     * 빈 기본 집계 생성
     */
    private BasicAggregation createEmptyBasicAggregation() {
        return BasicAggregation.builder()
            .totalEvents(0L)
            .totalDays(0)
            .daysWithEvents(0L)
            .averageEventsPerDay(0.0)
            .maxEventsInDay(0)
            .minEventsInDay(0)
            .averageDurationSeconds(0.0)
            .averageIntensity(0.0)
            .build();
    }

    /**
     * 이벤트 타입별 집계 수행
     */
    private List<EventTypeAggregation> performEventTypeAggregations(List<SleepAudioEvent> events) {
        Map<AudioEventType, List<SleepAudioEvent>> eventsByType = events.stream()
            .collect(Collectors.groupingBy(SleepAudioEvent::getEventType));
        
        return eventsByType.entrySet().stream()
            .map(entry -> {
                AudioEventType eventType = entry.getKey();
                List<SleepAudioEvent> typeEvents = entry.getValue();
                
                double avgIntensity = typeEvents.stream()
                    .filter(event -> event.getIntensityLevel() != null)
                    .mapToInt(SleepAudioEvent::getIntensityLevel)
                    .average()
                    .orElse(0.0);
                
                double avgDuration = typeEvents.stream()
                    .filter(event -> event.getDurationSeconds() != null)
                    .mapToInt(SleepAudioEvent::getDurationSeconds)
                    .average()
                    .orElse(0.0);
                
                return EventTypeAggregation.builder()
                    .eventType(eventType)
                    .count((long) typeEvents.size())
                    .averageIntensity(avgIntensity)
                    .averageDurationSeconds(avgDuration)
                    .maxIntensity(typeEvents.stream()
                        .filter(event -> event.getIntensityLevel() != null)
                        .mapToInt(SleepAudioEvent::getIntensityLevel)
                        .max()
                        .orElse(0))
                    .build();
            })
            .sorted(Comparator.comparing(EventTypeAggregation::getCount).reversed())
            .collect(Collectors.toList());
    }

    /**
     * 간단한 트렌드 지표 계산
     * 복잡한 통계 분석 대신 기본적인 증감 추세만 계산
     */
    private TrendIndicators calculateTrendIndicators(List<SleepAudioEvent> events, 
                                                   LocalDate startDate, LocalDate endDate) {
        if (events.isEmpty()) {
            return createEmptyTrendIndicators();
        }
        
        // 기간을 반으로 나누어 전반부와 후반부 비교
        LocalDate midDate = startDate.plusDays(ChronoUnit.DAYS.between(startDate, endDate) / 2);
        
        List<SleepAudioEvent> firstHalfEvents = events.stream()
            .filter(e -> !e.getEventDate().toLocalDate().isAfter(midDate))
            .collect(Collectors.toList());
        
        List<SleepAudioEvent> secondHalfEvents = events.stream()
            .filter(e -> e.getEventDate().toLocalDate().isAfter(midDate))
            .collect(Collectors.toList());
            
        long firstHalfDays = ChronoUnit.DAYS.between(startDate, midDate) + 1;
        long secondHalfDays = ChronoUnit.DAYS.between(midDate.plusDays(1), endDate) + 1;

        double firstHalfAvg = firstHalfDays > 0 ? (double) firstHalfEvents.size() / firstHalfDays : 0.0;
        double secondHalfAvg = secondHalfDays > 0 ? (double) secondHalfEvents.size() / secondHalfDays : 0.0;
        
        String trendDirection = "STABLE";
        double changePercentage = 0.0;
        
        if (firstHalfAvg > 0) {
            changePercentage = ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100;
            if (changePercentage > 10) {
                trendDirection = "INCREASING";
            } else if (changePercentage < -10) {
                trendDirection = "DECREASING";
            }
        } else if (secondHalfAvg > 0) {
            trendDirection = "INCREASING";
            changePercentage = 100.0;
        }
        
        return TrendIndicators.builder()
            .overallTrendDirection(trendDirection)
            .changePercentage(changePercentage)
            .firstHalfAverage(firstHalfAvg)
            .secondHalfAverage(secondHalfAvg)
            .trendConfidence(calculateTrendConfidence(changePercentage))
            .build();
    }

    /**
     * 빈 트렌드 지표 생성
     */
    private TrendIndicators createEmptyTrendIndicators() {
        return TrendIndicators.builder()
            .overallTrendDirection("NO_DATA")
            .changePercentage(0.0)
            .firstHalfAverage(0.0)
            .secondHalfAverage(0.0)
            .trendConfidence("LOW")
            .build();
    }

    /**
     * 트렌드 신뢰도 계산
     */
    private String calculateTrendConfidence(double changePercentage) {
        double absChange = Math.abs(changePercentage);
        if (absChange >= 50) return "HIGH";
        if (absChange >= 20) return "MEDIUM";
        return "LOW";
    }

    /**
     * 외부 분석 시스템 연동을 위한 참조 데이터 준비
     * Context7 베스트 프랙티스: 미래 확장성 고려
     */
    private ExternalAnalysisReference prepareExternalAnalysisReference(Long userId, 
                                                                      List<SleepAudioEvent> events,
                                                                      LocalDate startDate, 
                                                                      LocalDate endDate) {
        return ExternalAnalysisReference.builder()
            .userId(userId)
            .dataSize(events.size())
            .analysisTimestamp(LocalDateTime.now())
            .pythonAnalysisEndpoint("/api/external/python-analysis")
            .mlModelVersion("v1.0")
            .dataHash(calculateDataHash(events))
            .exportFormat("JSON")
            .recommendedAnalysisType(determineRecommendedAnalysisType(events))
            .build();
    }

    /**
     * 데이터 해시값 계산 (간단한 구현)
     */
    private String calculateDataHash(List<SleepAudioEvent> events) {
        int hash = events.stream()
            .mapToInt(event -> Objects.hash(
                event.getId(),
                event.getEventType(),
                event.getEventDate()
            ))
            .sum();
        return "hash_" + Math.abs(hash);
    }

    /**
     * 권장 분석 타입 결정
     */
    private String determineRecommendedAnalysisType(List<SleepAudioEvent> events) {
        if (events.isEmpty()) {
            return "NO_ANALYSIS_NEEDED";
        }
        
        long snoringCount = events.stream()
            .filter(event -> event.getEventType() == AudioEventType.SNORING)
            .count();
        
        if (snoringCount > events.size() * 0.7) {
            return "SLEEP_APNEA_ANALYSIS";
        }
        
        if (events.size() > 100) {
            return "COMPREHENSIVE_ANALYSIS";
        }
        
        return "BASIC_ANALYSIS";
    }

    /**
     * 총 일수 계산
     */
    private int calculateTotalDays(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
} 