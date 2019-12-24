package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.epf.Context;

import java.util.Map;

/**
 * Created by QuijadaJ on 5/22/2017.
 */
class TextOutputRuleExecutionResult extends BooleanRuleExecutionResult
        implements ITextOutputRuleExecutionResult {


    private final String output;

    TextOutputRuleExecutionResult(
            boolean pVal,
            Map<String, String> pParams,
            String pOutput,
            Context pInputCtx,
            Map<String, Rule> pOutFldMap)
            throws RuleException {
        super(pVal, pParams, pInputCtx, pOutFldMap);
        output = pOutput;
    }


    @Override
    public String evaluateResultAsString() throws RuleException {
        return !CommonUtils.stringIsBlank(output) ? output : Boolean.valueOf(super.evaluateResult()).toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("\nText Output: ");
        try {
            sb.append(evaluateResultAsString());
        } catch (RuleException e) {
            sb.append("Problem evaluating result: ");
            sb.append(e.toString());
            AbstractRule.logError(e);
        }
        return sb.toString();
    }

    @Override
    public String getOutput() {
        return output;
    }
}
