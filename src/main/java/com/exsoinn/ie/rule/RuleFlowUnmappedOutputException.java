package com.exsoinn.ie.rule;

import java.util.stream.Collectors;


/**
 * Created by QuijadaJ on 6/7/2017.
 */
class RuleFlowUnmappedOutputException extends RuleException {
    RuleFlowUnmappedOutputException(String pMsg, Throwable t) {
        super(pMsg, t);
    }

    RuleFlowUnmappedOutputException(String pActualOutput, RuleFlowImpl.RuleFlowEntity pRuleFlowEnt, RuleFlow pRuleFlow) {
        this("Rule '" + pRuleFlowEnt.ruleToExecute.name() + "' produced an output not expected in the rule flow configuration. Actual output was '"
                + pActualOutput + "', yet only one of these outputs was expected: "
                + (pRuleFlowEnt.outputToRuleMap != null ?
                pRuleFlowEnt.outputToRuleMap.entrySet().stream().map(e -> "'" + e.getKey() + "'").collect(Collectors.joining(",")) : "")
                + ". The full rule flow configuration is "
                + pRuleFlow.context().stringRepresentation(), null);
    }
}
