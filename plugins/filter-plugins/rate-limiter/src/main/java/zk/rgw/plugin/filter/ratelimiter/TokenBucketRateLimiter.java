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

package zk.rgw.plugin.filter.ratelimiter;

import reactor.core.publisher.Mono;

public class TokenBucketRateLimiter implements RateLimiter {

    private final long burstCapacity;

    private final long replenishRate;

    private final long cost;

    private long leftCapacity;

    private long updateTimeMs;

    public TokenBucketRateLimiter(TokenBucketRateLimitConf rateLimiterConf) {
        this.burstCapacity = rateLimiterConf.getBurstCapacity();
        this.replenishRate = rateLimiterConf.getReplenishRate();
        this.cost = rateLimiterConf.getCost();
        this.leftCapacity = this.burstCapacity;
        this.updateTimeMs = System.currentTimeMillis();
    }

    @Override
    public Mono<Boolean> isAllowed() {
        boolean result;
        synchronized (this) {
            long currentTimeMs = System.currentTimeMillis();
            leftCapacity = Math.min(burstCapacity, leftCapacity + replenishRate * (currentTimeMs - updateTimeMs) / 1000);
            updateTimeMs = currentTimeMs;
            if (leftCapacity < cost) {
                result = false;
            } else {
                leftCapacity -= cost;
                result = true;
            }
        }
        return Mono.just(result);
    }

}