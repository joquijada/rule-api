package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Context;

/**
 * Created by QuijadaJ on 6/13/2017.
 */
public class DuplicateRowIdException extends RuleException {
    DuplicateRowIdException(String pMsg, Throwable t) {
        super(pMsg, t);
    }

    DuplicateRowIdException(QueryableDataRule pRuleObj, Context pRow) {
        this("Not all 'id' field values are unique for component '" + pRuleObj.name() + "', data source '"
                + pRuleObj.dataSourceName() + "'. Check this row which had an ID already seen, and try again: "
                + pRow.stringRepresentation(), null);
    }
}
