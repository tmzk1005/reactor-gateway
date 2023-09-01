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
package zk.rgw.common.heartbeat;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GwHeartbeatPayload {

    /**
     * 通过向dashboard注册而被分配到的节点id
     */
    private String nodeId;

    /**
     * 节点上的最新拉取到的api操作序列号
     */
    private long apiOpSeq;

    /**
     * 每个组织的环境操作序列号
     */
    private Map<String, Long> orgEnvOpSeqMap = new HashMap<>();

}
