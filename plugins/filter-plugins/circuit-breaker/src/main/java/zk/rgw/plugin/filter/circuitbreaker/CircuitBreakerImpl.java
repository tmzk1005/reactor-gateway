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

import lombok.Getter;
import reactor.core.publisher.Mono;

public class CircuitBreakerImpl extends AbstractCircuitBreaker {

    private final CircuitBreakerConf conf;

    private final CircleArray circleArray;

    private long stateChangeTime;

    private long updateTime;

    private CircuitBreakerState currentState;

    public CircuitBreakerImpl(CircuitBreakerConf circuitBreakerConf) {
        this.conf = circuitBreakerConf;
        this.currentState = CircuitBreakerState.CLOSED;
        this.circleArray = new CircleArray(conf.getSlidingWindowSize());
        this.stateChangeTime = this.updateTime = currentTimeInSecond();
    }

    @Override
    protected Mono<CircuitBreakerState> getState() {
        return Mono.just(computeState());
    }

    private synchronized CircuitBreakerState computeState() {
        if (CircuitBreakerState.CLOSED == currentState) {
            return currentState;
        }
        long curTime = System.currentTimeMillis() / 1000;
        long durationPast = curTime - stateChangeTime;
        if (CircuitBreakerState.OPEN == currentState) {
            if (durationPast > conf.getOpenStateDuration() + conf.getHalfOpenStateDuration()) {
                // 当前虽然记录的是OPEN状态，但是在最后一个更新计数后，一直没有请求到来，下一次来获取状态的时候，OPEN状态最长时间
                // 已经过去，甚至HALF_OPEN最长时间也已经过去，此时状态实际应该是CLOSED了，应该更新
                setStateClosed(stateChangeTime + conf.getOpenStateDuration() + conf.getHalfOpenStateDuration());
            } else if (durationPast > conf.getOpenStateDuration()) {
                // 情况和前一个类似，但是HALF_OPEN最长时间还没有过去，应该是HALF_OPEN
                setStateHalfOpen(stateChangeTime + conf.getOpenStateDuration());
            }
        } else if (durationPast > conf.getHalfOpenStateDuration()) {
            // 和另一种场景类似，在HALF_OPEN状态下持续了空闲了足够的时间，直接变为CLOSED状态
            setStateClosed(stateChangeTime + conf.getHalfOpenStateDuration());
        }
        return currentState;
    }

    @Override
    protected synchronized void incrementErrorCount() {
        tryMoveWindow();
        circleArray.errorIncrease();
        checkIfShouldChangeState();
    }

    @Override
    protected synchronized void incrementSuccessCount() {
        tryMoveWindow();
        circleArray.successIncrease();
        checkIfShouldChangeState();
    }

    private void checkIfShouldChangeState() {
        if (CircuitBreakerState.CLOSED == currentState) {
            if (circleArray.getErrorTotal() >= conf.getFailureCountThreshold() && circleArray.getTotal() >= conf.getMinimumCalls()) {
                setStateOpen();
            }
        } else if (CircuitBreakerState.HALF_OPEN == currentState) {
            if (circleArray.getErrorTotal() > 0) {
                setStateOpen();
            } else if (circleArray.getSuccessTotal() >= conf.getHalfOpenStateCalls()) {
                setStateClosed(currentTimeInSecond());
            }
        }
    }

    private void setStateOpen() {
        currentState = CircuitBreakerState.OPEN;
        stateChangeTime = currentTimeInSecond();
        clear();
    }

    private void setStateClosed(long time) {
        currentState = CircuitBreakerState.CLOSED;
        stateChangeTime = time;
        clear();
    }

    private void setStateHalfOpen(long time) {
        currentState = CircuitBreakerState.HALF_OPEN;
        stateChangeTime = time;
        clear();
    }

    private void tryMoveWindow() {
        long currentTime = currentTimeInSecond();
        if (updateTime != currentTime) {
            circleArray.moveWindow((int) (currentTime - updateTime));
            updateTime = currentTime;
        }
    }

    private void clear() {
        circleArray.clear();
        this.stateChangeTime = this.updateTime = currentTimeInSecond();
    }

    private static long currentTimeInSecond() {
        return System.currentTimeMillis() / 1000;
    }

    private static class CircleArray {

        private final int size;

        private final int[] successArray;

        private final int[] errorArray;

        @Getter
        private int successTotal;

        @Getter
        private int errorTotal;

        private int headIndex;

        private CircleArray(int size) {
            this.size = size;
            this.successArray = new int[size];
            this.errorArray = new int[size];
            this.headIndex = 0;
            this.successTotal = 0;
            this.errorTotal = 0;
        }

        private void moveWindowByOne() {
            this.headIndex = (headIndex + 1) % size;
            successTotal -= successArray[headIndex];
            successArray[headIndex] = 0;
            errorTotal -= errorArray[headIndex];
            errorArray[headIndex] = 0;
        }

        private void moveWindow(int step) {
            step = Math.max(step, size);
            for (int i = 0; i < step; ++i) {
                moveWindowByOne();
            }
        }

        private void successIncrease() {
            ++this.successArray[headIndex];
            ++this.successTotal;
        }

        private void errorIncrease() {
            ++this.errorArray[headIndex];
            ++this.errorTotal;
        }

        private int getTotal() {
            return successTotal + errorTotal;
        }

        public void clear() {
            // 这个方法应该可以是private的，但是pmd有误报，因此暂先用public
            headIndex = 0;
            successTotal = 0;
            errorTotal = 0;
            for (int i = 0; i < size; ++i) {
                successArray[i] = errorArray[i] = 0;
            }
        }

    }

}
