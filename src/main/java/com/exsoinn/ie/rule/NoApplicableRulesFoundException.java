package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Context;

/**
 * Created by QuijadaJ on 6/27/2017.
 */
public class NoApplicableRulesFoundException extends RuleException {
    NoApplicableRulesFoundException(String pMsg, Throwable t) {
        super(pMsg, t);
    }

    NoApplicableRulesFoundException(Context pContext) {
        this("Did not find any rules to execute for the given context. Check if this is a problem in "
                + " rule configurations: " + pContext.stringRepresentation(), null);
    }
}
