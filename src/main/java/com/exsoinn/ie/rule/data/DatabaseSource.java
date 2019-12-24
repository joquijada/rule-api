package com.exsoinn.ie.rule.data;

import com.exsoinn.util.EscapeUtil;
import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.ContextFactory;
import com.exsoinn.util.epf.MutableContext;
import org.apache.tomcat.jdbc.pool.DataSource;
import java.sql.*;
import java.util.List;


/**
 * Created by QuijadaJ on 11/10/2017.
 */
public class DatabaseSource implements Source<SqlQuery, Context> {
    private final String name;
    private final DataSource connectionPool;


    public static class Builder implements com.exsoinn.ie.util.Builder<DatabaseSource> {
        // Required parameters
        private final String name;
        private final DataSource connectionPool;

        // Optional parameters


        public Builder(String pName, DataSource pConnPool) {
            name = pName;
            connectionPool = pConnPool;
        }

        @Override
        public DatabaseSource build() {
            return new DatabaseSource(this);
        }
    }


    private DatabaseSource(Builder pBuilder) {
        name = pBuilder.name;
        connectionPool = pBuilder.connectionPool;
    }

    @Override
    public Context retrieveData(SqlQuery pQry) throws SourceException {
        Connection conn = null;
        try {
            conn = connectionPool.getConnection();
            PreparedStatement stmt = conn.prepareStatement(pQry.getQuery());
            ParameterMetaData metadata = stmt.getParameterMetaData();
            List<String> params = pQry.getParameters();
            int actualParamCnt = params.size();
            int expectedParamCnt = metadata.getParameterCount();
            if (actualParamCnt != metadata.getParameterCount()) {
                throw new SourceException("The number of passed in query parameters is not the same as the"
                        + " number of accepted parameters by this query. Actual parameters is "
                        + actualParamCnt + " versus " + expectedParamCnt + ". Query is " + pQry.getQuery());
            }

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject((i + 1), params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData resMetadata = rs.getMetaData();
            int totalColumns = resMetadata.getColumnCount();
            MutableContext resultAry = ContextFactory.obtainMutableContext("[]");
            while (rs.next()) {
                MutableContext res = ContextFactory.obtainMutableContext("{}");
                for (int i = 1; i <= totalColumns; i++) {
                    String colName = resMetadata.getColumnName(i);
                    Object value = rs.getObject(colName);
                    // TODO: Should we camel-case column names?
                    String valueAsStr = null == value ? "" : value.toString();

                    /**
                     * Translate the column name if the caller so quested.
                     */
                    if (null != pQry.getColumnTranslator() && pQry.getColumnTranslator().containsKey(colName)) {
                        colName = pQry.getColumnTranslator().get(colName);
                    }

                    res.addMember(colName, ContextFactory.obtainContext(EscapeUtil.escapeSpecialCharacters(valueAsStr)));
                }
                resultAry.addEntryToArray(res);
            }

            if (null != stmt) {
                stmt.close();
            }

            if (null != rs) {
                rs.close();
            }
            return resultAry;
        } catch (Exception e) {
            throw new SourceException(e);
        } finally {
            if (null != conn) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new SourceException(e);
                }
            }
        }
    }

    @Override
    public Context insertData(SqlQuery pQry) {
        throw new UnsupportedOperationException("SQL insert/update operations not supported yet.");
    }


    @Override
    public String name() {
        return this.name;
    }
}
