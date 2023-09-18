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

package zk.rgw.common.constant;

import java.util.Map;

public class Constants {

    private Constants() {
    }

    public static final String ACCESS_LOG_KAFKA_TOPIC_NAME_PREFIX = "rgw-access-log-";

    public static final Map<String, String> BUILT_IN_ENV_IDS = Map.of(
            "dev", "e00000000000000000000001",
            "test", "e00000000000000000000002",
            "prod", "e00000000000000000000003"
    );

}
