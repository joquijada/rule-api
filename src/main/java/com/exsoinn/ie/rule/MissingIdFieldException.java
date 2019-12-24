package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Context;

/**
 * Created by QuijadaJ on 6/13/2017.
 */
public class MissingIdFieldException extends RuleException {
    MissingIdFieldException(String pMsg, Throwable t) {
        super(pMsg, t);
    }

    MissingIdFieldException(Context pRow) {
        this("Not all rows contain field 'id': " + pRow.stringRepresentation(), null);
    }
}
