package com.sleepwell.sleepwell_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SleepPeriodParser {

    private static final Pattern FULL_DATE_PATTERN = Pattern.compile("(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})");
    private static final Pattern SHORT_DATE_PATTERN = Pattern.compile("(\\d{1,2})[./-](\\d{1,2})");
    private static final Pattern KOREAN_DATE_PATTERN = Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");
    private static final Pattern FULL_RANGE_PATTERN = Pattern.compile(
            "(\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\s*[~]\\s*(\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})");
    private static final Pattern SHORT_RANGE_PATTERN = Pattern.compile(
            "(\\d{1,2}[./-]\\d{1,2})\\s*[~]\\s*(\\d{1,2}[./-]\\d{1,2})");
    private static final Pattern KOREAN_RANGE_PATTERN = Pattern.compile(
            "(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일\\s*[~]\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");

    public PeriodRoutingResult parse(String message) {
        if (message == null || message.isBlank()) {
            return PeriodRoutingResult.defaultMonthlyWithRecent();
        }

        String normalized = message.toLowerCase(Locale.KOREAN);

        PeriodRoutingResult explicitRange = parseExplicitRange(message);
        if (explicitRange != null) {
            return explicitRange;
        }

        LocalDate explicitDate = extractExplicitDate(message);
        if (explicitDate != null) {
            return PeriodRoutingResult.forRange(explicitDate, explicitDate);
        }

        if (normalized.contains("어제") || normalized.contains("전날") || normalized.contains("어젯밤")) {
            return PeriodRoutingResult.forRange(LocalDate.now().minusDays(1), LocalDate.now());
        }

        if (normalized.contains("최근")) {
            Integer days = extractRecentDays(normalized);
            if (days != null) {
                return PeriodRoutingResult.forRecentDays(days);
            }
        }

        if (normalized.contains("일주일") || normalized.contains("한주") || normalized.contains("1주")) {
            return PeriodRoutingResult.forRecentDays(7);
        }

        if (normalized.contains("2주") || normalized.contains("이주")) {
            return PeriodRoutingResult.forRecentDays(14);
        }

        if (normalized.contains("한달") || normalized.contains("1개월")) {
            return PeriodRoutingResult.forRecentDays(30);
        }

        if (normalized.contains("3개월")) {
            return PeriodRoutingResult.forRecentDays(90);
        }

        DayOfWeek dayOfWeek = detectDayOfWeek(normalized);
        if (dayOfWeek != null) {
            return PeriodRoutingResult.forWeekday(dayOfWeek, 8);
        }

        LocalDate specificDate = extractSpecificDate(normalized);
        if (specificDate != null) {
            return PeriodRoutingResult.forRange(specificDate, specificDate);
        }

        return PeriodRoutingResult.defaultMonthlyWithRecent();
    }

    private Integer extractRecentDays(String normalized) {
        if (normalized.contains("최근 7일") || normalized.contains("최근7일")) {
            return 7;
        }
        if (normalized.contains("최근 3일") || normalized.contains("최근3일")) {
            return 3;
        }
        if (normalized.contains("최근 14일") || normalized.contains("최근14일")) {
            return 14;
        }
        if (normalized.contains("최근 30일") || normalized.contains("최근30일")) {
            return 30;
        }
        if (normalized.contains("최근 한달")) {
            return 30;
        }
        return null;
    }

    private DayOfWeek detectDayOfWeek(String normalized) {
        if (normalized.contains("월요일") || normalized.contains("월요일마다") || normalized.contains("월요일에")) {
            return DayOfWeek.MONDAY;
        }
        if (normalized.contains("화요일") || normalized.contains("화요일마다") || normalized.contains("화요일에")) {
            return DayOfWeek.TUESDAY;
        }
        if (normalized.contains("수요일") || normalized.contains("수요일마다") || normalized.contains("수요일에")) {
            return DayOfWeek.WEDNESDAY;
        }
        if (normalized.contains("목요일") || normalized.contains("목요일마다") || normalized.contains("목요일에")) {
            return DayOfWeek.THURSDAY;
        }
        if (normalized.contains("금요일") || normalized.contains("금요일마다") || normalized.contains("금요일에")) {
            return DayOfWeek.FRIDAY;
        }
        if (normalized.contains("토요일") || normalized.contains("토요일마다") || normalized.contains("토요일에")) {
            return DayOfWeek.SATURDAY;
        }
        if (normalized.contains("일요일") || normalized.contains("일요일마다") || normalized.contains("일요일에")) {
            return DayOfWeek.SUNDAY;
        }
        return null;
    }

    private PeriodRoutingResult parseExplicitRange(String message) {
        LocalDate now = LocalDate.now();

        Matcher fullRangeMatcher = FULL_RANGE_PATTERN.matcher(message);
        if (fullRangeMatcher.find()) {
            LocalDate start = parseFullDate(fullRangeMatcher.group(1));
            LocalDate end = parseFullDate(fullRangeMatcher.group(2));
            return buildRangeResult(start, end);
        }

        Matcher shortRangeMatcher = SHORT_RANGE_PATTERN.matcher(message);
        if (shortRangeMatcher.find()) {
            LocalDate start = parseShortDate(shortRangeMatcher.group(1), now.getYear());
            LocalDate end = parseShortDate(shortRangeMatcher.group(2), now.getYear());
            return buildRangeResult(start, end);
        }

        Matcher koreanRangeMatcher = KOREAN_RANGE_PATTERN.matcher(message);
        if (koreanRangeMatcher.find()) {
            LocalDate start = parseKoreanDate(koreanRangeMatcher.group(1), koreanRangeMatcher.group(2), now.getYear());
            LocalDate end = parseKoreanDate(koreanRangeMatcher.group(3), koreanRangeMatcher.group(4), now.getYear());
            return buildRangeResult(start, end);
        }

        return null;
    }

    private PeriodRoutingResult buildRangeResult(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return null;
        }
        if (start.isAfter(end)) {
            log.debug("Invalid date range detected (start after end): {} - {}", start, end);
            return PeriodRoutingResult.defaultMonthlyWithRecent();
        }
        return PeriodRoutingResult.forRange(start, end);
    }

    private LocalDate extractExplicitDate(String message) {
        Matcher fullMatcher = FULL_DATE_PATTERN.matcher(message);
        if (fullMatcher.find()) {
            return parseFullDate(fullMatcher.group(0));
        }

        Matcher koreanMatcher = KOREAN_DATE_PATTERN.matcher(message);
        if (koreanMatcher.find()) {
            return parseKoreanDate(koreanMatcher.group(1), koreanMatcher.group(2), LocalDate.now().getYear());
        }

        Matcher shortMatcher = SHORT_DATE_PATTERN.matcher(message);
        if (shortMatcher.find()) {
            return parseShortDate(shortMatcher.group(0), LocalDate.now().getYear());
        }

        return null;
    }

    private LocalDate parseFullDate(String rawDate) {
        Matcher matcher = FULL_DATE_PATTERN.matcher(rawDate);
        if (!matcher.find()) {
            return null;
        }
        try {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            return LocalDate.of(year, month, day);
        } catch (DateTimeException | NumberFormatException e) {
            log.debug("Invalid full date format detected: {}", rawDate);
            return null;
        }
    }

    private LocalDate parseShortDate(String rawDate, int year) {
        Matcher matcher = SHORT_DATE_PATTERN.matcher(rawDate);
        if (!matcher.find()) {
            return null;
        }
        try {
            int month = Integer.parseInt(matcher.group(1));
            int day = Integer.parseInt(matcher.group(2));
            return LocalDate.of(year, month, day);
        } catch (DateTimeException | NumberFormatException e) {
            log.debug("Invalid short date format detected: {}", rawDate);
            return null;
        }
    }

    private LocalDate parseKoreanDate(String monthValue, String dayValue, int year) {
        try {
            int month = Integer.parseInt(monthValue);
            int day = Integer.parseInt(dayValue);
            return LocalDate.of(year, month, day);
        } catch (DateTimeException | NumberFormatException e) {
            log.debug("Invalid Korean date format detected: {}월 {}일", monthValue, dayValue);
            return null;
        }
    }

    private LocalDate extractSpecificDate(String normalized) {
        if (normalized.contains("이번주")) {
            return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        if (normalized.contains("지난주")) {
            return LocalDate.now().minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        return null;
    }
}
