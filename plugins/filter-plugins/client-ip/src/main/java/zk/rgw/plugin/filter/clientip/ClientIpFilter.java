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

import java.io.IOException;
import java.util.Objects;

import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.FilterChain;
import zk.rgw.plugin.api.filter.JsonConfFilterPlugin;
import zk.rgw.plugin.exception.PluginConfException;
import zk.rgw.plugin.util.ExchangeUtil;
import zk.rgw.plugin.util.ResponseUtil;

public class ClientIpFilter implements JsonConfFilterPlugin {

    private static final String TAG_PASS_WHITE_LIST = "IP过滤-白名单-通过";
    private static final String TAG_PASS_BLACK_LIST = "IP过滤-黑名单-通过";
    private static final String TAG_FAIL_WHITE_LIST = "IP过滤-白名单-拒绝";
    private static final String TAG_FAIL_BLACK_LIST = "IP过滤-黑名单-拒绝";

    private IpFilterRule ipFilterRule;

    @Override
    public void configure(String conf) throws PluginConfException {
        IpFilterRuleConf ipFilterRuleConf = new IpFilterRuleConf();
        try {
            OM.readerForUpdating(ipFilterRuleConf).readValue(conf);
        } catch (IOException ioException) {
            throw new PluginConfException(ioException.getMessage(), ioException);
        }
        this.ipFilterRule = new IpFilterRule(ipFilterRuleConf);
    }

    @Override
    public Mono<Void> filter(Exchange exchange, FilterChain chain) {
        String clientIp = Objects.requireNonNull(exchange.getRequest().remoteAddress()).getAddress().getHostAddress();
        if (ipFilterRule.acceptIp(clientIp)) {
            String tag = ipFilterRule.getIpFilterRuleType() == IpFilterRuleType.ACCEPT ? TAG_PASS_WHITE_LIST : TAG_PASS_BLACK_LIST;
            ExchangeUtil.addAuditTag(exchange, tag);
            return chain.filter(exchange);
        } else {
            String tag = ipFilterRule.getIpFilterRuleType() == IpFilterRuleType.ACCEPT ? TAG_FAIL_WHITE_LIST : TAG_FAIL_BLACK_LIST;
            ExchangeUtil.addAuditTag(exchange, tag);
            return ResponseUtil.sendStatus(exchange.getResponse(), HttpResponseStatus.FORBIDDEN);
        }
    }

}
