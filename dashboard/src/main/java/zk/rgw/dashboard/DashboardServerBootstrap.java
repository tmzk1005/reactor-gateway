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

import zk.rgw.common.bootstrap.Server;
import zk.rgw.common.bootstrap.ServerBootstrap;

public class DashboardServerBootstrap extends ServerBootstrap<DashboardConfiguration> {

    public static final String APP_NAME = "rgw-dashboard";

    protected DashboardServerBootstrap(String[] bootstrapArgs) {
        super(bootstrapArgs, new DashboardConfiguration(), APP_NAME);
    }

    @Override
    protected Server newServerInstance() {
        return new DashboardServer(configuration);
    }

    public static void main(String[] args) {
        new DashboardServerBootstrap(args).bootstrap();
    }

}
