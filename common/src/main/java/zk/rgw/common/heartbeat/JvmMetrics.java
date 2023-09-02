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
package zk.rgw.common.heartbeat;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JvmMetrics {

    private int availableProcessors;

    private double systemLoadAverage;

    private int threadCount;

    private VmMemoryUsage heapMemoryUsage = new VmMemoryUsage();

    private VmMemoryUsage directMemoryUsage = new VmMemoryUsage();

    public void refresh() {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.availableProcessors = operatingSystemMXBean.getAvailableProcessors();
        this.systemLoadAverage = operatingSystemMXBean.getSystemLoadAverage();

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.heapMemoryUsage.copy(memoryMXBean.getHeapMemoryUsage());
        this.directMemoryUsage.copy(memoryMXBean.getNonHeapMemoryUsage());

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        threadCount = threadMXBean.getThreadCount();
    }

    @Getter
    @Setter
    public static class VmMemoryUsage {

        private long init;
        private long used;
        private long committed;
        private long max;

        public void copy(MemoryUsage memoryUsage) {
            this.init = memoryUsage.getInit();
            this.used = memoryUsage.getUsed();
            this.committed = memoryUsage.getCommitted();
            this.max = memoryUsage.getMax();
        }

    }

}
