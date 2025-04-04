package com.navercorp.pinpoint.common.server.uid.mapper;

import com.navercorp.pinpoint.common.hbase.HbaseColumnFamily;
import com.navercorp.pinpoint.common.hbase.HbaseTables;
import com.navercorp.pinpoint.common.hbase.RowMapper;
import com.navercorp.pinpoint.common.hbase.util.CellUtils;
import com.navercorp.pinpoint.common.server.uid.ServiceUid;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Result;
import org.springframework.stereotype.Component;

@Component
public class ServiceUidMapper implements RowMapper<ServiceUid> {

    private static final HbaseColumnFamily DESCRIPTOR = HbaseTables.SERVICE_UID;

    @Override
    public ServiceUid mapRow(Result result, int rowNum) throws Exception {
        if (result.isEmpty()) {
            return null;
        }

        Cell cell = result.getColumnLatestCell(DESCRIPTOR.getName(), DESCRIPTOR.getName());
        int serviceUid = CellUtils.valueToInt(cell);
        return ServiceUid.of(serviceUid);
    }
}
