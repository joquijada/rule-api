package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.ContextFactory;
import com.exsoinn.util.epf.MutableContext;

import java.util.Map;

/**
 * This class encapsulates the bare minimum logic to display rule result as a {@link Context} object
 * for rule set operation types, I.e. those rules that can be executed from within a rule set of type
 * {@link QueryableRuleConfigurationData}. Child classes can feel free to add additional fields that make sense for them.
 * Again this class is primarily intended for rules which operate in the context of a configurable rule set. More specifically,
 * a rule class which has concept of a left operand, an operation, a right operand, and optionally a left operand
 * pre-processing operation. Examples of classes that return a rule result object of this type are
 * {@link StringHandlerRule} and {@link RelOpRule}.
 *
 * Created by QuijadaJ on 8/14/2017.
 */
class RuleSetOperationResult extends TextOutputRuleExecutionResult {
    RuleSetOperationResult(boolean pVal, Map<String, String> pParams, String pOutput, Context pInputCtx,
                           Map<String, Rule> pOutFldMap) throws RuleException {
        super(pVal, pParams, pOutput, pInputCtx, pOutFldMap);
    }


    /**
     * Overridden to ignore the {@param bSuppressNotApplicableFields} flag, which does not make sense for this
     * component.
     * @param bSuppressNotApplicableFields
     * @return
     */
    @Override
    public Context evaluateResultAsContext(boolean bSuppressNotApplicableFields) {
        return evaluateResultAsContext();
    }

    @Override
    public Context evaluateResultAsContext() {
        MutableContext mc = ContextFactory.obtainMutableContext("{}");
        try {
            populateOutputData(mc);
        } catch (RuleException e) {
            throw new IllegalArgumentException("Problem populating output fields for component "
                    + this.getClass().getName(), e);
        }

        String[] props = {RuleConstants.LEFT_HAND_OPERAND, RuleConstants.OPERATION, RuleConstants.RIGHT_HAND_OPERAND,
                RuleConstants.LEFT_OPERAND_PREPROCESSOR};
        for (String p : props) {
            if (CommonUtils.stringIsBlank(getParams().get(p))) {
                continue;
            }
            addNameValuePairToContext(p, getParams().get(p), mc);
        }

        try {
            addNameValuePairToContext("result", evaluateResultAsString(), mc);
        } catch (RuleException e) {
            addNameValuePairToContext("result", "There was a problem evaluating rule execution result: " + e, mc);
        }

        return ContextFactory.obtainContext(mc.stringRepresentation());
    }
}
