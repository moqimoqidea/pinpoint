/*
 * Copyright 2019 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.context.id;

import com.google.inject.Inject;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.common.profiler.util.TransactionId;
import com.navercorp.pinpoint.common.profiler.util.TransactionIdUtils;
import com.navercorp.pinpoint.profiler.context.module.AgentStartTime;
import com.navercorp.pinpoint.profiler.name.ObjectName;

import java.util.Objects;

/**
 * @author Woonduk Kang(emeroad)
 */
public class DefaultTraceIdFactory implements TraceIdFactory {

    private final ObjectName objectName;
    private final long agentStartTime;

    @Inject
    public DefaultTraceIdFactory(ObjectName objectName, @AgentStartTime long agentStartTime) {
        this.objectName = Objects.requireNonNull(objectName, "objectName");
        this.agentStartTime = agentStartTime;

    }

    @Override
    public TraceId newTraceId(long localTransactionId) {
        TransactionId transactionId = TransactionId.of(objectName.getAgentId(), agentStartTime, localTransactionId);
        return newTraceId(transactionId);
    }

    public TraceId continueTraceId(String transactionId, long parentSpanId, long spanId, short flags) {
        Objects.requireNonNull(transactionId, "transactionId");

        final TransactionId parseTxId = TransactionIdUtils.parseTransactionId(transactionId);
        return continueTraceId(parseTxId, parentSpanId, spanId, flags);
    }


    public static TraceId newTraceId(TransactionId transactionId) {
        return new DefaultTraceId(transactionId);
    }

    public static TraceId continueTraceId(TransactionId transactionId, long parentSpanId, long spanId, short flags) {
        return new DefaultTraceId(transactionId, null, parentSpanId, spanId, flags);
    }
}
