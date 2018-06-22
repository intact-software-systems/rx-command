package com.intact.rx.templates;

import java.time.*;
import java.time.format.DateTimeFormatter;

public final class Utility {

    private static final LocalTime endOfToday = LocalTime.of(23, 59, 59, 999999999);

    private Utility() {
    }

    public static String msecDateTimeFormatter(long msecs) {
        return DateTimeFormatter.ISO_DATE_TIME.format(
                ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(msecs),
                        ZoneId.systemDefault()
                )
        );
    }

    @SafeVarargs
    public static <T> T[] toArray(final T... items) {
        return items;
    }

    public static Duration timeUntilNext(LocalTime timeOfDay) {
        return timeOfDay.isBefore(LocalTime.now())
                ? Duration.between(LocalTime.now(), endOfToday).plus(Duration.between(LocalTime.MIDNIGHT, timeOfDay))
                : Duration.between(LocalTime.now(), timeOfDay);
    }
}
