package com.mindcare.emotionservice.shared.util;

import com.mindcare.emotionservice.shared.exception.InvalidRequestException;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public enum TrendBucket {
    DAY,
    WEEK,
    MONTH;

    public static TrendBucket parse(String value) {
        if (value == null) {
            throw new InvalidRequestException("INVALID_BUCKET", "bucket must not be null");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("INVALID_BUCKET", "bucket must be DAY, WEEK or MONTH");
        }
    }

    public OffsetDateTime startOf(OffsetDateTime timestamp, ZoneId timezone) {
        ZonedDateTime local = timestamp.atZoneSameInstant(timezone);
        ZonedDateTime start = switch (this) {
            case DAY -> local.truncatedTo(ChronoUnit.DAYS);
            case WEEK -> local.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .truncatedTo(ChronoUnit.DAYS);
            case MONTH -> local.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        };
        return start.toOffsetDateTime();
    }

    public OffsetDateTime endOf(OffsetDateTime periodStart, ZoneId timezone) {
        ZonedDateTime start = periodStart.atZoneSameInstant(timezone);
        return (switch (this) {
            case DAY -> start.plusDays(1);
            case WEEK -> start.plusWeeks(1);
            case MONTH -> start.plusMonths(1);
        }).toOffsetDateTime();
    }
}
