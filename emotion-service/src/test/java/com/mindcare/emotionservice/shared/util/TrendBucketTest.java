package com.mindcare.emotionservice.shared.util;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrendBucketTest {

    private static final ZoneId HO_CHI_MINH = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void weekStartsOnMondayInRequestedTimezone() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-07-23T20:15:00Z");

        OffsetDateTime start = TrendBucket.WEEK.startOf(timestamp, HO_CHI_MINH);

        assertEquals(OffsetDateTime.parse("2026-07-20T00:00:00+07:00"), start);
        assertEquals(
                OffsetDateTime.parse("2026-07-27T00:00:00+07:00"),
                TrendBucket.WEEK.endOf(start, HO_CHI_MINH)
        );
    }

    @Test
    void monthUsesCalendarBoundaryInRequestedTimezone() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-10-31T18:00:00Z");

        OffsetDateTime start = TrendBucket.MONTH.startOf(timestamp, HO_CHI_MINH);

        assertEquals(OffsetDateTime.parse("2026-11-01T00:00:00+07:00"), start);
        assertEquals(
                OffsetDateTime.parse("2026-12-01T00:00:00+07:00"),
                TrendBucket.MONTH.endOf(start, HO_CHI_MINH)
        );
    }
}
