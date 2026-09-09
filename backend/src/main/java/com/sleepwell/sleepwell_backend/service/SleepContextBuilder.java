package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.RagEnhancedConsultationResponseDto;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.SleepDataFetchStatus;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

@Slf4j
@Component
@RequiredArgsConstructor
public class SleepContextBuilder {

    private final SleepRecordRepository sleepRecordRepository;

    public SleepContextResult buildSleepContext(User user, int days) {
        PeriodRoutingResult routing = PeriodRoutingResult.forRecentDays(days);
        return buildSleepContext(user, routing);
    }

    public SleepContextResult buildSleepContext(User user, PeriodRoutingResult routing) {
        LocalDate endDate = routing.endDate() != null ? routing.endDate() : LocalDate.now();
        LocalDate startDate = routing.startDate() != null ? routing.startDate() : endDate.minusDays(30);

        FetchResult monthlyFetch = fetchRecords(user, endDate.minusDays(30), endDate);
        FetchResult targetFetch = fetchRecords(user, startDate, endDate);

        List<SleepRecord> monthlyRecords = monthlyFetch.records();
        List<SleepRecord> targetRecords = targetFetch.records();

        if (routing.dayOfWeek() != null) {
            targetRecords = targetRecords.stream()
                    .filter(r -> r.getRecordDate() != null && r.getRecordDate().getDayOfWeek() == routing.dayOfWeek())
                    .toList();
        }

        RagEnhancedConsultationResponseDto.PersonalSleepSummary monthlySummary =
                buildSummaryFromRecords(monthlyRecords, 30, endDate.minusDays(30), endDate);

        RagEnhancedConsultationResponseDto.PersonalSleepSummary targetSummary =
                buildSummaryFromRecords(targetRecords, calculateDays(startDate, endDate), startDate, endDate);

        SleepRecord latestRecord = sleepRecordRepository.findLatestByUser(user).orElse(null);

        SleepDataFetchStatus status = targetFetch.status();
        if (status == SleepDataFetchStatus.SUCCESS && targetRecords.isEmpty()) {
            status = monthlyRecords.isEmpty() ? SleepDataFetchStatus.EMPTY : SleepDataFetchStatus.SUCCESS;
        }

        String summarySection = buildCompositeSummarySection(monthlySummary, latestRecord, targetSummary, routing);

        return new SleepContextResult(status, targetSummary, summarySection, routing.recentDays() != null ? routing.recentDays() : 30);
    }

    private FetchResult fetchRecords(User user, LocalDate startDate, LocalDate endDate) {
        try {
            return new FetchResult(SleepDataFetchStatus.SUCCESS,
                    sleepRecordRepository.findByDateRange(user, startDate, endDate));
        } catch (BusinessException e) {
            SleepDataFetchStatus status = e.getStatus() == HttpStatus.FORBIDDEN
                    ? SleepDataFetchStatus.ACCESS_DENIED
                    : SleepDataFetchStatus.ERROR;
            log.warn("[수면데이터] 조회 실패 - userId={}, status={}, error={}",
                    user.getId(),
                    status,
                    e.getMessage());
            return new FetchResult(status, List.of());
        } catch (Exception e) {
            log.error("[수면데이터] 조회 실패 - userId={}, error={}", user.getId(), e.getMessage(), e);
            return new FetchResult(SleepDataFetchStatus.ERROR, List.of());
        }
    }

    private RagEnhancedConsultationResponseDto.PersonalSleepSummary buildSummaryFromRecords(
            List<SleepRecord> records,
            int days,
            LocalDate startDate,
            LocalDate endDate) {
        if (records == null || records.isEmpty()) {
            return null;
        }

        List<SleepRecord> validRecords = records.stream()
                .filter(r -> r.getTotalSleepMinutes() != null)
                .filter(r -> r.getTotalSleepMinutes() > 0 && r.getTotalSleepMinutes() <= 1440)
                .toList();

        if (validRecords.isEmpty()) {
            return null;
        }

        double avgDurationHours = validRecords.stream()
                .mapToDouble(r -> r.getTotalSleepMinutes() / 60.0)
                .average()
                .orElse(0.0);

        OptionalDouble efficiencyAverage = validRecords.stream()
                .map(this::calculateEfficiency)
                .filter(eff -> eff != null && eff > 0 && eff <= 100)
                .mapToDouble(Double::doubleValue)
                .average();

        OptionalDouble wakeupAverage = validRecords.stream()
                .filter(r -> r.getWakeupCount() != null)
                .mapToDouble(SleepRecord::getWakeupCount)
                .average();

        OptionalDouble bedtimeAverage = averageTimeMinutes(validRecords.stream()
                .map(SleepRecord::getSleepStartTime)
                .filter(t -> t != null)
                .map(t -> t.toLocalTime())
                .toList());

        OptionalDouble wakeTimeAverage = averageTimeMinutes(validRecords.stream()
                .map(SleepRecord::getSleepEndTime)
                .filter(t -> t != null)
                .map(t -> t.toLocalTime())
                .toList());

        String keyPattern = identifyKeyPattern(avgDurationHours,
                efficiencyAverage.orElse(0.0),
                wakeupAverage.orElse(0.0));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("startDate", startDate.toString());
        metadata.put("endDate", endDate.toString());
        if (validRecords.size() < 3) {
            metadata.put("warning", "수면 데이터가 부족하여 통계의 신뢰도가 낮을 수 있습니다");
        }

        return RagEnhancedConsultationResponseDto.PersonalSleepSummary.builder()
                .periodDays(days)
                .avgSleepDuration(round1(avgDurationHours))
                .avgSleepEfficiency(efficiencyAverage.isPresent() ? round1(efficiencyAverage.getAsDouble()) : null)
                .avgWakeupCount(wakeupAverage.isPresent() ? round1(wakeupAverage.getAsDouble()) : null)
                .avgSleepLatencyMinutes(null)
                .avgBedTime(bedtimeAverage.isPresent() ? formatTime(bedtimeAverage.getAsDouble()) : null)
                .avgWakeTime(wakeTimeAverage.isPresent() ? formatTime(wakeTimeAverage.getAsDouble()) : null)
                .totalRecords(validRecords.size())
                .keyPattern(keyPattern)
                .metadata(metadata)
                .build();
    }

    private String buildCompositeSummarySection(
            RagEnhancedConsultationResponseDto.PersonalSleepSummary monthlySummary,
            SleepRecord latestRecord,
            RagEnhancedConsultationResponseDto.PersonalSleepSummary targetSummary,
            PeriodRoutingResult routing) {
        StringBuilder section = new StringBuilder();
        if (monthlySummary != null) {
            section.append("### 기본 수면 요약 (최근 30일)\n");
            section.append("- 평균 총 수면시간: ")
                    .append(valueOrNone(monthlySummary.getAvgSleepDuration(), "시간")).append("\n");
            section.append("- 수면 효율: ")
                    .append(valueOrNone(monthlySummary.getAvgSleepEfficiency(), "%")).append("\n");
            section.append("- 중간 각성 횟수: ")
                    .append(valueOrNone(monthlySummary.getAvgWakeupCount(), "회")).append("\n");
        }

        if (latestRecord != null) {
            section.append("\n### 최근 1회 수면\n");
            section.append("- 기록일: ").append(latestRecord.getRecordDate()).append("\n");
            section.append("- 총 수면 시간: ").append(latestRecord.getTotalSleepMinutes()).append("분\n");
            section.append("- 수면 점수: ").append(latestRecord.getSleepQualityScore()).append("/100\n");
            section.append("- 깨어난 횟수: ").append(latestRecord.getWakeupCount()).append("회\n");
        }

        if (targetSummary != null) {
            section.append("\n### 요청 기반 분석\n");
            if (routing.dayOfWeek() != null) {
                section.append("- 필터: ").append(routing.dayOfWeek()).append(" 기준\n");
            } else if (routing.recentDays() != null) {
                section.append("- 기간: 최근 ").append(routing.recentDays()).append("일\n");
            } else {
                section.append("- 기간: ").append(routing.startDate()).append(" ~ ").append(routing.endDate()).append("\n");
            }
            section.append("- 평균 총 수면시간: ")
                    .append(valueOrNone(targetSummary.getAvgSleepDuration(), "시간")).append("\n");
            section.append("- 수면 효율: ")
                    .append(valueOrNone(targetSummary.getAvgSleepEfficiency(), "%")).append("\n");
            section.append("- 중간 각성 횟수: ")
                    .append(valueOrNone(targetSummary.getAvgWakeupCount(), "회")).append("\n");
            section.append("- 주요 패턴: ").append(targetSummary.getKeyPattern()).append("\n");
        }

        return section.toString().trim();
    }

    private int calculateDays(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.DAYS.between(start, end);
    }

    private record FetchResult(SleepDataFetchStatus status, List<SleepRecord> records) {}

    private Double calculateEfficiency(SleepRecord record) {
        if (record.getSleepInBedMinutes() != null && record.getSleepInBedMinutes() > 0) {
            return (double) record.getTotalSleepMinutes() / record.getSleepInBedMinutes() * 100;
        }

        if (record.getSleepAwakeMinutes() != null && record.getSleepAwakeMinutes() >= 0) {
            int timeInBed = record.getTotalSleepMinutes() + record.getSleepAwakeMinutes();
            if (timeInBed > 0) {
                return (double) record.getTotalSleepMinutes() / timeInBed * 100;
            }
        }

        if (record.getSleepStartTime() != null && record.getSleepEndTime() != null) {
            long bedTimeMinutes = java.time.Duration.between(
                    record.getSleepStartTime(),
                    record.getSleepEndTime()
            ).toMinutes();
            if (bedTimeMinutes > 0) {
                return (double) record.getTotalSleepMinutes() / bedTimeMinutes * 100;
            }
        }

        Integer stageSumMinutes = record.calculateTotalSleepStageMinutes();
        if (stageSumMinutes != null && stageSumMinutes > 0) {
            return (double) stageSumMinutes / record.getTotalSleepMinutes() * 100;
        }

        return null;
    }

    private OptionalDouble averageTimeMinutes(List<LocalTime> times) {
        if (times == null || times.isEmpty()) {
            return OptionalDouble.empty();
        }

        double sumSin = 0.0;
        double sumCos = 0.0;
        for (LocalTime time : times) {
            double minutes = time.getHour() * 60 + time.getMinute();
            double angle = (minutes / 1440.0) * (2 * Math.PI);
            sumSin += Math.sin(angle);
            sumCos += Math.cos(angle);
        }

        double averageAngle = Math.atan2(sumSin / times.size(), sumCos / times.size());
        if (averageAngle < 0) {
            averageAngle += 2 * Math.PI;
        }

        double averageMinutes = averageAngle / (2 * Math.PI) * 1440.0;
        return OptionalDouble.of(averageMinutes);
    }

    private String buildSummarySection(RagEnhancedConsultationResponseDto.PersonalSleepSummary summary) {
        StringBuilder section = new StringBuilder();
        section.append("### 당신의 수면 상태 요약\n");
        section.append("- 기간: 최근 ").append(summary.getPeriodDays()).append("일\n");
        section.append("- 평균 총 수면시간: ")
                .append(valueOrNone(summary.getAvgSleepDuration(), "시간")).append("\n");
        section.append("- 수면 효율: ")
                .append(valueOrNone(summary.getAvgSleepEfficiency(), "%")).append("\n");
        section.append("- 잠들기까지 시간: ")
                .append(valueOrNone(summary.getAvgSleepLatencyMinutes(), "분")).append("\n");
        section.append("- 중간 각성 횟수: ")
                .append(valueOrNone(summary.getAvgWakeupCount(), "회")).append("\n");
        section.append("- 취침/기상 시간 패턴: ")
                .append(formatBedWake(summary.getAvgBedTime(), summary.getAvgWakeTime())).append("\n");
        return section.toString();
    }

    private String valueOrNone(Double value, String unit) {
        if (value == null) {
            return "데이터 없음";
        }
        return value + unit;
    }

    private String formatBedWake(String bedTime, String wakeTime) {
        if (bedTime == null && wakeTime == null) {
            return "데이터 없음";
        }
        if (bedTime != null && wakeTime != null) {
            return "평균 취침 " + bedTime + ", 평균 기상 " + wakeTime;
        }
        if (bedTime != null) {
            return "평균 취침 " + bedTime + ", 평균 기상 데이터 없음";
        }
        return "평균 취침 데이터 없음, 평균 기상 " + wakeTime;
    }

    private String formatTime(double minutes) {
        int rounded = (int) Math.round(minutes);
        int hour = (rounded / 60) % 24;
        int minute = rounded % 60;
        return String.format("%02d:%02d", hour, minute);
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private String identifyKeyPattern(double avgDuration, double avgEfficiency, double avgWakeupCount) {
        List<String> patterns = new java.util.ArrayList<>();

        if (avgDuration < 6.0) {
            patterns.add("수면 부족");
        } else if (avgDuration > 9.0) {
            patterns.add("과다 수면");
        }

        if (avgEfficiency > 0 && avgEfficiency < 75.0) {
            patterns.add("낮은 수면 효율");
        }

        if (avgWakeupCount > 3.0) {
            patterns.add("잦은 야간 각성");
        }

        return patterns.isEmpty() ? "정상 범위" : String.join(", ", patterns);
    }

    public record SleepContextResult(
            SleepDataFetchStatus status,
            RagEnhancedConsultationResponseDto.PersonalSleepSummary summary,
            String summarySection,
            int periodDays
    ) {
        public static SleepContextResult empty(int days) {
            return new SleepContextResult(SleepDataFetchStatus.EMPTY, null, null, days);
        }

        public static SleepContextResult error(SleepDataFetchStatus status, int days) {
            return new SleepContextResult(status, null, null, days);
        }
    }
}
