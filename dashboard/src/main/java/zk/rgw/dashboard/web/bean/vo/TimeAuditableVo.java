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
import zk.rgw.dashboard.framework.xo.TimeAuditable;

@Getter
@Setter
public abstract class TimeAuditableVo {


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtil.DEFAULT_DATE_TIME_PATTERN)
    protected Instant createdDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtil.DEFAULT_DATE_TIME_PATTERN)
    protected Instant lastModifiedDate;

    public void copyTimeAuditInfo(TimeAuditable<Instant> entity) {
        this.createdDate = entity.getCreatedDate();
        this.lastModifiedDate = entity.getLastModifiedDate();
    }

}
