package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Context;

/**
 * Created by QuijadaJ on 8/21/2017.
 */
public class MissingOutputFieldException extends RuleException {
    MissingOutputFieldException(Rule pRule, Context pCtx, String pFldName) {
        super("Rule component '" + pRule.name() + "' is missing output field '" + pFldName
                + "'. Fix this in the rule configuration and try again. Rule configuration context in question is "
                + pCtx.stringRepresentation());
    }
}
