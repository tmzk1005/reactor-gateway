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

package zk.rgw.common.definition;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccessLogConf {

    public static final long BODY_LIMIT_MAX = 1L << 20;

    private boolean reqHeadersEnabled = false;

    private boolean respHeadersEnabled = false;

    private boolean reqBodyEnabled = false;

    private boolean respBodyEnabled = false;

    private long bodyLimit = BODY_LIMIT_MAX;

    public void setBodyLimitMax(long bodyLimit) {
        if (bodyLimit <= 0) {
            throw new IllegalArgumentException("body limit can not must bigger than 0.");
        }
        this.bodyLimit = Math.min(bodyLimit, BODY_LIMIT_MAX);
    }

}
