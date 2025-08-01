/*
 * Copyright 2019 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.collector.handler.grpc;

import com.navercorp.pinpoint.collector.mapper.grpc.event.GrpcAgentEventBatchMapper;
import com.navercorp.pinpoint.collector.mapper.grpc.event.GrpcAgentEventMapper;
import com.navercorp.pinpoint.collector.service.AgentEventService;
import com.navercorp.pinpoint.common.server.bo.event.AgentEventBo;
import com.navercorp.pinpoint.common.server.bo.event.DeadlockEventBo;
import com.navercorp.pinpoint.common.server.util.AgentEventMessageSerializerV1;
import com.navercorp.pinpoint.common.util.CollectionUtils;
import com.navercorp.pinpoint.grpc.MessageFormatUtils;
import com.navercorp.pinpoint.grpc.trace.PAgentStat;
import com.navercorp.pinpoint.grpc.trace.PAgentStatBatch;
import com.navercorp.pinpoint.io.request.ServerHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author Taejin Koo
 * @author jaehong.kim - Add AgentEventMessageSerializerV1
 */
@Service
public class GrpcAgentEventService {
    private final Logger logger = LogManager.getLogger(this.getClass());

    private final GrpcAgentEventMapper agentEventMapper;

    private final GrpcAgentEventBatchMapper agentEventBatchMapper;

    private final AgentEventMessageSerializerV1 agentEventMessageSerializerV1;

    private final AgentEventService agentEventService;

    public GrpcAgentEventService(GrpcAgentEventMapper agentEventMapper,
                                 GrpcAgentEventBatchMapper agentEventBatchMapper,
                                 AgentEventMessageSerializerV1 agentEventMessageSerializerV1,
                                 AgentEventService agentEventService) {
        this.agentEventMapper = Objects.requireNonNull(agentEventMapper, "agentEventMapper");
        this.agentEventBatchMapper = Objects.requireNonNull(agentEventBatchMapper, "agentEventBatchMapper");
        this.agentEventMessageSerializerV1 = Objects.requireNonNull(agentEventMessageSerializerV1, "agentEventMessageSerializerV1");
        this.agentEventService = Objects.requireNonNull(agentEventService, "agentEventService");
    }


    public void handleAgentStat(ServerHeader header, PAgentStat agentStat) {
        if (logger.isDebugEnabled()) {
            logger.debug("Handle PAgentStat={}", MessageFormatUtils.debugLog(agentStat));
        }

        final AgentEventBo agentEventBo = this.agentEventMapper.map(agentStat, header);
        if (agentEventBo == null) {
            return;
        }

        try {
            insert(agentEventBo);
        } catch (Exception e) {
            logger.warn("Failed to handle agentStat={}", MessageFormatUtils.debugLog(agentStat), e);
        }
    }

    public void handleAgentStatBatch(ServerHeader header, PAgentStatBatch agentStatBatch) {
        if (logger.isDebugEnabled()) {
            logger.debug("Handle PAgentStatBatch={}", MessageFormatUtils.debugLog(agentStatBatch));
        }

        final List<AgentEventBo> agentEventBoList = this.agentEventBatchMapper.map(agentStatBatch, header);
        if (CollectionUtils.isEmpty(agentEventBoList)) {
            return;
        }

        try {
            for (AgentEventBo agentEventBo : agentEventBoList) {
                insert(agentEventBo);
            }
        } catch (Exception e) {
            logger.warn("Failed to handle agentStatBatch={}", MessageFormatUtils.debugLog(agentStatBatch), e);
        }
    }

    private void insert(final AgentEventBo agentEventBo) {
        final Object eventMessage = getEventMessage(agentEventBo);
        final byte[] eventBody = agentEventMessageSerializerV1.serialize(agentEventBo.getEventType(), eventMessage);
        agentEventBo.setEventBody(eventBody);
        this.agentEventService.insert(agentEventBo);
    }

    private Object getEventMessage(AgentEventBo agentEventBo) {
        if (agentEventBo instanceof DeadlockEventBo deadlockEventBo) {
            return deadlockEventBo.getDeadlockBo();
        }
        throw new IllegalArgumentException("unsupported message " + agentEventBo);
    }
}