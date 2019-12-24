package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Context;

/**
 * Created by QuijadaJ on 6/13/2017.
 */
public class RowIsNotComplexException extends RuleException {
    RowIsNotComplexException(String pMsg, Throwable t) {
        super(pMsg, t);
    }

    RowIsNotComplexException(Context pRow) {
        this("Found a row which is not a complex object. Check and try again: "
                + pRow.stringRepresentation(), null);
    }
}
