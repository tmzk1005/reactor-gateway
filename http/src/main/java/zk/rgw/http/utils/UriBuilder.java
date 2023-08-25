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

package zk.rgw.http.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;

public class UriBuilder {

    private String scheme;

    private String userInfo;

    private String host;

    private int port = -1;

    private String path;

    private final Map<String, List<String>> queryParams;

    private String fragment;

    public UriBuilder(String uri) throws URISyntaxException {
        this(new URI(uri));
    }

    public UriBuilder(URI uri) {
        this.scheme = uri.getScheme();
        this.userInfo = uri.getUserInfo();
        this.host = uri.getHost();
        this.port = uri.getPort();
        this.path = uri.getPath();
        this.fragment = uri.getFragment();
        this.queryParams = queryToMap(uri.getQuery());
    }

    private static Map<String, List<String>> queryToMap(String query) {
        if (Objects.isNull(query)) {
            return new HashMap<>(1);
        } else {
            return new QueryStringDecoder(query, false).parameters();
        }
    }

    private static String mapToQuery(Map<String, List<String>> map) {
        if (Objects.isNull(map) || map.isEmpty()) {
            return null;
        }
        QueryStringEncoder queryStringEncoder = new QueryStringEncoder("");
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                queryStringEncoder.addParam(key, value);
            }
        }
        // 去掉自带的问号，需要substring(1)
        return queryStringEncoder.toString().substring(1);
    }

    public URI build() throws URISyntaxException {
        return new URI(scheme, userInfo, host, port, path, mapToQuery(this.queryParams), fragment);
    }

    public UriBuilder scheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    public UriBuilder userInfo(String userInfo) {
        this.userInfo = userInfo;
        return this;
    }

    public UriBuilder host(String host) {
        this.host = host;
        return this;
    }

    public UriBuilder port(int port) {
        this.port = port;
        return this;
    }

    public UriBuilder path(String path) {
        this.path = path;
        return this;
    }

    public UriBuilder clearQueryParams() {
        this.queryParams.clear();
        return this;
    }

    public UriBuilder clearQueryParam(String name) {
        this.queryParams.remove(name);
        return this;
    }

    public UriBuilder queryParam(String name, String value) {
        if (queryParams.containsKey(name)) {
            queryParams.get(name).add(value);
        } else {
            List<String> list = new ArrayList<>(1);
            list.add(value);
            this.queryParams.put(name, list);
        }
        return this;
    }

    public UriBuilder removeQueryParam(String name) {
        queryParams.remove(name);
        return this;
    }

    public UriBuilder queryParam(String name, List<String> values) {
        for (String value : values) {
            queryParam(name, value);
        }
        return this;
    }

    public UriBuilder fragment(String fragment) {
        this.fragment = fragment;
        return this;
    }

}
