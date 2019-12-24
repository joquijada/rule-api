package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Context;

/**
 *
 * Created by QuijadaJ on 6/13/2017.
 */
public class NotAllRowFieldValuesArePrimitiveOrArray extends RuleException {
    NotAllRowFieldValuesArePrimitiveOrArray(String pMsg, Throwable t) {
        super(pMsg, t);
    }

    NotAllRowFieldValuesArePrimitiveOrArray(Context pRow) {
        this("Found a field in a row which is not a primitive or array. Check and try again: "
                + pRow.stringRepresentation(), null);
    }

}
