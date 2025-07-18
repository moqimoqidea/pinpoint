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

package com.navercorp.pinpoint.web.mapper;

import com.navercorp.pinpoint.common.buffer.Buffer;
import com.navercorp.pinpoint.common.buffer.OffsetFixedBuffer;
import com.navercorp.pinpoint.common.hbase.HbaseTables;
import com.navercorp.pinpoint.common.hbase.RowMapper;
import com.navercorp.pinpoint.common.hbase.RowTypeHint;
import com.navercorp.pinpoint.common.profiler.util.TransactionId;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author emeroad
 * @author netspider
 */
@Component
public class TransactionIdMapper implements RowMapper<List<TransactionId>>, RowTypeHint {

    private final Logger logger = LogManager.getLogger(this.getClass());

    @Override
    public List<TransactionId> mapRow(Result result, int rowNum) throws Exception {
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        Cell[] rawCells = result.rawCells();
        List<TransactionId> traceIdList = new ArrayList<>(rawCells.length);
        for (Cell cell : rawCells) {
            if (CellUtil.matchingFamily(cell, HbaseTables.APPLICATION_TRACE_INDEX_TRACE.getName())) {
                final byte[] qualifierArray = cell.getQualifierArray();
                final int qualifierOffset = cell.getQualifierOffset();
                final int qualifierLength = cell.getQualifierLength();
                // increment by value of key
                TransactionId traceId = parseVarTransactionId(qualifierArray, qualifierOffset, qualifierLength);
                traceIdList.add(traceId);

                logger.debug("found traceId {}", traceId);
            }
        }
        return traceIdList;
    }

    public static TransactionId parseVarTransactionId(byte[] bytes, int offset, int length) {
        Objects.requireNonNull(bytes, "bytes");

        final Buffer buffer = new OffsetFixedBuffer(bytes, offset, length);
        
        // skip elapsed time (not used) hbase column prefix - only used for filtering.
        // Not sure if we can reduce the data size any further.
        // buffer.readInt();
        
        String agentId = buffer.readPrefixedString();
        long agentStartTime = buffer.readSVLong();
        long transactionSequence = buffer.readVLong();
        return TransactionId.of(agentId, agentStartTime, transactionSequence);
    }

    @Override
    public Class<?> rowType() {
        return List.class;
    }
}
