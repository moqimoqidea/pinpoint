/*
 * Copyright 2024 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.metric.common.model.chart;

import com.navercorp.pinpoint.common.timeseries.point.Point;

/**
 * @author minwoo-jung
 */
public class MinMaxMetricPoint implements Point {

    private final long timestamp;

    private final double minValue;

    private final double maxValue;

    public MinMaxMetricPoint(long timestamp, double minValue, double maxValue) {
        this.timestamp = timestamp;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        MinMaxMetricPoint that = (MinMaxMetricPoint) o;
        return timestamp == that.timestamp && Double.compare(minValue, that.minValue) == 0 && Double.compare(maxValue, that.maxValue) == 0;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(timestamp);
        result = 31 * result + Double.hashCode(minValue);
        result = 31 * result + Double.hashCode(maxValue);
        return result;
    }

    @Override
    public String toString() {
        return "MinMaxMetricPoint{" +
                "timestamp=" + timestamp +
                ", minValue=" + minValue +
                ", maxValue=" + maxValue +
                '}';
    }

}
