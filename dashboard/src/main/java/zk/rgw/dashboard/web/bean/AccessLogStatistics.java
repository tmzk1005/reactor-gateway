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
package zk.rgw.dashboard.web.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonIgnore;

/**
 * 给定的时间区间内各个http响应状态码的统计个数
 */
@Getter
@Setter
public class AccessLogStatistics {

    /**
     * 时间区间内响应码为1xx的个数
     */
    private long count1xx;

    /**
     * 时间区间内响应码为2xx的个数
     */
    private long count2xx;

    /**
     * 时间区间内响应码为3xx的个数
     */
    private long count3xx;

    /**
     * 时间区间内响应码为4xx的个数
     */
    private long count4xx;

    /**
     * 时间区间内响应码为5xx的个数
     */
    private long count5xx;

    /**
     * 时间区间内所有请求的耗时累积和，单位为毫秒
     */
    private long millisCost;

    /**
     * 时间区间内所有上行流量累积和，单位为字节
     */
    private long upFlow;

    /**
     * 时间区间内所有下行流量累积和，单位为字节
     */
    private long downFlow;

    @JsonProperty("succeedCount")
    @BsonIgnore
    public long succeedCount() {
        return count1xx + count2xx + count3xx;
    }

    @JsonProperty("failedCount")
    @BsonIgnore
    public long failedCount() {
        return count4xx + count5xx;
    }

    @JsonProperty("totalCount")
    @BsonIgnore
    public long totalCount() {
        return succeedCount() + failedCount();
    }

    @JsonProperty("avgMillisCost")
    @BsonIgnore
    public long avgMillisCost() {
        long totalCount = totalCount();
        if (totalCount == 0) {
            return 0L;
        }
        return millisCost / totalCount;
    }

}
