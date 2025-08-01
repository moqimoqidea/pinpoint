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

package com.navercorp.pinpoint.collector.applicationmap.dao.hbase;

import com.navercorp.pinpoint.collector.applicationmap.Vertex;
import com.navercorp.pinpoint.collector.applicationmap.dao.HostApplicationMapDao;
import com.navercorp.pinpoint.collector.util.AtomicLongUpdateMap;
import com.navercorp.pinpoint.common.annotations.VisibleForTesting;
import com.navercorp.pinpoint.common.buffer.AutomaticBuffer;
import com.navercorp.pinpoint.common.buffer.Buffer;
import com.navercorp.pinpoint.common.buffer.ByteArrayUtils;
import com.navercorp.pinpoint.common.hbase.HbaseColumnFamily;
import com.navercorp.pinpoint.common.hbase.HbaseOperations;
import com.navercorp.pinpoint.common.hbase.HbaseTableConstants;
import com.navercorp.pinpoint.common.hbase.HbaseTables;
import com.navercorp.pinpoint.common.hbase.TableNameProvider;
import com.navercorp.pinpoint.common.hbase.util.Puts;
import com.navercorp.pinpoint.common.hbase.wd.ByteHasher;
import com.navercorp.pinpoint.common.hbase.wd.RowKeyDistributor;
import com.navercorp.pinpoint.common.timeseries.window.TimeSlot;
import com.navercorp.pinpoint.common.util.BytesUtils;
import com.navercorp.pinpoint.common.util.TimeUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Put;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Objects;

/**
 * @author netspider
 * @author emeroad
 */
@Repository
public class HbaseHostApplicationMapDao implements HostApplicationMapDao {

    private final Logger logger = LogManager.getLogger(this.getClass());
    private static final HbaseColumnFamily DESCRIPTOR = HbaseTables.HOST_APPLICATION_MAP_VER2_MAP;

    private final HbaseOperations hbaseTemplate;

    private final TableNameProvider tableNameProvider;

    private final TimeSlot timeSlot;

    private final ByteHasher hasher;

    // FIXME should modify to save a cachekey at each 30~50 seconds instead of saving at each time
    private final AtomicLongUpdateMap<CacheKey> updater = new AtomicLongUpdateMap<>();


    public HbaseHostApplicationMapDao(HbaseOperations hbaseTemplate,
                                      TableNameProvider tableNameProvider,
                                      @Qualifier("acceptApplicationRowKeyDistributor") RowKeyDistributor rowKeyDistributor,
                                      TimeSlot timeSlot) {
        this.hbaseTemplate = Objects.requireNonNull(hbaseTemplate, "hbaseTemplate");
        this.tableNameProvider = Objects.requireNonNull(tableNameProvider, "tableNameProvider");
        Objects.requireNonNull(rowKeyDistributor, "rowKeyDistributor");
        this.hasher = rowKeyDistributor.getByteHasher();
        this.timeSlot = Objects.requireNonNull(timeSlot, "timeSlot");
    }


    @Override
    public void insert(long requestTime, String host, Vertex selfVertex,
            String parentApplicationName, short parentServiceType) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(selfVertex, "selfVertex");
        if (logger.isDebugEnabled()) {
            logger.debug("insert HostApplicationMap host:{}, self:{} parent:{}/{}", host, selfVertex, parentApplicationName, parentServiceType);
        }

        final long statisticsRowSlot = timeSlot.getTimeSlot(requestTime);

        final CacheKey cacheKey = new CacheKey(host, selfVertex.applicationName(), selfVertex.serviceType().getCode(), parentApplicationName, parentServiceType);
        final boolean needUpdate = updater.update(cacheKey, statisticsRowSlot);
        if (needUpdate) {
            insertHostVer2(host, selfVertex, statisticsRowSlot, parentApplicationName, parentServiceType);
        }
    }

    private void insertHostVer2(String host, Vertex selfVertex, long statisticsRowSlot, String parentApplicationName, short parentServiceType) {
        if (logger.isDebugEnabled()) {
            logger.debug("Insert HostApplicationMap Ver2 host={}, self={}, parent={}/{}",
                    host, selfVertex, parentApplicationName, parentServiceType);
        }

        // TODO should consider to add bellow codes again later.
        //String parentAgentId = null;
        //final byte[] rowKey = createRowKey(parentApplicationName, parentServiceType, statisticsRowSlot, parentAgentId);
        final byte[] rowKey = createRowKey(parentApplicationName, parentServiceType, statisticsRowSlot, null);

        byte[] columnName = createColumnName(host, selfVertex);

        TableName hostApplicationMapTableName = tableNameProvider.getTableName(DESCRIPTOR.getTable());

        Put put = Puts.put(rowKey, DESCRIPTOR.getName(), columnName, null);
        this.hbaseTemplate.put(hostApplicationMapTableName, put);

    }

    private byte[] createColumnName(String host, Vertex self) {
        Buffer buffer = new AutomaticBuffer(64);
        buffer.putPrefixedString(host);
        buffer.putPrefixedString(self.applicationName());
        buffer.putShort(self.serviceType().getCode());
        return buffer.getBuffer();
    }


    private byte[] createRowKey(String parentApplicationName, short parentServiceType, long statisticsRowSlot, String parentAgentId) {
        final byte[] rowKey = createRowKey0(hasher.getSaltKey().size(), parentApplicationName, parentServiceType, statisticsRowSlot, parentAgentId);
        return hasher.writeSaltKey(rowKey);
    }


    @VisibleForTesting
    static byte[] createRowKey0(int saltKeySize, String parentApplicationName, short parentServiceType, long statisticsRowSlot, String parentAgentId) {

        // even if  a agentId be added for additional specifications, it may be safe to scan rows.
        // But is it needed to add parentAgentServiceType?
        int offset = saltKeySize + HbaseTableConstants.APPLICATION_NAME_MAX_LEN;
        final int SIZE = offset + BytesUtils.SHORT_BYTE_LENGTH + BytesUtils.LONG_BYTE_LENGTH;

        byte[] rowKey = new byte[SIZE];

        final byte[] parentAppNameBytes = BytesUtils.toBytes(parentApplicationName);
        if (parentAppNameBytes.length > HbaseTableConstants.APPLICATION_NAME_MAX_LEN) {
            throw new IllegalArgumentException("Parent application name length exceed " + parentApplicationName);
        }
        BytesUtils.writeBytes(rowKey, saltKeySize, parentAppNameBytes);
        offset = ByteArrayUtils.writeShort(parentServiceType, rowKey, offset);
        long timestamp = TimeUtils.reverseTimeMillis(statisticsRowSlot);
        ByteArrayUtils.writeLong(timestamp, rowKey, offset);
        return rowKey;
    }

    private static final class CacheKey {
        private final String host;
        private final String applicationName;
        private final short serviceType;

        private final String parentApplicationName;
        private final short parentServiceType;

        public CacheKey(String host, String applicationName, short serviceType, String parentApplicationName, short parentServiceType) {
            this.host = Objects.requireNonNull(host, "host");
            this.applicationName = Objects.requireNonNull(applicationName, "applicationName");
            this.serviceType = serviceType;

            // may be null for below two parent values.
            this.parentApplicationName = parentApplicationName;
            this.parentServiceType = parentServiceType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (parentServiceType != cacheKey.parentServiceType) return false;
            if (serviceType != cacheKey.serviceType) return false;
            if (!applicationName.equals(cacheKey.applicationName)) return false;
            if (!host.equals(cacheKey.host)) return false;
            if (parentApplicationName != null ? !parentApplicationName.equals(cacheKey.parentApplicationName) : cacheKey.parentApplicationName != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = host.hashCode();
            result = 31 * result + applicationName.hashCode();
            result = 31 * result + (int) serviceType;
            result = 31 * result + (parentApplicationName != null ? parentApplicationName.hashCode() : 0);
            result = 31 * result + (int) parentServiceType;
            return result;
        }
    }
}
