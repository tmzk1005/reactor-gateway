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

package zk.rgw.plugin.filter.clientip;

import java.util.HashSet;
import java.util.Set;

import io.netty.util.NetUtil;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

public class IpFilterRule {

    @Getter
    private final IpFilterRuleType ipFilterRuleType;

    private final Set<String> singleIps;

    private final Set<IpSection> ipSections;

    public IpFilterRule(IpFilterRuleConf ipFilterRuleConf) {
        this.ipFilterRuleType = ipFilterRuleConf.getIpFilterRuleType();
        this.singleIps = ipFilterRuleConf.getSingleIps();
        this.ipSections = parseIpSectionsFromConf(ipFilterRuleConf);
    }

    private static Set<IpSection> parseIpSectionsFromConf(IpFilterRuleConf ipFilterRuleConf) {
        Set<IpSection> ipSections = new HashSet<>();
        for (String ipRangeStr : ipFilterRuleConf.getIpRanges()) {
            final String[] ipPair = ipRangeStr.split("-");
            ipSections.add(new IpSection(IpUtil.ipv4ToLong(ipPair[0]), IpUtil.ipv4ToLong(ipPair[1])));
        }
        for (String subnetStr : ipFilterRuleConf.getSubNets()) {
            final String[] ipAndCidrPrefix = subnetStr.split("/");
            long ipLong = IpUtil.ipv4ToLong(ipAndCidrPrefix[0]);
            byte idrPrefix = Byte.parseByte(ipAndCidrPrefix[1]);
            long min = ipLong >> idrPrefix << idrPrefix;
            long max = min | (0xffffffffL >> (32 - idrPrefix));
            ipSections.add(new IpSection(min, max));
        }
        return ipSections;
    }

    public boolean acceptIp(String ip) {
        return (ipFilterRuleType == IpFilterRuleType.ACCEPT) == innerAccept(ip);
    }

    private boolean innerAccept(String ip) {
        if (NetUtil.isValidIpV4Address(ip)) {
            return acceptIpV4(ip);
        }
        // 暂时不支持对ipv6过滤,先直接返回false,后续有需要再实现ipv6过滤功能
        return false;
    }

    private boolean acceptIpV4(String ip) {
        ip = ip.trim();
        if (singleIps.contains(ip)) {
            return true;
        }
        final long ipLong = IpUtil.ipv4ToLong(ip);
        for (IpSection ipSection : ipSections) {
            if (ipLong >= ipSection.min && ipLong <= ipSection.max) {
                return true;
            }
        }
        return false;
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    public static class IpSection {
        long min;
        long max;
    }

}
