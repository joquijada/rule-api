package com.exsoinn.ie.rule;

/**
 * Created by QuijadaJ on 4/19/2017.
 */
public class RuleException extends Exception {
    RuleException(String pMsg, Throwable t) {
        super(pMsg, t);
    }

    RuleException(String pMsg) {
        this(pMsg, null);
    }

    RuleException(Throwable t) {
        super(t);
    }
}
