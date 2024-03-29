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

import lombok.Getter;
import lombok.Setter;

import zk.rgw.dashboard.framework.xo.OperatorAuditable;
import zk.rgw.dashboard.web.bean.entity.User;

@Getter
@Setter
public abstract class AuditableVo extends TimeAuditableVo {

    protected SimpleUserVo createdBy;

    protected SimpleUserVo lastModifiedBy;

    public void copyOperatorAuditableInfo(OperatorAuditable<User> operatorAuditable) {
        this.createdBy = new SimpleUserVo().initFromPo(operatorAuditable.getCreatedBy());
        this.lastModifiedBy = new SimpleUserVo().initFromPo(operatorAuditable.getLastModifiedBy());
    }

}
