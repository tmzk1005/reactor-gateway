/*
 * Copyright 2023 zoukang, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zk.rgw.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class TimeUtil {

    public static final String ZONE_ID_STR = "CTT";

    public static final ZoneId TZ_ID = ZoneId.of(ZONE_ID_STR, ZoneId.SHORT_IDS);

    private TimeUtil() {
    }

    public static Range lastMinuteRange(Instant instant) {
        return minutesAgoRange(instant, 1);
    }

    public static Range minutesAgoRange(Instant instant, int minutes) {
        long begin = instant.truncatedTo(ChronoUnit.MINUTES).minusSeconds(60L * minutes).toEpochMilli();
        return new Range(begin, begin + 60_000L);
    }

    public static Range lastHourRange(Instant instant) {
        return hoursAgeRange(instant, 1);
    }

    public static Range hoursAgeRange(Instant instant, int hourCount) {
        long begin = instant.truncatedTo(ChronoUnit.HOURS).minusSeconds(3600L * hourCount).toEpochMilli();
        return new Range(begin, begin + 3600_000L * hourCount);
    }

    public static Range lastDayRange(Instant instant) {
        return daysAgeRange(instant, 1);
    }

    public static Range daysAgeRange(Instant instant, int daysCount) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, TZ_ID);
        ZoneOffset offset = TZ_ID.getRules().getOffset(dateTime);
        long begin = dateTime.truncatedTo(ChronoUnit.DAYS).minusDays(daysCount).toEpochSecond(offset) * 1000L;
        return new Range(begin, begin + 24 * 3600_000L * daysCount);
    }

    public static Range lastMonthRange(Instant instant) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, TZ_ID).minusMonths(1);
        LocalDateTime beginOfMonth = LocalDateTime.of(dateTime.getYear(), dateTime.getMonth(), 1, 0, 0);
        ZoneOffset offset = TZ_ID.getRules().getOffset(dateTime);
        long begin = beginOfMonth.toEpochSecond(offset) * 1000L;
        int monthDays = beginOfMonth.getMonth().length(beginOfMonth.toLocalDate().isLeapYear());
        return new Range(begin, begin + monthDays * 24 * 3600_000L);
    }

    /**
     * 毫秒时间戳范围：[begin, end)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Range {
        private long begin;
        private long end;
    }

}
