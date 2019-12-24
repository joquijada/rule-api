package com.exsoinn.ie.rule;

/**
 * Created by QuijadaJ on 6/5/2017.
 */
class DataSourceJoinLimitExceededException extends RuleException {
    public DataSourceJoinLimitExceededException(QueryableDataRule pQryData, Throwable t) {
        super("At this time only one join is supported for non-mapper data sources, "
                + "and exactly two for mapper data sources. Found " + pQryData.getDataSourceJoinInformation().size()
                + " joins for data source '" + pQryData.name() + "'. Fix the Context configuration and try again: "
                +  pQryData.context(), t);
    }

    public DataSourceJoinLimitExceededException(QueryableDataRule pQryData) {
        this(pQryData, null);
    }
}
