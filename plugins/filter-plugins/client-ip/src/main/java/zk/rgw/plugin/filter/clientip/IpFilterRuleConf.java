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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IpFilterRuleConf implements Serializable {

    private IpFilterRuleType ipFilterRuleType = IpFilterRuleType.ACCEPT;

    private Set<String> singleIps = new HashSet<>();

    private Set<String> subNets = new HashSet<>();

    private Set<String> ipRanges = new HashSet<>();

    public IpFilterRuleConf(String ips) {
        this(ips, IpFilterRuleType.ACCEPT);
    }

    public IpFilterRuleConf(String ips, IpFilterRuleType ruleType) {
        ipFilterRuleType = ruleType;
        String[] segments = ips.split(";");
        for (String text : segments) {
            text = text.trim();
            if (IpUtil.isIpV4(text)) {
                singleIps.add(text);
            } else if (IpUtil.isIpV4Subnet(text)) {
                subNets.add(text);
            } else if (IpUtil.isIpV4Range(text)) {
                ipRanges.add(text);
            }
        }
    }

    public boolean verify() {
        for (String ip : singleIps) {
            if (!IpUtil.isIpV4(ip)) {
                return false;
            }
        }

        for (String ipSubnet : subNets) {
            if (!IpUtil.isIpV4Subnet(ipSubnet)) {
                return false;
            }
        }

        for (String ipRange : ipRanges) {
            if (!IpUtil.isIpV4Range(ipRange)) {
                return false;
            }
        }
        return true;
    }

}
