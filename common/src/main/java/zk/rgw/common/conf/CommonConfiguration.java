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

package zk.rgw.common.conf;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Option;

public class CommonConfiguration implements Runnable {

    @Getter
    protected boolean inited = false;

    @Getter
    @Setter
    protected Path rgwHome;

    protected CommonConfiguration() {
    }

    @Setter
    @Getter
    @Option(names = "--log.path", description = "The directory logs will be write to")
    protected String logPath = "logs";

    @Setter
    @Getter
    @Option(names = "--log.level", description = "The log level configuration for log4j2")
    protected LogLevel logLevel = LogLevel.INFO;

    @Setter
    @Getter
    @Option(names = "--conf", description = "The server configuration file")
    protected String confFile;

    @Override
    public void run() {
        inited = true;
    }

    public enum LogLevel {
        OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL
    }

}
