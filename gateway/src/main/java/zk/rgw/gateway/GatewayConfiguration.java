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

package zk.rgw.gateway;

import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import zk.rgw.http.conf.ServerConfiguration;

@SuppressWarnings("java:S1075")
// @formatter:off
@Command(
    name = "reactor-gateway",
    version = "0.0.1",
    mixinStandardHelpOptions = true,
    description = "A reactive http api gateway server.",
    usageHelpAutoWidth = true,
    showDefaultValues = true
)
// @formatter:on
public class GatewayConfiguration extends ServerConfiguration {

    private static final String DEFAULT_CONF_FILE = "conf/rgw-gateway.properties";

    private static final int DEFAULT_PORT = 8000;

    @Getter
    @Setter
    @CommandLine.Option(names = "--dashboard.address", description = "The dashboard address.")
    private String dashboardAddress = "http://127.0.0.1:7000";

    @Getter
    @Setter
    @CommandLine.Option(names = "--dashboard.api.context.path", description = "The api context path of dashboard.")
    private String dashboardApiContextPath = "/rgw/api";

    @Getter
    @Setter
    @CommandLine.Option(names = "--dashboard.auth.key", description = "The key for authorization to dashboard.")
    private String dashboardAuthKey = "";

    @Getter
    @Setter
    @CommandLine.Option(names = "--environment.id", required = true, description = "The environment id used to fetch route definitions from dashboard.")
    private String environmentId;

    @Getter
    @Setter
    @CommandLine.Option(names = "--heartbeat.interval", description = "The interval of for send heartbeat to dashboard.")
    private int heartbeatInterval = 30;

    @Getter
    @Setter
    @CommandLine.Option(names = "--server.schema", description = "The uri schema used for dashboard send http request to gateway, http or https.")
    private String serverSchema = "http";

    public GatewayConfiguration() {
        this.confFile = DEFAULT_CONF_FILE;
        this.serverPort = DEFAULT_PORT;
    }

}
