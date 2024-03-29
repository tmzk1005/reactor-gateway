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
package zk.rgw.dashboard.web.bean.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import zk.rgw.dashboard.framework.xo.Vo;
import zk.rgw.dashboard.web.bean.entity.EnvBinding;

@Getter
@Setter
public class EnvBindingVo implements Vo<EnvBinding> {

    private String envId;

    private String envName;

    private String orgId;

    private String orgName;

    private Map<String, String> variables = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public EnvBindingVo initFromPo(EnvBinding poInstance) {
        this.envId = poInstance.getEnvironment().getId();
        this.envName = poInstance.getEnvironment().getName();
        this.orgId = poInstance.getOrganization().getId();
        this.orgName = poInstance.getOrganization().getName();
        this.variables = poInstance.getVariables();
        return this;
    }

    @JsonProperty
    public List<KvPair> getVariableList() {
        List<KvPair> list = new ArrayList<>();
        if (Objects.nonNull(variables)) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                list.add(new KvPair(entry.getKey(), entry.getValue()));
            }
        }
        return list;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KvPair {
        private String varName;
        private String varValue;
    }

}
