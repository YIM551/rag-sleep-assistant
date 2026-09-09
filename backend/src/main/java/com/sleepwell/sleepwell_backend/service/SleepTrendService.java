package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.trend.SleepTrendResponseDto;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.SleepTrend;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.repository.SleepTrendRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SleepTrendService {

    private final SleepTrendRepository sleepTrendRepository;
    private final SleepRecordRepository sleepRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public SleepTrendResponseDto calculateAndSaveWeeklyTrend(Long userId, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 사용자를 찾을 수 없습니다: " + userId));

        LocalDate startDate = endDate.minusDays(6);
        List<SleepRecord> records = sleepRecordRepository.findByDateRange(user, startDate, endDate);

        if (records.isEmpty()) {
            throw new IllegalStateException("분석할 수면 기록 데이터가 없습니다.");
        }

        BigDecimal avgSleepMinutes = calculateAverage(records, record -> new BigDecimal(record.getTotalSleepMinutes()));
        BigDecimal avgSleepEfficiency = calculateAverage(records, record -> BigDecimal.valueOf(record.calculateSleepEfficiency()));
        BigDecimal avgSleepScore = calculateAverage(records, record -> new BigDecimal(record.getSleepQualityScore()));

        SleepTrend trend = SleepTrend.builder()
                .user(user)
                .trendDate(endDate)
                .trendType("WEEKLY")
                .periodDays(7)
                .dataPointCount(records.size())
                .averageSleepMinutes(avgSleepMinutes)
                .averageSleepEfficiency(avgSleepEfficiency)
                .averageSleepScore(avgSleepScore)
                .build();

        SleepTrend savedTrend = sleepTrendRepository.save(trend);
        return SleepTrendResponseDto.from(savedTrend);
    }
    
    public Optional<SleepTrendResponseDto> getLatestWeeklyTrend(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 사용자를 찾을 수 없습니다: " + userId));

        return sleepTrendRepository.findFirstByUserAndTrendTypeOrderByTrendDateDesc(user, "WEEKLY")
                .map(SleepTrendResponseDto::from);
    }

    private BigDecimal calculateAverage(List<SleepRecord> records, java.util.function.Function<SleepRecord, BigDecimal> mapper) {
        if (records == null || records.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = records.stream()
                .map(mapper)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = records.stream().map(mapper).filter(java.util.Objects::nonNull).count();
        
        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return sum.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
    }
} 