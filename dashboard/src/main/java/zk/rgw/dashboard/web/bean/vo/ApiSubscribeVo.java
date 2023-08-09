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

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import zk.rgw.common.util.JsonUtil;
import zk.rgw.dashboard.framework.xo.Vo;
import zk.rgw.dashboard.web.bean.entity.ApiSubscribe;

@Getter
@Setter
public class ApiSubscribeVo implements Vo<ApiSubscribe> {

    private String id;

    private String apiId;

    private String apiName;

    private String appId;

    private String appName;

    private String userId;

    private String userName;

    private String appOrganizationId;

    private String appOrganizationName;

    private String apiOrganizationId;

    private String apiOrganizationName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtil.DEFAULT_DATE_TIME_PATTERN)
    private Instant applyTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtil.DEFAULT_DATE_TIME_PATTERN)
    private Instant handleTime;

    private ApiSubscribe.State state;

    @SuppressWarnings("unchecked")
    @Override
    public ApiSubscribeVo initFromPo(ApiSubscribe po) {
        this.id = po.getId();
        this.apiId = po.getApi().getId();
        this.apiName = po.getApi().getName();
        this.appId = po.getApp().getId();
        this.appName = po.getApp().getName();
        this.userId = po.getUser().getId();
        this.userName = po.getUser().getNickname();
        this.appOrganizationId = po.getAppOrganization().getId();
        this.appOrganizationName = po.getAppOrganization().getName();
        this.apiOrganizationId = po.getApiOrganization().getId();
        this.apiOrganizationName = po.getApiOrganization().getName();
        this.applyTime = po.getApplyTime();
        this.handleTime = po.getHandleTime();
        this.state = po.getState();
        return this;
    }

}
