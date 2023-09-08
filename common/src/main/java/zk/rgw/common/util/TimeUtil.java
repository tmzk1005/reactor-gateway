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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class TimeUtil {

    public static final String ZONE_ID_STR = "CTT";

    public static final ZoneId TZ_ID = ZoneId.of(ZONE_ID_STR, ZoneId.SHORT_IDS);

    public static final long SECOND_IN_MILLIS = 1000L;

    public static final long MINUTE_IN_MILLS = 60 * SECOND_IN_MILLIS;

    public static final long HOUR_IN_MILLS = 60 * MINUTE_IN_MILLS;

    public static final long DAY_IN_MILLIS = 24 * HOUR_IN_MILLS;

    private TimeUtil() {
    }

    /**
     * 将给定的的时间的 "分钟零头" 去掉，即把时间向整分钟对齐，然后再减去一个给定的整分钟数
     */
    public static long minutesAgo(Instant instant, int minutes) {
        return instant.truncatedTo(ChronoUnit.MINUTES).minusMillis(minutes * MINUTE_IN_MILLS).toEpochMilli();
    }

    /**
     * 将给定的的时间的 "小时零头" 去掉，即把时间向整小时对齐，然后再减去一个给定的整小时数
     */
    public static long hoursAgo(Instant instant, int hours) {
        return instant.truncatedTo(ChronoUnit.HOURS).minusMillis(hours * HOUR_IN_MILLS).toEpochMilli();
    }

    /**
     * 将给定的的时间的 "天零头" 去掉，即把时间向整天对齐，然后再减去一个给定的整天数
     */
    public static long daysAgo(Instant instant, int days) {
        return instant.truncatedTo(ChronoUnit.DAYS).minusMillis(days * DAY_IN_MILLIS).toEpochMilli();
    }

}
