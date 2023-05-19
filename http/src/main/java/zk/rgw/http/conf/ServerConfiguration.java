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

package zk.rgw.http.conf;

import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Option;

import zk.rgw.common.conf.CommonConfiguration;

public class ServerConfiguration extends CommonConfiguration {

    protected ServerConfiguration() {
    }

    @Getter
    @Setter
    @Option(names = "--server.host", description = "The host http server will bind to")
    protected String serverHost = "0.0.0.0";

    @Getter
    @Setter
    @Option(names = "--server.port", description = "The port http server will listen to")
    protected int serverPort = 8000;

}
