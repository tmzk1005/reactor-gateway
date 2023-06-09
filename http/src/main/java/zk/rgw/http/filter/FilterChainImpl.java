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

package zk.rgw.http.filter;

import java.util.List;

import reactor.core.publisher.Mono;

import zk.rgw.plugin.api.Exchange;
import zk.rgw.plugin.api.filter.Filter;
import zk.rgw.plugin.api.filter.FilterChain;

public class FilterChainImpl implements FilterChain {

    private int index;

    private final List<Filter> filters;

    public FilterChainImpl(List<Filter> filters) {
        this.filters = filters;
        this.index = 0;
    }

    @Override
    public Mono<Void> filter(Exchange exchange) {
        return Mono.defer(() -> {
            if (this.index < filters.size()) {
                Filter filter = filters.get(this.index);
                this.index++;
                return filter.filter(exchange, this);
            } else {
                return Mono.empty();
            }
        });
    }

}
