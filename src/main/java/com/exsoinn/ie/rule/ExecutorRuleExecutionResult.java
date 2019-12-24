package com.exsoinn.ie.rule;


import java.util.List;

/**
 * Created by QuijadaJ on 5/22/2017.
 * TODO: This only gets used in {@link com.exsoinn.ie.rule.ConfigurableRuleExecutor.ConfigurableRuleExecutorResult} to provide
 * TODO:  {@link RuleExecutionResult} lis. Add getter in that class and remove this interface.
 */
public interface ExecutorRuleExecutionResult extends RuleExecutionResult {
    List<RuleExecutionResult> getExecutedRuleResults();
}
