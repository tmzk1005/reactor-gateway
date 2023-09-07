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
package zk.rgw.dashboard.web.bean.vo.chart;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import zk.rgw.common.util.JsonUtil;
import zk.rgw.dashboard.web.bean.AccessLogStatisticsWithTime;

/**
 * 方便前端使用Echarts渲染折线图的数数据结构
 */
@Getter
@Setter
public class LineChartVo {

    private String title = "";

    @JsonProperty("xAxis")
    private NamedArray axisX = new NamedArray();

    @JsonProperty("yAxis")
    private NamedArray axisY = new NamedArray();

    private List<NamedArray> series = new ArrayList<>(0);

    public void setTitleAndNames(String title, String nameX, String nameY, String... seriesNames) {
        if (series.size() != seriesNames.length) {
            throw new IllegalArgumentException("Series name count not equals to series count.");
        }
        this.title = title;
        this.axisX.setName(nameX);
        this.axisY.setName(nameY);
        for (int i = 0; i < seriesNames.length; ++i) {
            series.get(i).setName(seriesNames[i]);
        }
    }

    public static LineChartVo apiCallsCountTrend(List<AccessLogStatisticsWithTime> dataList) {
        LineChartVo lineChartVo = new LineChartVo();
        lineChartVo.setTitle("请求数趋势");
        lineChartVo.axisX.setName("时间");
        lineChartVo.axisY.setName("请求数");
        NamedArray seriesCount1xx = new NamedArray("1XX请求数");
        NamedArray seriesCount2xx = new NamedArray("2XX请求数");
        NamedArray seriesCount3xx = new NamedArray("3XX请求数");
        NamedArray seriesCount4xx = new NamedArray("4XX请求数");
        NamedArray seriesCount5xx = new NamedArray("5XX请求数");
        NamedArray seriesCountError = new NamedArray("异常请求数");
        NamedArray seriesCountSuccess = new NamedArray("正常请求数");
        NamedArray seriesCountTotal = new NamedArray("总请求数");
        for (AccessLogStatisticsWithTime item : dataList) {
            lineChartVo.axisX.add(JsonUtil.formatInstant(item.getTimestampMillis()));
            seriesCount1xx.add(item.getCount1xx());
            seriesCount2xx.add(item.getCount2xx());
            seriesCount3xx.add(item.getCount3xx());
            seriesCount4xx.add(item.getCount4xx());
            seriesCount5xx.add(item.getCount5xx());
            seriesCountError.add(item.failedCount());
            seriesCountSuccess.add(item.succeedCount());
            seriesCountTotal.add(item.totalCount());
        }

        // 下面的放置数序是会影响到前端展示顺序的，这里按照一般的关注度高低排序，不能随意改顺序
        lineChartVo.series.add(seriesCountTotal);
        lineChartVo.series.add(seriesCountSuccess);
        lineChartVo.series.add(seriesCountError);
        lineChartVo.series.add(seriesCount2xx);
        lineChartVo.series.add(seriesCount5xx);
        lineChartVo.series.add(seriesCount4xx);
        lineChartVo.series.add(seriesCount3xx);
        lineChartVo.series.add(seriesCount1xx);

        return lineChartVo;
    }

    public static LineChartVo apiCallsDelayTrend(List<AccessLogStatisticsWithTime> dataList) {
        LineChartVo lineChartVo = new LineChartVo();
        lineChartVo.setTitle("请求平均耗时趋势");
        lineChartVo.axisX.setName("时间");
        lineChartVo.axisY.setName("耗时");
        NamedArray seriesAvgDelay = new NamedArray("耗时");
        for (AccessLogStatisticsWithTime item : dataList) {
            lineChartVo.axisX.add(JsonUtil.formatInstant(item.getTimestampMillis()));
            seriesAvgDelay.add(item.avgMillisCost());
        }
        lineChartVo.series.add(seriesAvgDelay);
        return lineChartVo;
    }

    public static LineChartVo apiCallsFlowTrend(List<AccessLogStatisticsWithTime> dataList) {
        LineChartVo lineChartVo = new LineChartVo();
        lineChartVo.setTitle("流量趋势");
        lineChartVo.axisX.setName("时间");
        lineChartVo.axisY.setName("流量");
        NamedArray seriesUpFlow = new NamedArray("上行流量");
        NamedArray seriesDownFlow = new NamedArray("下行流量");
        for (AccessLogStatisticsWithTime item : dataList) {
            lineChartVo.axisX.add(JsonUtil.formatInstant(item.getTimestampMillis()));
            seriesUpFlow.add(item.getUpFlow());
            seriesDownFlow.add(item.getDownFlow());
        }

        lineChartVo.series.add(seriesUpFlow);
        lineChartVo.series.add(seriesDownFlow);

        return lineChartVo;
    }

}