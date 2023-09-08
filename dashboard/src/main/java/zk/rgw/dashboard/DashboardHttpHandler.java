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
package zk.rgw.dashboard;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import zk.rgw.http.route.locator.RouteLocator;
import zk.rgw.http.server.HttpHandler;
import zk.rgw.plugin.util.ResponseUtil;

public class DashboardHttpHandler extends HttpHandler {

    private final String apiContextPath;

    private final Path staticResourceRootPath;

    public DashboardHttpHandler(RouteLocator routeLocator, String apiContextPath, Path staticResourceRootPath) {
        super(routeLocator);
        this.apiContextPath = apiContextPath;
        this.staticResourceRootPath = staticResourceRootPath;
    }

    @Override
    public Mono<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        String path = request.fullPath();
        if (path.startsWith(apiContextPath)) {
            return super.apply(request, response);
        } else {
            // 注意，request.fullPath()和request.path()是不同的！这里为了后续静态资源处理方便，传递过去的是request.path()
            return serveStaticResource(request.path(), response);
        }
    }

    private Mono<Void> serveStaticResource(String path, HttpServerResponse response) {
        if ("".equals(path) || "/".equals(path) || !isStaticResourceReq(path)) {
            // 最后一个条件是为了解决前端vue使用history路由模式时按浏览器F5刷新页面会出现404的问题
            // 凡是不以${apiContextPath}开头的都认为是访问静态资源
            path = "index.html";
        }
        Path resourcePath = staticResourceRootPath.resolve(path).toAbsolutePath().normalize();
        if (!resourcePath.startsWith(staticResourceRootPath) || Files.notExists(resourcePath)) {
            return ResponseUtil.sendNotFound(response);
        }
        File file = resourcePath.toFile();
        if (!file.isFile()) {
            return ResponseUtil.sendNotFound(response);
        }

        long contentLength = file.length();
        response.header(HttpHeaderNames.CONTENT_LENGTH, contentLength + "");

        String suffix = path.substring(path.lastIndexOf(".") + 1);
        String contentType = MimeTypesHelper.getContentTypeBySuffix(suffix);
        if (Objects.nonNull(contentType)) {
            response.header(HttpHeaderNames.CONTENT_TYPE, contentType);
        }
        return Mono.from(response.sendFile(resourcePath));
    }

    private boolean isStaticResourceReq(String path) {
        // 是否是请求vue打包后的文件
        return path.startsWith("assets/") || path.equals("favicon.ico");
    }

    private static class MimeTypesHelper {

        private static final Map<String, String> mimeTypes = new HashMap<>(32);

        static {
            // 按需设置了部分，没有设置全，要设置全可以参考nginx的conf目录下的mime.types文件
            mimeTypes.put("html", "text/html");
            mimeTypes.put("htm", "text/html");
            mimeTypes.put("shtml", "text/html");
            mimeTypes.put("css", "text/css");
            mimeTypes.put("xml", "text/xml");
            mimeTypes.put("gif", "image/gif");
            mimeTypes.put("jpeg", "image/jpeg");
            mimeTypes.put("js", "application/javascript ");
            mimeTypes.put("atom", "application/atom+xml");
            mimeTypes.put("rss", "application/rss+xml");
            mimeTypes.put("txt", "text/plain");
            mimeTypes.put("json", "application/json");
            mimeTypes.put("png", "image/png");
            mimeTypes.put("svg", "image/svg+xml");
            mimeTypes.put("svgz", "image/svg+xml");
            mimeTypes.put("ico", "image/x-icon");
        }

        private MimeTypesHelper() {
        }

        public static String getContentTypeBySuffix(String suffix) {
            return mimeTypes.get(suffix);
        }

    }

}
