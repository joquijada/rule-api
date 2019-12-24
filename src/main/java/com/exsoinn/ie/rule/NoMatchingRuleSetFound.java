package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Context;

/**
 * Created by QuijadaJ on 7/12/2017.
 */
public class NoMatchingRuleSetFound extends RuleException {
    NoMatchingRuleSetFound(String pMsg, Throwable t) {
        super(pMsg, t);
    }


    NoMatchingRuleSetFound(Context pContext, Rule pRuleSet) {
        this("A matching rule set was not found for the provided input data: " + pContext.stringRepresentation()
                + ". Rule set name is " + pRuleSet, null);
    }

}
