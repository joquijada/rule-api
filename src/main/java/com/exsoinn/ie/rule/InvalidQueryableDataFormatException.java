package com.exsoinn.ie.rule;

/**
 * Created by QuijadaJ on 5/24/2017.
 */
public class InvalidQueryableDataFormatException extends RuleException {
    public InvalidQueryableDataFormatException(String pMsg) {
        this(pMsg, null);
    }

    public InvalidQueryableDataFormatException(String pMsg, Throwable t) {
        super(pMsg, t);
    }
}
