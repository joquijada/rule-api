package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Context;

/**
 * Created by QuijadaJ on 6/19/2017.
 */
public class NotAllRowsHaveSameFieldsException extends RuleException {

    NotAllRowsHaveSameFieldsException(Rule pRule, String pExpectedFlds, String pThisRowFlds, Context pRow, Context pTable) {
        super("Problem processing rule component " + pRule.name() + "This row did not have same fields as the others: " + pRow.stringRepresentation() +
                ". All rows are expected to have these fields: \"" + pExpectedFlds
                + "\". The full context is " + pTable.stringRepresentation());
    }

}
