package com.exsoinn.ie.rule;


/**
 * Created by QuijadaJ on 6/15/2017.
 */
public class DuplicateDataSourceNameException extends RuleException {
    DuplicateDataSourceNameException(String pMsg, Throwable t) {
        super(pMsg, t);
    }

    DuplicateDataSourceNameException(QueryableDataRule pRuleFirst, QueryableDataRule pRuleLast) {
        this("Rule object " + pRuleFirst.name() + " already defines a data source with a name '"
                + pRuleLast.dataSourceName() + "', and " + pRuleLast.name()
                +  " is trying to use it. Fix the configuration and try again. Contexts in question are (original) "
                + pRuleFirst.context().stringRepresentation() + ", and (last) "
                + pRuleLast.context().stringRepresentation(), null);
    }
}
