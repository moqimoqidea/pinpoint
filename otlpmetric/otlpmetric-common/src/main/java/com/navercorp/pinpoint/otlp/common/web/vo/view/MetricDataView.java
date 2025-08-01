/*
 * Copyright 2025 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.otlp.common.web.vo.view;

import com.navercorp.pinpoint.otlp.common.web.definition.property.ChartType;
import com.navercorp.pinpoint.otlp.common.web.vo.MetricData;
import com.navercorp.pinpoint.otlp.common.web.vo.MetricValue;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author minwoo-jung
 */
public class MetricDataView {
    private final List<Long> timestamp;
    private final ChartType chartType;
    private final String unit;
    private final List<MetricValue> metricValueList;
    private final String message;

    public MetricDataView(MetricData metricData) {
        this(metricData.getTimestampList(), metricData.getChartType(), metricData.getUnit(), metricData.getMetricValueList(), metricData.getMessage());
    }

    public MetricDataView(List<Long> timestamp, ChartType chartType, String unit, List<MetricValue> metricValueList, String message) {
        this.timestamp = timestamp;
        this.chartType = chartType;
        this.unit = unit;
        this.metricValueList = metricValueList;
        this.message = message;
    }

    public List<Long> getTimestamp() {
        return timestamp;
    }

    public String getChartType() {
        return chartType.getChartName();
    }

    public String getUnit() {
        return unit;
    }

    public List<MetricValueView> getMetricValues() {
        return metricValueList.stream().map(MetricValueView::new).collect(Collectors.toList());
    }

    public String getMessage() {
        return message;
    }

    public static class MetricValueView {
        private final String legendName;
        private final List<Number> valueList;
        private final String version;

        public MetricValueView(MetricValue metricValue) {
            this.legendName = metricValue.legendName();
            this.valueList = metricValue.valueList();
            this.version = metricValue.version();
        }

        public String getLegendName() {
            return legendName;
        }

        public List<Number> getValues() {
            return valueList;
        }

        public String getVersion() {
            return version;
        }
    }
}
