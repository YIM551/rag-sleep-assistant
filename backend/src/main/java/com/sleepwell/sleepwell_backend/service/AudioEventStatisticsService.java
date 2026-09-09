package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.AudioEventStatisticsDto;
import com.sleepwell.sleepwell_backend.entity.SleepAudioEvent;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import com.sleepwell.sleepwell_backend.repository.SleepAudioEventRepository;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 오디오 이벤트 통계 서비스
 * 
 * 사용자의 수면 오디오 이벤트 데이터를 분석하여 상세한 통계 정보를 제공합니다.
 * JPA Criteria API와 네이티브 쿼리를 활용하여 효율적인 통계 계산을 수행합니다.
 * 
 * 주요 기능:
 * - 기간별 오디오 이벤트 통계 계산
 * - 이벤트 유형별 상세 분석
 * - 트렌드 분석 및 패턴 감지
 * - 의료적 권장사항 생성
 * - 수면 품질 영향 분석
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see AudioEventStatisticsDto
 * @see SleepAudioEvent
 * @see AudioEventType
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AudioEventStatisticsService {

    private final SleepAudioEventRepository audioEventRepository;
    private final SleepRecordRepository sleepRecordRepository;

    /**
     * 지정된 기간의 오디오 이벤트 통계 계산
     */
    public AudioEventStatisticsDto calculateStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Calculating audio event statistics for user: {} from {} to {}", userId, startDate, endDate);

        // 기본 데이터 조회
        List<SleepRecord> sleepRecords = getSleepRecordsInPeriod(userId, startDate, endDate);

        if (sleepRecords.isEmpty()) {
            return createEmptyStatistics(startDate, endDate);
        }

        List<SleepAudioEvent> audioEvents = getAudioEventsInPeriod(userId, startDate, endDate);

        // 통계 계산
        AudioEventStatisticsDto.AudioEventStatisticsDtoBuilder builder = AudioEventStatisticsDto.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalSleepRecords(sleepRecords.size())
                .totalAudioEvents((long) audioEvents.size());

        // 전체 통계 계산
        calculateOverallStatistics(builder, audioEvents, startDate, endDate);

        // 이벤트 유형별 통계 계산
        calculateEventTypeStatistics(builder, audioEvents);

        // 특화 통계 계산
        calculateSpecializedStatistics(builder, audioEvents, startDate, endDate);

        // 트렌드 분석
        calculateTrendAnalysis(builder, audioEvents, startDate, endDate);

        // 수면 품질 영향 분석
        calculateSleepQualityImpact(builder, audioEvents, sleepRecords);

        // 의료적 권장사항 생성
        generateMedicalRecommendations(builder, audioEvents);

        // 비교 분석 (이전 기간과 비교)
        calculateComparisonAnalysis(builder, userId, startDate, endDate, audioEvents);

        AudioEventStatisticsDto statistics = builder.build();
        log.info("Statistics calculation completed for user: {} - {} total events", userId, statistics.getTotalAudioEvents());

        return statistics;
    }

    /**
     * 기간별 수면 기록 조회
     */
    private List<SleepRecord> getSleepRecordsInPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        return sleepRecordRepository.findByUserIdAndRecordDateBetween(userId, startDate, endDate);
    }

    /**
     * 기간별 오디오 이벤트 조회
     */
    private List<SleepAudioEvent> getAudioEventsInPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        
        return audioEventRepository.findByUserIdAndEventDateBetween(userId, startDateTime, endDateTime);
    }

    /**
     * 빈 통계 객체 생성
     */
    private AudioEventStatisticsDto createEmptyStatistics(LocalDate startDate, LocalDate endDate) {
        return AudioEventStatisticsDto.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalSleepRecords(0)
                .totalAudioEvents(0L)
                .averageDailyEvents(BigDecimal.ZERO)
                .averageIntensity(BigDecimal.ZERO)
                .averageDurationSeconds(BigDecimal.ZERO)
                .averageConfidenceScore(BigDecimal.ZERO)
                .highIntensityEventRatio(BigDecimal.ZERO)
                .medicalAttentionRequiredRatio(BigDecimal.ZERO)
                .eventCountByType(new HashMap<>())
                .averageIntensityByType(new HashMap<>())
                .averageDurationByType(new HashMap<>())
                .eventRatioByType(new HashMap<>())
                .intensityDistribution(new HashMap<>())
                .hourlyDistribution(new HashMap<>())
                .dailyTrends(new ArrayList<>())
                .weeklyTrends(new ArrayList<>())
                .overallRiskLevel(0)
                .recommendMedicalConsultation(false)
                .recommendEnvironmentalImprovement(false)
                .recommendLifestyleChanges(false)
                .keyRecommendations(new ArrayList<>())
                .eventChangeRate(BigDecimal.ZERO)
                .intensityChangeRate(BigDecimal.ZERO)
                .isImproving(true)
                .build();
    }

    /**
     * 전체 통계 계산
     */
    private void calculateOverallStatistics(AudioEventStatisticsDto.AudioEventStatisticsDtoBuilder builder, 
                                          List<SleepAudioEvent> audioEvents, LocalDate startDate, LocalDate endDate) {
        if (audioEvents.isEmpty()) {
            builder.averageDailyEvents(BigDecimal.ZERO)
                   .averageIntensity(BigDecimal.ZERO)
                   .averageDurationSeconds(BigDecimal.ZERO)
                   .averageConfidenceScore(BigDecimal.ZERO)
                   .highIntensityEventRatio(BigDecimal.ZERO)
                   .medicalAttentionRequiredRatio(BigDecimal.ZERO);
            return;
        }

        long daysInPeriod = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (daysInPeriod > 0) {
            BigDecimal averageDailyEvents = BigDecimal.valueOf(audioEvents.size())
                    .divide(BigDecimal.valueOf(daysInPeriod), 2, RoundingMode.HALF_UP);
            builder.averageDailyEvents(averageDailyEvents);
        } else {
            builder.averageDailyEvents(BigDecimal.ZERO);
        }

        // 평균 강도 계산
        BigDecimal avgIntensity = audioEvents.stream()
                .filter(event -> event.getIntensityLevel() != null)
                .map(event -> BigDecimal.valueOf(event.getIntensityLevel()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(audioEvents.size()), 2, RoundingMode.HALF_UP);

        // 평균 지속시간 계산
        BigDecimal avgDuration = audioEvents.stream()
                .filter(event -> event.getDurationSeconds() != null)
                .map(event -> BigDecimal.valueOf(event.getDurationSeconds()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(audioEvents.size()), 2, RoundingMode.HALF_UP);

        // 평균 신뢰도 계산
        BigDecimal avgConfidence = audioEvents.stream()
                .filter(event -> event.getConfidenceScore() != null)
                .map(SleepAudioEvent::getConfidenceScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(audioEvents.size()), 3, RoundingMode.HALF_UP);

        // 고강도 이벤트 비율 계산 (강도 7+)
        long highIntensityCount = audioEvents.stream()
                .filter(event -> event.getIntensityLevel() != null && event.getIntensityLevel() >= 7)
                .count();
        BigDecimal highIntensityRatio = BigDecimal.valueOf(highIntensityCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(audioEvents.size()), 2, RoundingMode.HALF_UP);

        // 의료진 상담 필요 이벤트 비율 계산
        long medicalAttentionCount = audioEvents.stream()
                .filter(SleepAudioEvent::requiresMedicalAttention)
                .count();
        BigDecimal medicalAttentionRatio = BigDecimal.valueOf(medicalAttentionCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(audioEvents.size()), 2, RoundingMode.HALF_UP);

        builder.averageIntensity(avgIntensity)
               .averageDurationSeconds(avgDuration)
               .averageConfidenceScore(avgConfidence)
               .highIntensityEventRatio(highIntensityRatio)
               .medicalAttentionRequiredRatio(medicalAttentionRatio);
    }

    /**
     * 이벤트 유형별 통계 계산
     */
    private void calculateEventTypeStatistics(AudioEventStatisticsDto.AudioEventStatisticsDtoBuilder builder,
                                            List<SleepAudioEvent> audioEvents) {
        Map<AudioEventType, Long> eventCountByType = audioEvents.stream()
                .collect(Collectors.groupingBy(SleepAudioEvent::getEventType, Collectors.counting()));

        Map<AudioEventType, BigDecimal> averageIntensityByType = audioEvents.stream()
                .filter(event -> event.getIntensityLevel() != null)
                .collect(Collectors.groupingBy(
                        SleepAudioEvent::getEventType,
                        Collectors.averagingInt(SleepAudioEvent::getIntensityLevel)
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> BigDecimal.valueOf(entry.getValue()).setScale(2, RoundingMode.HALF_UP)
                ));

        Map<AudioEventType, BigDecimal> averageDurationByType = audioEvents.stream()
                .filter(event -> event.getDurationSeconds() != null)
                .collect(Collectors.groupingBy(
                        SleepAudioEvent::getEventType,
                        Collectors.averagingInt(SleepAudioEvent::getDurationSeconds)
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> BigDecimal.valueOf(entry.getValue()).setScale(2, RoundingMode.HALF_UP)
                ));

        // 이벤트 유형별 발생 비율 계산
        Map<AudioEventType, BigDecimal> eventRatioByType = eventCountByType.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> BigDecimal.valueOf(entry.getValue())
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(audioEvents.size()), 2, RoundingMode.HALF_UP)
                ));

        builder.eventCountByType(eventCountByType)
               .averageIntensityByType(averageIntensityByType)
               .averageDurationByType(averageDurationByType)
               .eventRatioByType(eventRatioByType);
    }

    /**
     * 특화 통계 계산 (코골이, 이갈이, 잠꼬대, 환경소음)
     */
    private void calculateSpecializedStatistics(AudioEventStatisticsDto.AudioEventStatisticsDtoBuilder builder,
                                              List<SleepAudioEvent> audioEvents, LocalDate startDate, LocalDate endDate) {
        // 코골이 통계
        calculateSnoringStatistics(builder, audioEvents, startDate, endDate);
        
        // 이갈이 통계
        calculateBruxismStatistics(builder, audioEvents, startDate, endDate);
        
        // 잠꼬대 통계
        calculateSleepTalkStatistics(builder, audioEvents, startDate, endDate);
        
        // 환경소음 통계
        calculateEnvironmentalNoiseStatistics(builder, audioEvents, startDate, endDate);
    }

    /**
     * 코골이 특화 통계 계산
     */
    private void calculateSnoringStatistics(AudioEventStatisticsDto.AudioEventStatisticsDtoBuilder builder,
                                          List<SleepAudioEvent> audioEvents, LocalDate startDate, LocalDate endDate) {
        List<SleepAudioEvent> snoringEvents = audioEvents.stream()
                .filter(event -> event.getEventType() == AudioEventType.SNORING)
                .collect(Collectors.toList());

        if (snoringEvents.isEmpty()) {
            builder.snoringDays(0)
                   .snoringAverageIntensity(BigDecimal.ZERO)
                   .snoringMaxIntensity(0)
                   .snoringAverageFrequency(BigDecimal.ZERO)
                   .severeSnoringRatio(BigDecimal.ZERO);
            return;
        }

        // 코골이 발생 일수 계산
        Set<LocalDate> snoringDates = snoringEvents.stream()
                .map(event -> event.getEventDate().toLocalDate())
                .collect(Collectors.toSet());

        // 평균 강도 계산
        BigDecimal avgIntensity = snoringEvents.stream()
                .filter(event -> event.getIntensityLevel() != null)
                .map(event -> BigDecimal.valueOf(event.getIntensityLevel()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(snoringEvents.size()), 2, RoundingMode.HALF_UP);

        // 최대 강도 계산
        Integer maxIntensity = snoringEvents.stream()
                .filter(event -> event.getIntensityLevel() != null)
                .mapToInt(SleepAudioEvent::getIntensityLevel)
                .max()
                .orElse(0);

        // 평균 빈도 계산 (시간당)
        BigDecimal avgFrequency = snoringEvents.stream()
                .filter(event -> event.getFrequencyPerHour() != null)
                .map(SleepAudioEvent::getFrequencyPerHour)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(snoringEvents.size()), 2, RoundingMode.HALF_UP);

        // 심한 코골이 비율 (강도 8+)
        long severeSnoringCount = snoringEvents.stream()
                .filter(event -> event.getIntensityLevel() != null && event.getIntensityLevel() >= 8)
                .count();
        BigDecimal severeSnoringRatio = BigDecimal.valueOf(severeSnoringCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(snoringEvents.size()), 2, RoundingMode.HALF_UP);

        builder.snoringDays(snoringDates.size())
               .snoringAverageIntensity(avgIntensity)
               .snoringMaxIntensity(maxIntensity)
               .snoringAverageFrequency(avgFrequency)
               .severeSnoringRatio(severeSnoringRatio);
    }

    /**
     * 이갈이 특화 통계 계산
     */
    private void calculateBruxismStatistics(AudioEventStatisticsDto.AudioEventStatisticsDtoBuilder builder,
                                          List<SleepAudioEvent> audioEvents, LocalDate startDate, LocalDate endDate) {
        List<SleepAudioEvent> bruxismEvents = audioEvents.stream()
                .filter(event -> event.getEventType() == AudioEventType.BRUXISM)
                .collect(Collectors.toList());

        if (bruxismEvents.isEmpty()) {
            builder.bruxismDays(0)
                   .bruxismAverageIntensity(BigDecimal.ZERO)
                   .bruxismMaxIntensity(0)
                   .bruxismAverageFrequency(BigDecimal.ZERO)
                   .severeBruxismRatio(BigDecimal.ZERO);
            return;
        }

        // 이갈이 발생 일수 계산
        Set<LocalDate> bruxismDates = bruxismEvents.stream()
                .map(event -> event.getEventDate().toLocalDate())
                .collect(Collectors.toSet());

        // 평균 강도 계산
        BigDecimal avgIntensity = bruxismEvents.stream()
                .filter(event -> event.getIntensityLevel() != null)
                .map(event -> BigDecimal.valueOf(event.getIntensityLevel()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(bruxismEvents.size()), 2, RoundingMode.HALF_UP);

        // 최대 강도 계산
        Integer maxIntensity = bruxismEvents.stream()
                .filter(event -> event.getIntensityLevel() != null)
                .mapToInt(SleepAudioEvent::getIntensityLevel)
                .max()
                .orElse(0);

        // 평균 빈도 계산 (시간당)
        BigDecimal avgFrequency = bruxismEvents.stream()
                .filter(event -> event.getFrequencyPerHour() != null)
                .map(SleepAudioEvent::getFrequencyPerHour)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(bruxismEvents.size()), 2, RoundingMode.HALF_UP);

        // 심한 이갈이 비율 (강도 7+)
        long severeBruxismCount = bruxismEvents.stream()
                .filter(event -> event.getIntensityLevel() != null && event.getIntensityLevel() >= 7)
                .count();
        BigDecimal severeBruxismRatio = BigDecimal.valueOf(severeBruxismCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(bruxismEvents.size()), 2, RoundingMode.HALF_UP);

        builder.bruxismDays(bruxismDates.size())
               .bruxismAverageIntensity(avgIntensity)
               .bruxismMaxIntensity(maxIntensity)
               .bruxismAverageFrequency(avgFrequency)
               .severeBruxismRatio(severeBruxismRatio);
    }

    /**
     * 잠꼬대 특화 통계 계산
     */
    private void calculateSleepTalkStatistics(AudioEventStatisticsDto.AudioEventStatisticsDtoBuilder builder,
                                            List<SleepAudioEvent> audioEvents, LocalDate startDate, LocalDate endDate) {
        List<SleepAudioEvent> sleepTalkEvents = audioEvents.stream()
                .filter(event -> event.getEventType() == AudioEventType.SLEEP_TALKING)
                .collect(Collectors.toList());

        if (sleepTalkEvents.isEmpty()) {
            builder.sleepTalkDays(0)
                   .sleepTalkAverageIntensity(BigDecimal.ZERO)
                   .sleepTalkAverageDuration(BigDecimal.ZERO)
                   .sleepTalkAverageFrequency(BigDecimal.ZERO);
            return;
        }

        // 잠꼬대 발생 일수 계산
        Set<LocalDate> sleepTalkDates = sleepTalkEvents.stream()
                .map(event -> event.getEventDate().toLocalDate())
                .collect(Collectors.toSet());

        // 평균 강도 계산
        BigDecimal avgIntensity = sleepTalkEvents.stream()
                .filter(event -> event.getIntensityLevel() != null)
                .map(event -> BigDecimal.valueOf(event.getIntensityLevel()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(sleepTalkEvents.size()), 2, RoundingMode.HALF_UP);

        // 평균 지속시간 계산
        BigDecimal avgDuration = sleepTalkEvents.stream()
                .filter(event -> event.getDurationSeconds() != null)
                .map(event -> BigDecimal.valueOf(event.getDurationSeconds()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(sleepTalkEvents.size()), 2, RoundingMode.HALF_UP);

        // 평균 빈도 계산 (시간당)
        BigDecimal avgFrequency = sleepTalkEvents.stream()
                .filter(event -> event.getFrequencyPerHour() != null)
                .map(SleepAudioEvent::getFrequencyPerHour)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(sleepTalkEvents.size()), 2, RoundingMode.HALF_UP);

        builder.sleepTalkDays(sleepTalkDates.size())
               .sleepTalkAverageIntensity(avgIntensity)
               .sleepTalkAverageDuration(avgDuration)
               .sleepTalkAverageFrequency(avgFrequency);
    }

    /**
     * 환경소음 특화 통계 계산
     */
    private void calculateEnvironmentalNoiseStatistics(AudioEventStatisticsDto.AudioEventStatisticsDtoBuilder builder,
                                                      List<SleepAudioEvent> audioEvents, LocalDate startDate, LocalDate endDate) {
        List<SleepAudioEvent> noiseEvents = audioEvents.stream()
                .filter(event -> event.getEventType() == AudioEventType.ENVIRONMENTAL_NOISE)
                .collect(Collectors.toList());

        if (noiseEvents.isEmpty()) {
            builder.environmentalNoiseDays(0)
                   .averageDecibelLevel(BigDecimal.ZERO)
                   .maxDecibelLevel(BigDecimal.ZERO)
                   .noiseThresholdExceededRatio(BigDecimal.ZERO);
            return;
        }

        // 환경소음 감지 일수 계산
        Set<LocalDate> noiseDates = noiseEvents.stream()
                .map(event -> event.getEventDate().toLocalDate())
                .collect(Collectors.toSet());

        // 평균 소음 레벨 계산
        BigDecimal avgDecibelLevel = noiseEvents.stream()
                .filter(event -> event.getDecibelLevel() != null)
                .map(SleepAudioEvent::getDecibelLevel)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(noiseEvents.size()), 2, RoundingMode.HALF_UP);

        // 최대 소음 레벨 계산
        BigDecimal maxDecibelLevel = noiseEvents.stream()
                .filter(event -> event.getDecibelLevel() != null)
                .map(SleepAudioEvent::getDecibelLevel)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // 소음 임계값 초과 비율 (70dB+)
        long thresholdExceededCount = noiseEvents.stream()
                .filter(event -> event.getDecibelLevel() != null && 
                        event.getDecibelLevel().compareTo(BigDecimal.valueOf(70)) >= 0)
                .count();
        BigDecimal thresholdExceededRatio = BigDecimal.valueOf(thresholdExceededCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(noiseEvents.size()), 2, RoundingMode.HALF_UP);

        builder.environmentalNoiseDays(noiseDates.size())
               .averageDecibelLevel(avgDecibelLevel)
               .maxDecibelLevel(maxDecibelLevel)
               .noiseThresholdExceededRatio(thresholdExceededRatio);
    }

    /**
     * 트렌드 분석 계산
     */
    private void calculateTrendAnalysis(AudioEventStatisticsDto.AudioEventStatisticsDtoBuilder builder,
                                      List<SleepAudioEvent> audioEvents, LocalDate startDate, LocalDate endDate) {
        // 일별 트렌드 계산
        List<AudioEventStatisticsDto.DailyEventTrend> dailyTrends = calculateDailyTrends(audioEvents, startDate, endDate);
        
        // 주별 트렌드 계산
        List<AudioEventStatisticsDto.WeeklyEventTrend> weeklyTrends = calculateWeeklyTrends(audioEvents, startDate, endDate);
        
        // 강도별 분포 계산
        Map<Integer, Long> intensityDistribution = audioEvents.stream()
                .filter(event -> event.getIntensityLevel() != null)
                .collect(Collectors.groupingBy(SleepAudioEvent::getIntensityLevel, Collectors.counting()));

        // 시간대별 분포 계산
        Map<Integer, Long> hourlyDistribution = audioEvents.stream()
                .filter(event -> event.getEventStartTime() != null)
                .collect(Collectors.groupingBy(
                        event -> event.getEventStartTime().getHour(),
                        Collectors.counting()
                ));

        builder.dailyTrends(dailyTrends)
               .weeklyTrends(weeklyTrends)
               .intensityDistribution(intensityDistribution)
               .hourlyDistribution(hourlyDistribution);
    }

    /**
     * 일별 트렌드 계산
     */
    private List<AudioEventStatisticsDto.DailyEventTrend> calculateDailyTrends(List<SleepAudioEvent> audioEvents, 
                                                                              LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<SleepAudioEvent>> eventsByDate = audioEvents.stream()
                .collect(Collectors.groupingBy(event -> event.getEventDate().toLocalDate()));

        List<AudioEventStatisticsDto.DailyEventTrend> dailyTrends = new ArrayList<>();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<SleepAudioEvent> dayEvents = eventsByDate.getOrDefault(date, Collections.emptyList());
            
            BigDecimal avgIntensity = dayEvents.isEmpty() ? BigDecimal.ZERO :
                    dayEvents.stream()
                            .filter(event -> event.getIntensityLevel() != null)
                            .map(event -> BigDecimal.valueOf(event.getIntensityLevel()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(dayEvents.size()), 2, RoundingMode.HALF_UP);

            Map<AudioEventType, Long> eventsByType = dayEvents.stream()
                    .collect(Collectors.groupingBy(SleepAudioEvent::getEventType, Collectors.counting()));

            dailyTrends.add(AudioEventStatisticsDto.DailyEventTrend.builder()
                    .date(date)
                    .eventCount((long) dayEvents.size())
                    .averageIntensity(avgIntensity)
                    .eventsByType(eventsByType)
                    .build());
        }
        
        return dailyTrends;
    }

    /**
     * 주별 트렌드 계산
     */
    private List<AudioEventStatisticsDto.WeeklyEventTrend> calculateWeeklyTrends(List<SleepAudioEvent> audioEvents,
                                                                                LocalDate startDate, LocalDate endDate) {
        List<AudioEventStatisticsDto.WeeklyEventTrend> weeklyTrends = new ArrayList<>();
        
        LocalDate weekStart = startDate;
        while (!weekStart.isAfter(endDate)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }
            
            final LocalDate finalWeekStart = weekStart;
            final LocalDate finalWeekEnd = weekEnd;
            
            List<SleepAudioEvent> weekEvents = audioEvents.stream()
                    .filter(event -> {
                        LocalDate eventDate = event.getEventDate().toLocalDate();
                        return !eventDate.isBefore(finalWeekStart) && !eventDate.isAfter(finalWeekEnd);
                    })
                    .collect(Collectors.toList());

            BigDecimal avgIntensity = weekEvents.isEmpty() ? BigDecimal.ZERO :
                    weekEvents.stream()
                            .filter(event -> event.getIntensityLevel() != null)
                            .map(event -> BigDecimal.valueOf(event.getIntensityLevel()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(weekEvents.size()), 2, RoundingMode.HALF_UP);

            Map<AudioEventType, Long> eventsByType = weekEvents.stream()
                    .collect(Collectors.groupingBy(SleepAudioEvent::getEventType, Collectors.counting()));

            weeklyTrends.add(AudioEventStatisticsDto.WeeklyEventTrend.builder()
                    .weekStart(weekStart)
                    .weekEnd(weekEnd)
                    .eventCount((long) weekEvents.size())
                    .averageIntensity(avgIntensity)
                    .eventsByType(eventsByType)
                    .build());
            
            weekStart = weekStart.plusDays(7);
        }
        
        return weeklyTrends;
    }

    /**
     * 수면 품질 영향 분석
     */
    private void calculateSleepQualityImpact(AudioEventStatisticsDto.AudioEventStatisticsDtoBuilder builder,
                                           List<SleepAudioEvent> audioEvents, List<SleepRecord> sleepRecords) {
        if (audioEvents.isEmpty() || sleepRecords.isEmpty()) {
            builder.averageSleepQualityImpact(BigDecimal.ZERO)
                   .sleepEfficiencyReduction(BigDecimal.ZERO)
                   .wakeupCorrelationScore(BigDecimal.ZERO);
            return;
        }

        // 평균 수면 품질 영향 점수 계산
        BigDecimal avgSleepQualityImpact = audioEvents.stream()
                .filter(SleepAudioEvent::isHighIntensity)
                .map(event -> BigDecimal.valueOf(75)) // 고강도 이벤트는 75점 영향
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(audioEvents.stream()
                        .filter(event -> !event.isHighIntensity())
                        .map(event -> BigDecimal.valueOf(25)) // 일반 이벤트는 25점 영향
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .divide(BigDecimal.valueOf(audioEvents.size()), 2, RoundingMode.HALF_UP);

        // 수면 효율성 저하 계산 (추정)
        long highIntensityEvents = audioEvents.stream()
                .filter(SleepAudioEvent::isHighIntensity)
                .count();
        BigDecimal sleepEfficiencyReduction = BigDecimal.valueOf(highIntensityEvents)
                .multiply(BigDecimal.valueOf(5)) // 고강도 이벤트당 5% 효율성 저하
                .min(BigDecimal.valueOf(50)); // 최대 50% 저하

        // 깨어남 상관관계 점수 (단순 추정)
        BigDecimal wakeupCorrelationScore = audioEvents.size() > 10 ? 
                BigDecimal.valueOf(0.6) : BigDecimal.valueOf(0.3);

        builder.averageSleepQualityImpact(avgSleepQualityImpact)
               .sleepEfficiencyReduction(sleepEfficiencyReduction)
               .wakeupCorrelationScore(wakeupCorrelationScore);
    }

    /**
     * 의료적 권장사항 생성
     */
    private void generateMedicalRecommendations(AudioEventStatisticsDto.AudioEventStatisticsDtoBuilder builder,
                                              List<SleepAudioEvent> audioEvents) {
        List<String> recommendations = new ArrayList<>();
        int riskLevel = 0;
        boolean needsMedicalConsultation = false;
        boolean needsEnvironmentalImprovement = false;
        boolean needsLifestyleChanges = false;
        AudioEventType priorityEventType = null;

        if (audioEvents.isEmpty()) {
            builder.overallRiskLevel(0)
                   .recommendMedicalConsultation(false)
                   .recommendEnvironmentalImprovement(false)
                   .recommendLifestyleChanges(false)
                   .keyRecommendations(recommendations)
                   .priorityEventType(null);
            return;
        }

        // 코골이 분석
        List<SleepAudioEvent> snoringEvents = audioEvents.stream()
                .filter(event -> event.getEventType() == AudioEventType.SNORING)
                .collect(Collectors.toList());
        
        if (!snoringEvents.isEmpty()) {
            long severeSnoringCount = snoringEvents.stream()
                    .filter(event -> event.getIntensityLevel() != null && event.getIntensityLevel() >= 8)
                    .count();
            
            if (severeSnoringCount > 0) {
                riskLevel = Math.max(riskLevel, 3);
                needsMedicalConsultation = true;
                // 가장 높은 강도 찾기
                int maxIntensity = snoringEvents.stream()
                        .filter(event -> event.getIntensityLevel() != null && event.getIntensityLevel() >= 8)
                        .mapToInt(SleepAudioEvent::getIntensityLevel)
                        .max()
                        .orElse(8);
                recommendations.add(String.format("높은 강도(%d)의 코골이가 감지되었습니다. 의료 상담을 권장합니다.", maxIntensity));
                if (priorityEventType == null) priorityEventType = AudioEventType.SNORING;
            } else if (snoringEvents.size() > 5) {
                riskLevel = Math.max(riskLevel, 2);
                needsLifestyleChanges = true;
                recommendations.add("지속적인 코골이가 관찰됩니다. 수면 자세 개선을 권장합니다.");
            }
        }

        // 이갈이 분석
        List<SleepAudioEvent> bruxismEvents = audioEvents.stream()
                .filter(event -> event.getEventType() == AudioEventType.BRUXISM)
                .collect(Collectors.toList());
        
        if (!bruxismEvents.isEmpty()) {
            long severeBruxismCount = bruxismEvents.stream()
                    .filter(event -> event.getIntensityLevel() != null && event.getIntensityLevel() >= 7)
                    .count();
            
            if (severeBruxismCount > 0) {
                riskLevel = Math.max(riskLevel, 2);
                needsMedicalConsultation = true;
                recommendations.add("심한 이갈이가 감지되었습니다. 치과 상담을 받아보세요.");
                if (priorityEventType == null) priorityEventType = AudioEventType.BRUXISM;
            }
            
            // 이갈이 빈발 체크 (10회 이상)
            if (bruxismEvents.size() >= 10) {
                riskLevel = Math.max(riskLevel, 2);
                needsMedicalConsultation = true;
                recommendations.add("이갈이 발생 빈도가 높습니다. 치과 방문을 고려해보세요.");
                if (priorityEventType == null) priorityEventType = AudioEventType.BRUXISM;
            }
        }

        // 환경 소음 분석
        List<SleepAudioEvent> noiseEvents = audioEvents.stream()
                .filter(event -> event.getEventType() == AudioEventType.ENVIRONMENTAL_NOISE)
                .collect(Collectors.toList());
        
        if (!noiseEvents.isEmpty()) {
            long loudNoiseCount = noiseEvents.stream()
                    .filter(event -> event.getDecibelLevel() != null && 
                            event.getDecibelLevel().compareTo(BigDecimal.valueOf(70)) >= 0)
                    .count();
            
            if (loudNoiseCount > 0) {
                riskLevel = Math.max(riskLevel, 1);
                needsEnvironmentalImprovement = true;
                recommendations.add("수면 환경의 소음 수준이 높습니다. 방음 개선을 고려해보세요.");
                if (priorityEventType == null) priorityEventType = AudioEventType.ENVIRONMENTAL_NOISE;
            }
        }

        // 전체 고강도 이벤트 비율 확인
        long highIntensityCount = audioEvents.stream()
                .filter(SleepAudioEvent::isHighIntensity)
                .count();
        
        if (highIntensityCount > audioEvents.size() * 0.3) { // 30% 이상이 고강도
            riskLevel = Math.max(riskLevel, 2);
            needsLifestyleChanges = true;
            recommendations.add("고강도 수면 방해 요소가 많습니다. 종합적인 수면 환경 개선이 필요합니다.");
        }

        // 일반적인 권장사항 추가
        if (audioEvents.size() > 20) {
            recommendations.add("수면 중 다양한 이벤트가 빈번하게 발생하고 있습니다. 수면 위생 관리를 강화하세요.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("현재 수면 상태가 양호합니다. 지속적인 모니터링을 권장합니다.");
        }

        builder.overallRiskLevel(riskLevel)
               .recommendMedicalConsultation(needsMedicalConsultation)
               .recommendEnvironmentalImprovement(needsEnvironmentalImprovement)
               .recommendLifestyleChanges(needsLifestyleChanges)
               .keyRecommendations(recommendations)
               .priorityEventType(priorityEventType);
    }

    /**
     * 비교 분석 (이전 기간과 비교)
     */
    private void calculateComparisonAnalysis(AudioEventStatisticsDto.AudioEventStatisticsDtoBuilder builder,
                                           Long userId, LocalDate startDate, LocalDate endDate,
                                           List<SleepAudioEvent> currentEvents) {
        long daysInPeriod = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (daysInPeriod <= 1) {
            // 비교 분석을 위한 데이터 부족 - 추가 데이터베이스 호출 없이 기본값 설정
            builder.eventChangeRate(BigDecimal.ZERO)
                    .intensityChangeRate(BigDecimal.ZERO)
                    .isImproving(true);
            return;
        }

        LocalDate previousStartDate = startDate.minusDays(daysInPeriod);
        LocalDate previousEndDate = startDate.minusDays(1);

        List<SleepAudioEvent> previousEvents = getAudioEventsInPeriod(userId, previousStartDate, previousEndDate);

        if (previousEvents.isEmpty()) {
            builder.eventChangeRate(BigDecimal.ZERO)
                    .intensityChangeRate(BigDecimal.ZERO)
                    .isImproving(true);
            return;
        }

        // 현재 기간 통계
        BigDecimal currentAvgIntensity = currentEvents.stream()
                .filter(event -> event.getIntensityLevel() != null)
                .map(event -> BigDecimal.valueOf(event.getIntensityLevel()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(currentEvents.size()), 2, RoundingMode.HALF_UP);

        // 이전 기간 통계
        BigDecimal previousAvgIntensity = previousEvents.stream()
                .filter(event -> event.getIntensityLevel() != null)
                .map(event -> BigDecimal.valueOf(event.getIntensityLevel()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(previousEvents.size()), 2, RoundingMode.HALF_UP);

        // 이벤트 수 변화율 계산
        BigDecimal eventChangeRate = BigDecimal.valueOf(currentEvents.size() - previousEvents.size())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previousEvents.size()), 2, RoundingMode.HALF_UP);

        // 평균 강도 변화율 계산
        BigDecimal intensityChangeRate = previousAvgIntensity.compareTo(BigDecimal.ZERO) == 0 ? 
                BigDecimal.ZERO :
                currentAvgIntensity.subtract(previousAvgIntensity)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(previousAvgIntensity, 2, RoundingMode.HALF_UP);

        // 개선 여부 판단 (이벤트 수 감소 또는 강도 감소)
        boolean isImproving = eventChangeRate.compareTo(BigDecimal.ZERO) <= 0 ||
                             intensityChangeRate.compareTo(BigDecimal.ZERO) <= 0;

        builder.eventChangeRate(eventChangeRate)
               .intensityChangeRate(intensityChangeRate)
               .isImproving(isImproving);
    }

    /**
     * 지정된 기간, 형식, 이벤트 유형에 따라 통계 데이터를 내보냅니다.
     * @param userId 사용자 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @param format 내보내기 형식 (예: "csv")
     * @param events 필터링할 오디오 이벤트 유형 목록
     * @return 데이터 바이트 배열
     */
    public byte[] exportStatistics(Long userId, LocalDate startDate, LocalDate endDate, String format, List<AudioEventType> events) {
        // Basic implementation for CSV
        if ("csv".equalsIgnoreCase(format)) {
            // In a real implementation, you would query data based on all parameters.
            String header = "userId,startDate,endDate,format,events,data\n";
            String eventsString = events != null ? events.toString() : "all";
            String data = String.format("%d,%s,%s,%s,%s,sample_data\n", userId, startDate.toString(), endDate.toString(), format, eventsString);
            return (header + data).getBytes(StandardCharsets.UTF_8);
        }
        return new byte[0];
    }
} 