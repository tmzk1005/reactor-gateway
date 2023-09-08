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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimeUtilTest {

    @Test
    void test() {
        Instant instant = Instant.now();
        Assertions.assertEquals(0, TimeUtil.minutesAgo(instant, 1) % TimeUtil.MINUTE_IN_MILLS);
        Assertions.assertEquals(0, TimeUtil.hoursAgo(instant, 1) % TimeUtil.HOUR_IN_MILLS);
        Assertions.assertEquals(0, TimeUtil.daysAgo(instant, 1) % TimeUtil.DAY_IN_MILLIS);
    }

}
