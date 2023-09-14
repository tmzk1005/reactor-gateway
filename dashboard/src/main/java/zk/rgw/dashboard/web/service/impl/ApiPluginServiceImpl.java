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

package zk.rgw.dashboard.web.service.impl;

import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import zk.rgw.common.definition.PluginInstanceDefinition;
import zk.rgw.common.util.JsonUtil;
import zk.rgw.dashboard.framework.context.ContextUtil;
import zk.rgw.dashboard.framework.exception.BizException;
import zk.rgw.dashboard.web.bean.dto.ApiPluginDto;
import zk.rgw.dashboard.web.bean.entity.ApiPlugin;
import zk.rgw.dashboard.web.repository.ApiPluginRepository;
import zk.rgw.dashboard.web.repository.factory.RepositoryFactory;
import zk.rgw.dashboard.web.service.ApiPluginService;

public class ApiPluginServiceImpl implements ApiPluginService {

    private final ApiPluginRepository apiPluginRepository = RepositoryFactory.get(ApiPluginRepository.class);

    @Override
    public Mono<Void> installPlugin(ApiPluginDto apiPluginDto) {
        return Mono.error(BizException.of("暂时不支持安装插件"));
    }

    @Override
    public Flux<ApiPlugin> getAllPlugins() {
        Mono<Bson> filterMono = ContextUtil.getUser().map(user -> {
            if (user.isSystemAdmin()) {
                return Filters.empty();
            } else {
                return Filters.or(
                        Filters.eq("organizationId", new ObjectId(user.getOrganization().getId())),
                        Filters.eq("organizationId", null)
                );
            }
        });
        return filterMono.flatMapMany(filter -> apiPluginRepository.find(filter, Sorts.ascending("uiOrder")));
    }

    @Override
    public Mono<Boolean> checkPluginDefinition(PluginInstanceDefinition definition) {
        return apiPluginRepository.findOneByNameAndVersion(definition.getName(), definition.getVersion())
                .switchIfEmpty(Mono.error(BizException.of("引用了不存在的插件，请检查插件的名称和版本号")))
                .map(apiPlugin -> validateJsonSchema(apiPlugin.getJsonSchema(), definition.getJsonConf()));
    }

    private boolean validateJsonSchema(String schemaDef, String jsonStr) {
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema jsonSchema = jsonSchemaFactory.getSchema(schemaDef);
        JsonNode jsonNode;
        try {
            jsonNode = JsonUtil.str2JsonNode(jsonStr);
        } catch (JsonProcessingException exception) {
            return false;
        }
        Set<ValidationMessage> errMessages = jsonSchema.validate(jsonNode);
        return errMessages.isEmpty();
    }

}
