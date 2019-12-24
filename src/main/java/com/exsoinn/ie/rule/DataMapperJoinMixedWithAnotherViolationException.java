package com.exsoinn.ie.rule;

import java.util.stream.Collectors;

/**
 * Created by QuijadaJ on 9/19/2017.
 */
public class DataMapperJoinMixedWithAnotherViolationException extends RuleException {
    DataMapperJoinMixedWithAnotherViolationException(QueryableDataRule pQryDataComponent) {
        super("This data source component joins with a data mapper and one or more other sources. When" +
                "a data source joins with a data mapper, other joins are not allowed. Component"
                + " in question is '" + pQryDataComponent.name() + "'. Data source name is "
                + pQryDataComponent.dataSourceName() + ". The data sources joined with are "
                + pQryDataComponent.getDataSourceJoinInformation().stream()
                .map(e -> "'" + e.getTargetDataSource().dataSourceName() + "'").collect(Collectors.joining(", ")));
    }
}
