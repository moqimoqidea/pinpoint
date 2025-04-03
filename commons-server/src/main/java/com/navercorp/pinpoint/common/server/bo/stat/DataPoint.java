/*
 * Copyright 2016 Naver Corp.
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

package com.navercorp.pinpoint.common.server.bo.stat;

import com.navercorp.pinpoint.common.server.timeseries.Point;

/**
 * @author HyunGil Jeong
 */
public interface DataPoint extends Point {
    String getAgentId();

    String getApplicationName();

    long getStartTimestamp();

    @Override
    long getTimestamp();

    static DataPoint of(String agentId, String applicationName, long startTimestamp, long timestamp) {
        return new DefaultDataPoint(agentId, applicationName, startTimestamp, timestamp);
    }
}
