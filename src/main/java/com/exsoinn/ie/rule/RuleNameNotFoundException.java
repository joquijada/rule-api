package com.exsoinn.ie.rule;

/**
 * Created by QuijadaJ on 5/9/2017.
 */
public class RuleNameNotFoundException extends RuleException {
    public RuleNameNotFoundException(String pRuleName, Throwable t) {
        super("Could not find rule for name provided: " + pRuleName, t);
    }

    public RuleNameNotFoundException(String pRuleName) {
        this(pRuleName, null);
    }
}
