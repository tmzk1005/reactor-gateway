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

package zk.rgw.plugin.filter.circuitbreaker;

import java.util.Set;

import lombok.Data;

@Data
public class CircuitBreakerConf {

    /**
     * 被认为是需要增加熔断状态中错误数的状态码
     */
    private Set<Integer> failureCodes;

    /**
     * 触发熔断的失败次数阈值
     */
    private int failureCountThreshold = 50;

    /**
     * 统计失败次数和最小请求次数的时间窗口大小，单位为秒
     */
    private int slidingWindowSize = 100;

    /**
     * 触发熔断的最小调用次数
     */
    private int minimumCalls = 100;

    /**
     * 熔断持续时间，单位为秒
     */
    private int openStateDuration = 60;

    /**
     * 半开状态下，关闭熔断需要连续成功的次数
     */
    private int halfOpenStateCalls = 20;

    /**
     * 半开状态最大持续时间，单位为秒
     */
    private int halfOpenStateDuration = 30;

}
