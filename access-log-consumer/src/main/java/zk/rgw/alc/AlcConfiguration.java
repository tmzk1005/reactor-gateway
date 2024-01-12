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

package zk.rgw.alc;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

import zk.rgw.common.conf.CommonConfiguration;

@SuppressWarnings("java:S1075")
// @formatter:off
@CommandLine.Command(
        name = "reactor-access-log-consumer",
        version = "0.0.1",
        mixinStandardHelpOptions = true,
        description = "A kafka consumer application, read access log written by gateway, and then write to mongodb.",
        usageHelpAutoWidth = true,
        showDefaultValues = true
)
// @formatter:on
public class AlcConfiguration extends CommonConfiguration {

    private static final String DEFAULT_CONF_FILE = "conf/rgw-access-log-consumer.properties";

    public AlcConfiguration() {
        this.confFile = DEFAULT_CONF_FILE;
    }

    @Getter
    @Setter
    @CommandLine.Option(names = "--kafka.bootstrap.servers", description = "The kafka bootstrap server used to read access log.")
    private String kafkaBootstrapServers = "127.0.0.1:9092";

    @Getter
    @Setter
    @CommandLine.Option(names = "--mongodb.database", description = "Mongodb database name to store access logs to.")
    private String mongodbDatabaseName = "rgw";

    @Getter
    @Setter
    @CommandLine.Option(names = "--mongodb.connection", description = "Mongodb connection string used to connect to mongo.")
    private String mongodbConnectString = "mongodb://127.0.0.1:27017";

    @Getter
    @Setter
    @CommandLine.Option(
            names = "--environments", split = ",", defaultValue = "dev,test,prod",
            description = "Environment ids for consume access logs in kafka, split by ',' ."
    )
    private List<String> environments;

}
