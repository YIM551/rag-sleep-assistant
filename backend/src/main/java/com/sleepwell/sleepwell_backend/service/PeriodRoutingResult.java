package com.sleepwell.sleepwell_backend.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

public record PeriodRoutingResult(
        LocalDate startDate,
        LocalDate endDate,
        Integer recentDays,
        DayOfWeek dayOfWeek,
        Integer weekLookback,
        boolean includeMonthlySummary,
        boolean includeRecentSummary
) {
    public static PeriodRoutingResult forRange(LocalDate start, LocalDate end) {
        return new PeriodRoutingResult(start, end, null, null, null, false, true);
    }

    public static PeriodRoutingResult forRecentDays(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        return new PeriodRoutingResult(start, end, days, null, null, true, true);
    }

    public static PeriodRoutingResult forWeekday(DayOfWeek dayOfWeek, int weekLookback) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusWeeks(weekLookback);
        return new PeriodRoutingResult(start, end, null, dayOfWeek, weekLookback, true, true);
    }

    public static PeriodRoutingResult defaultMonthlyWithRecent() {
        return forRecentDays(30);
    }

    public boolean isDefault() {
        return recentDays != null && recentDays == 30 && dayOfWeek == null && startDate != null && endDate != null;
    }
}
