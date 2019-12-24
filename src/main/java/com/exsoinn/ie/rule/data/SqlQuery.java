package com.exsoinn.ie.rule.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by QuijadaJ on 11/10/2017.
 */
public class SqlQuery implements Query {
    private final String query;
    private final List<String> parameters;
    private final Map<String, String> columnTranslator;


    public static class Builder implements com.exsoinn.ie.util.Builder<SqlQuery> {
        // Required parameters
        private final String query;

        // Optional parameters
        private List<String> parameters = null;
        private Map<String, String> columnTranslator = null;

        public Builder parameters(List<String> pParams) {
            if (null != pParams) {
                parameters = new ArrayList<>(pParams);
            }
            return this;
        }

        public Builder columnTranslator(Map<String, String> pMap) {
            if (null != pMap) {
                columnTranslator = new HashMap<>(pMap);
            }
            return this;
        }

        public Builder(String pQry) {
            query = pQry;
        }


        @Override
        public SqlQuery build() {
            return new SqlQuery(this);
        }
    }

    SqlQuery(Builder pBuilder) {
        query = pBuilder.query;
        parameters = pBuilder.parameters;
        columnTranslator = pBuilder.columnTranslator;
    }


    /*
     * Getters
     */
    public String getQuery() {
        return query;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public Map<String, String> getColumnTranslator() {
        return columnTranslator;
    }
}
