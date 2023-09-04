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
import java.time.ZoneOffset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimeUtilTest {

    @Test
    void test() {
        TimeUtil.Range range;
        Instant instant = Instant.now();

        range = TimeUtil.lastMinuteRange(instant);
        Assertions.assertEquals(0, range.getBegin() % 60_000L);

        range = TimeUtil.lastHourRange(instant);
        Assertions.assertEquals(0, range.getBegin() % 3600_000L);

        range = TimeUtil.lastDayRange(instant);
        ZoneOffset offset = TimeUtil.TZ_ID.getRules().getOffset(instant);
        long offsetInMillis = offset.getTotalSeconds() * 1000L;
        Assertions.assertEquals(0, range.getBegin() % 3600_000L);
        Assertions.assertEquals(86400_000L - offsetInMillis, range.getBegin() % 86400_000L);

        range = TimeUtil.lastMonthRange(instant);
        Assertions.assertEquals(0, range.getBegin() % 3600_000L);
        Assertions.assertTrue(range.getEnd() - range.getBegin() >= 28 * 24 * 3600_000L);
    }

}
