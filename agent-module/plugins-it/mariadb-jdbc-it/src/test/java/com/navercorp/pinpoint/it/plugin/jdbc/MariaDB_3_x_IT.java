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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.it.plugin.jdbc;

import com.navercorp.pinpoint.bootstrap.plugin.test.PluginTestVerifier;
import com.navercorp.pinpoint.bootstrap.plugin.test.PluginTestVerifierHolder;
import com.navercorp.pinpoint.it.plugin.utils.AgentPath;
import com.navercorp.pinpoint.it.plugin.utils.TestcontainersOption;
import com.navercorp.pinpoint.it.plugin.utils.jdbc.DefaultJDBCApi;
import com.navercorp.pinpoint.it.plugin.utils.jdbc.JDBCApi;
import com.navercorp.pinpoint.it.plugin.utils.jdbc.JDBCDriverClass;
import com.navercorp.pinpoint.it.plugin.utils.jdbc.JDBCTestConstants;
import com.navercorp.pinpoint.test.plugin.Dependency;
import com.navercorp.pinpoint.test.plugin.PinpointAgent;
import com.navercorp.pinpoint.test.plugin.PinpointLogLocationConfig;
import com.navercorp.pinpoint.test.plugin.PluginTest;
import com.navercorp.pinpoint.test.plugin.shared.SharedDependency;
import com.navercorp.pinpoint.test.plugin.shared.SharedTestLifeCycleClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.args;
import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.cachedArgs;
import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.event;
import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.sql;

@PluginTest
@PinpointAgent(AgentPath.PATH)
@PinpointLogLocationConfig(".")
// Skip 3.0.10, 3.1.1 - ConcurrentModificationException bug
@Dependency({"org.mariadb.jdbc:mariadb-java-client:[3.0,3.0.9],[3.1.2,]",
        JDBCTestConstants.VERSION})
@SharedDependency({"org.mariadb.jdbc:mariadb-java-client:1.3.0", JDBCTestConstants.VERSION, TestcontainersOption.TEST_CONTAINER, TestcontainersOption.MARIADB})
@SharedTestLifeCycleClass(MariaDBServer.class)
public class MariaDB_3_x_IT extends MariaDB_IT_Base {
    // see CallableParameterMetaData#queryMetaInfos
    private static final String CALLABLE_QUERY_META_INFOS_QUERY = "SELECT * from information_schema.PARAMETERS WHERE SPECIFIC_NAME = ? AND SPECIFIC_SCHEMA = ? ORDER BY ORDINAL_POSITION";

    private static final JDBCDriverClass driverClass = newDriverClass(PreparedStatementType.Server);
    private static final JDBCApi jdbcApi = new DefaultJDBCApi(driverClass);

    private static final JDBCDriverClass clientDriverClass = newDriverClass(PreparedStatementType.Client);
    private static final JDBCApi clientJdbcApi = new DefaultJDBCApi(clientDriverClass);

    private static JDBCDriverClass newDriverClass(PreparedStatementType server) {
        return new MariaDB_3_x_DriverClass(server);
    }

    @Override
    public JDBCDriverClass getJDBCDriverClass() {
        return driverClass;
    }

    @Test
    public void testStatement() throws Exception {
        super.executeStatement();

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();

        // Driver#connect(String, Properties)
        Method connect = jdbcApi.getDriver().getConnect();
        verifier.verifyTrace(event(DB_TYPE, connect, null, URL, DATABASE_NAME, cachedArgs(getJdbcUrl())));

        // MariaDbStatement#executeQuery(String)
        Method executeQuery = jdbcApi.getStatement().getExecuteQuery();
        verifier.verifyTrace(event(DB_EXECUTE_QUERY, executeQuery, null, URL, DATABASE_NAME, sql(STATEMENT_NORMALIZED_QUERY, "1")));
    }

    @Test
    public void testPreparedStatement() throws Exception {
        super.executePreparedStatement();
        final JDBCApi jdbcMethod = clientJdbcApi;

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();
        verifier.verifyTraceCount(3);

        // Driver#connect(String, Properties)
        Method connect = jdbcMethod.getDriver().getConnect();
        verifier.verifyTrace(event(DB_TYPE, connect, null, URL, DATABASE_NAME, cachedArgs(getJdbcUrl())));

        // MariaDbConnection#prepareStatement(String)
        Method prepareStatement = jdbcMethod.getConnection().getPrepareStatement();
        verifier.verifyTrace(event(DB_TYPE, prepareStatement, null, URL, DATABASE_NAME, sql(PREPARED_STATEMENT_QUERY, null)));

        // ClientSidePreparedStatement#executeQuery
        Method executeQuery = jdbcMethod.getPreparedStatement().getExecuteQuery();
        verifier.verifyTrace(event(DB_EXECUTE_QUERY, executeQuery, null, URL, DATABASE_NAME, sql(PREPARED_STATEMENT_QUERY, null, "3")));
    }

    @Test
    public void testCallableStatement() throws Exception {
        super.executeCallableStatement();

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();
        final int traceCount = verifier.getTraceCount();
        if (!(traceCount == 6 || traceCount == 5)) {
            Assertions.fail("traceCount=" + traceCount);
        }

        // Driver#connect(String, Properties)
        Method connect = jdbcApi.getDriver().getConnect();
        verifier.verifyTrace(event(DB_TYPE, connect, null, URL, DATABASE_NAME, cachedArgs(getJdbcUrl())));

        // MariaDbConnection#prepareCall(String)
        Method prepareCall = jdbcApi.getConnection().getPrepareCall();
        verifier.verifyTrace(event(DB_TYPE, prepareCall, null, URL, DATABASE_NAME, sql(CALLABLE_STATEMENT_QUERY, null)));

        // CallableProcedureStatement#registerOutParameter
        final JDBCApi.CallableStatementClass callableStatement = jdbcApi.getCallableStatement();
        Method registerOutParameter = callableStatement.getRegisterOutParameter();
        verifier.verifyTrace(event(DB_TYPE, registerOutParameter, null, URL, DATABASE_NAME, args(2, CALLABLE_STATMENT_OUTPUT_PARAM_TYPE)));

        // ServerSidePreparedStatement#executeQuery
        Method executeQuery = jdbcApi.getPreparedStatement().getExecuteQuery();
        verifier.verifyTrace(event(DB_EXECUTE_QUERY, executeQuery, null, URL, DATABASE_NAME, sql(CALLABLE_STATEMENT_QUERY, null, CALLABLE_STATEMENT_INPUT_PARAM)));

        Method executeQueryMethod = getMethod("org.mariadb.jdbc.ClientPreparedStatement", "executeQuery");
        verifier.verifyTrace(event(DB_EXECUTE_QUERY, executeQueryMethod, null, URL, DATABASE_NAME, sql(CALLABLE_QUERY_META_INFOS_QUERY, null, "getPlaygroundByName, test")));
    }

    private Method getMethod(String className, String methodName, Class<?>... parameterTypes) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> clazz = cl.loadClass(className);
            return clazz.getMethod(methodName, parameterTypes);
        } catch (Exception e) {
            throw new RuntimeException("reflect error", e);
        }
    }

    public String getPrepareCallBindVariable() {
        return PROCEDURE_NAME + ", " + DATABASE_NAME;
    }
}
