package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Context;

/**
 * Created by QuijadaJ on 6/13/2017.
 */
public class TopLevelElementValueIsNotArrayException extends RuleException {
    TopLevelElementValueIsNotArrayException(String pMsg, Throwable t) {
        super(pMsg, t);
    }

    TopLevelElementValueIsNotArrayException(Context pContext) {
        this("The value of the outer most element must be and array, " +
                "check configuration and try again: " + pContext.stringRepresentation(), null);
    }
}
