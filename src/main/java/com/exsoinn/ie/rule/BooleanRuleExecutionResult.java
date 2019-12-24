package com.exsoinn.ie.rule;


import com.exsoinn.util.epf.Context;

import java.util.Map;

/**
 * Created by QuijadaJ on 4/20/2017.
 */
class BooleanRuleExecutionResult extends AbstractRuleExecutionResult {
    private final boolean result;

    BooleanRuleExecutionResult(boolean pVal, Map<String, String> pParams, Context pInputCtx, Map<String, Rule> pOutFldMap)
            throws RuleException {
        super(pParams, pInputCtx, pOutFldMap);
        result = pVal;
    }

    @Override
    public boolean evaluateResult() {
        return result;
    }

    @Override
    public String evaluateResultAsString() throws RuleException {
        return Boolean.valueOf(evaluateResult()).toString();
    }
}
