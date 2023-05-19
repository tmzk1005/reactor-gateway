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

import picocli.CommandLine.Command;

import zk.rgw.http.conf.ServerConfiguration;

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

    public GatewayConfiguration() {
        this.confFile = DEFAULT_CONF_FILE;
        this.serverPort = DEFAULT_PORT;
    }

}
