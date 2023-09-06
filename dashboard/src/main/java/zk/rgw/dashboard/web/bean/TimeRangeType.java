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
package zk.rgw.dashboard.web.bean;

import java.time.Instant;

import zk.rgw.common.util.TimeUtil;

public enum TimeRangeType {

    LAST_ONE_HOUR, LAST_SIX_HOUR, LAST_HALF_DAY, LAST_DAY, LAST_MONTH, LAST_YEAR, ALL_TIME;

    public TimeUtil.Range toRange() {
        Instant instant = Instant.now();
        return switch (this) {
            case LAST_ONE_HOUR -> TimeUtil.lastHourRange(instant);
            case LAST_SIX_HOUR -> TimeUtil.hoursAgeRange(instant, 6);
            case LAST_HALF_DAY -> TimeUtil.hoursAgeRange(instant, 12);
            case LAST_DAY -> TimeUtil.hoursAgeRange(instant, 24);
            case LAST_MONTH -> TimeUtil.daysAgeRange(instant, 30);
            case LAST_YEAR -> null;
            case ALL_TIME -> null;
        };
    }

}
