package com.navercorp.pinpoint.web.applicationmap.config;


import com.navercorp.pinpoint.common.hbase.RowMapper;
import com.navercorp.pinpoint.loader.service.ServiceTypeRegistryService;
import com.navercorp.pinpoint.web.applicationmap.dao.mapper.InLinkMapper;
import com.navercorp.pinpoint.web.applicationmap.dao.mapper.LinkFilter;
import com.navercorp.pinpoint.web.applicationmap.dao.mapper.OutLinkMapper;
import com.navercorp.pinpoint.web.applicationmap.dao.mapper.ResponseTimeMapper;
import com.navercorp.pinpoint.web.applicationmap.dao.mapper.RowMapperFactory;
import com.navercorp.pinpoint.web.applicationmap.rawdata.LinkDataMap;
import com.navercorp.pinpoint.web.component.ApplicationFactory;
import com.navercorp.pinpoint.web.vo.ResponseTime;
import com.sematext.hbase.wd.RowKeyDistributorByHashPrefix;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapMapperConfiguration {

    @Bean
    public RowMapperFactory<LinkDataMap> mapOutLinkMapper(ApplicationFactory applicationFactory,
                                                          @Qualifier("mapOutLinkRowKeyDistributor")
                                         RowKeyDistributorByHashPrefix rowKeyDistributor) {
        return (windowFunction) -> new OutLinkMapper(applicationFactory, rowKeyDistributor, LinkFilter::skip, windowFunction);
    }

    @Bean
    public RowMapperFactory<LinkDataMap> mapInLinkMapper(ServiceTypeRegistryService registry,
                                                         ApplicationFactory applicationFactory,
                                                         @Qualifier("mapInLinkRowKeyDistributor")
                                                         RowKeyDistributorByHashPrefix rowKeyDistributor) {
        return (windowFunction) -> new InLinkMapper(registry, applicationFactory, rowKeyDistributor, LinkFilter::skip, windowFunction);
    }

    @Bean
    public RowMapper<ResponseTime> responseTimeMapper(ServiceTypeRegistryService registry,
                                                      @Qualifier("mapSelfRowKeyDistributor")
                                                      RowKeyDistributorByHashPrefix rowKeyDistributor) {
        return new ResponseTimeMapper(registry, rowKeyDistributor);
    }
}
