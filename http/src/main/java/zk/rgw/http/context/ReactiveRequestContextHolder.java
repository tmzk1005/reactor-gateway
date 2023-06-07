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

package zk.rgw.http.context;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public class ReactiveRequestContextHolder {

    private static final String REQUEST_CONTEXT_KEY = "REQUEST_CONTEXT_KEY";

    private ReactiveRequestContextHolder() {
    }

    public static Mono<RequestContext> getContext() {
        return Mono.deferContextual(contextView -> Mono.just(Context.of(contextView)))
                .filter(ReactiveRequestContextHolder::hasRequestContext)
                .flatMap(ReactiveRequestContextHolder::getRequestContext);
    }

    private static boolean hasRequestContext(Context context) {
        return context.hasKey(REQUEST_CONTEXT_KEY);
    }

    private static Mono<RequestContext> getRequestContext(Context context) {
        return context.get(REQUEST_CONTEXT_KEY);
    }

    public static Context withRequestContext(Mono<? extends RequestContext> requestContext) {
        return Context.of(REQUEST_CONTEXT_KEY, requestContext);
    }

}
