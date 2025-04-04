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

package com.navercorp.pinpoint.web.heatmap.dao.pinot;

import com.navercorp.pinpoint.common.server.metric.dao.TableNameManager;
import com.navercorp.pinpoint.web.heatmap.dao.HeatmapChartDao;
import com.navercorp.pinpoint.web.heatmap.vo.HeatmapCell;
import com.navercorp.pinpoint.web.heatmap.vo.HeatmapSearchKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * @author minwoo-jung
 */
@Repository
public class PinotHeatmapChartDao implements HeatmapChartDao {

    private final Logger logger = LogManager.getLogger(this.getClass());
    private static final String NAMESPACE = PinotHeatmapChartDao.class.getName() + ".";

    private final SqlSessionTemplate syncTemplate;
//    private final TableNameManager tableNameManager;

    public PinotHeatmapChartDao(@Qualifier("heatmapPinotTemplate") SqlSessionTemplate syncTemplate) {
        this.syncTemplate = Objects.requireNonNull(syncTemplate, "syncTemplate");
        // TODO : (minwoo) add tablenamemanager .
//        this.tableNameManager = new TableNameManager(inspectorWebProperties.getAgentStatTablePrefix(), inspectorWebProperties.getAgentStatTablePaddingLength(), inspectorWebProperties.getAgentStatTableCount());
    }

    @Override
    public List<HeatmapCell> getHeatmapAppData(HeatmapSearchKey heatmapSearchKey) {
        return syncTemplate.selectList(NAMESPACE + "selectHeatmapApp", heatmapSearchKey);
    }
}
