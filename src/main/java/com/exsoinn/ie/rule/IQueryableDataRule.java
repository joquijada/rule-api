package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Context;

import java.util.List;

/**
 * This interface was created so that it can be implemented by {@link QueryableDataRule}, so that certain methods
 * (mostly getters that don't allow mutation of the object) have public visibility, allowing client code to call them.
 *
 * Created by QuijadaJ on 5/11/2017.
 */
public interface IQueryableDataRule extends Rule {

    Context context();

    boolean dataSourceIsMapper() throws RuleException;

    List<DataSourceJoinInformation> getDataSourceJoinInformation();

    String dataSourceName();

    int rowCount() throws RuleException;
}
