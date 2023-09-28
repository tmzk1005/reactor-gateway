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

package zk.rgw.gateway.test.filter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import com.sun.net.httpserver.HttpServer;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import zk.rgw.common.definition.IdRouteDefinition;
import zk.rgw.common.definition.PluginInstanceDefinition;
import zk.rgw.common.definition.RouteDefinition;
import zk.rgw.gateway.ForTestUpStreamHttpServerHelper;
import zk.rgw.gateway.TestGatewayServer;
import zk.rgw.gateway.route.RouteConverter;
import zk.rgw.http.route.Route;
import zk.rgw.http.utils.UriBuilder;

class BuiltinFiltersTest {

    private static TestGatewayServer testGatewayServer;

    private static HttpClient httpClient;

    private static UriBuilder uriBuilder;

    @BeforeAll
    static void initTestGatewayServer() throws Exception {
        testGatewayServer = new TestGatewayServer();
        new Thread(testGatewayServer::start).start();
        testGatewayServer.waitStarted();
        httpClient = HttpClient.newHttpClient();
        String baseUri = "http://" + TestGatewayServer.HOST + ":" + TestGatewayServer.PORT;
        uriBuilder = new UriBuilder(baseUri);
    }

    @AfterAll
    static void stopTestGatewayServer() {
        testGatewayServer.stop();
    }

    @Test
    void testNotFound() throws Exception {
        URI uri = uriBuilder.build();
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(uri).method(HttpMethod.GET.name(), HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<Void> httpResponse = httpClient.send(httpRequest, responseInfo -> HttpResponse.BodySubscribers.discarding());
        int statusCode = httpResponse.statusCode();
        Assertions.assertEquals(HttpResponseStatus.NOT_FOUND.code(), statusCode);
    }

    @Test
    void testMock() throws Exception {
        String path = "/test-mock";
        Route route = createRoute(
                HttpMethod.GET.name(),
                path,
                createPluginInstanceDefinition(
                        "zk.rgw.plugin.filter.mock.http.HttpMockFilter",
                        """
                                {
                                    "statusCode": 200,
                                    "content": "hello",
                                    "headers": {
                                        "Content-Type": [
                                            "text/plain"
                                        ]
                                    }
                                }"""
                )
        );
        testGatewayServer.setRoute(route);

        URI uri = uriBuilder.path(path).build();
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(uri).method(HttpMethod.GET.name(), HttpRequest.BodyPublishers.noBody()).build();

        HttpResponse<String> httpResponse = httpClient.send(httpRequest, responseInfo -> HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8));
        int statusCode = httpResponse.statusCode();
        Assertions.assertEquals(HttpResponseStatus.OK.code(), statusCode);
        Assertions.assertEquals("hello", httpResponse.body());
    }

    @Test
    void testProxyHttp() throws Exception {
        String path = "/test-http-proxy";
        Route route = createRoute(
                HttpMethod.GET.name(),
                path,
                createPluginInstanceDefinition(
                        "zk.rgw.plugin.filter.proxyhttp.ProxyHttpFilter",
                        """
                                {
                                    "upstreamEndpoint": "http://127.0.0.1:6000/hello",
                                    "upstreamEndpointDecideFuncDef": null,
                                    "timeout": 0
                                }"""
                )
        );
        testGatewayServer.setRoute(route);

        String respBody = "hello!";
        HttpServer httpServer = ForTestUpStreamHttpServerHelper.create(
                "/hello", HttpHeaderValues.TEXT_PLAIN.toString(), HttpResponseStatus.OK.code(), respBody
        );
        httpServer.start();

        URI uri = uriBuilder.path(path).build();
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(uri).method(HttpMethod.GET.name(), HttpRequest.BodyPublishers.noBody()).build();

        HttpResponse<String> httpResponse = httpClient.send(httpRequest, responseInfo -> HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8));
        int statusCode = httpResponse.statusCode();
        Assertions.assertEquals(HttpResponseStatus.OK.code(), statusCode);
        Assertions.assertEquals(respBody, httpResponse.body());

        httpServer.stop(0);
    }

    private static PluginInstanceDefinition createPluginInstanceDefinition(String fullClassName, String filterConf) {
        PluginInstanceDefinition pluginInstanceDefinition = new PluginInstanceDefinition();
        pluginInstanceDefinition.setFullClassName(fullClassName);
        pluginInstanceDefinition.setJsonConf(filterConf);
        pluginInstanceDefinition.setBuiltin(true);
        pluginInstanceDefinition.setName(fullClassName);
        pluginInstanceDefinition.setVersion("1.0.0");
        return pluginInstanceDefinition;
    }

    private static Route createRoute(String method, String path, PluginInstanceDefinition... pluginInstanceDefinitions) throws Exception {
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setMethods(Set.of(method));
        routeDefinition.setPath(path);
        for (PluginInstanceDefinition pluginInstanceDefinition : pluginInstanceDefinitions) {
            routeDefinition.getPluginDefinitions().add(pluginInstanceDefinition);
        }

        IdRouteDefinition idRouteDefinition = new IdRouteDefinition();
        idRouteDefinition.setId("test");
        idRouteDefinition.setOrgId("test");
        idRouteDefinition.setRouteDefinition(routeDefinition);
        return RouteConverter.convertRouteDefinition(idRouteDefinition);
    }

}
