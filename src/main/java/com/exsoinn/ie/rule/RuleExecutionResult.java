package com.exsoinn.ie.rule;


import com.exsoinn.util.DnbBusinessObject;
import com.exsoinn.util.epf.Context;

/**
 * Created by QuijadaJ on 4/19/2017.
 */
public interface RuleExecutionResult extends DnbBusinessObject {
    /**
     * Returns a boolean to indicate the rule result. Implementing classes are free to return whatever boolean value
     * makes sense to the rule result produced.
     * @return - <code>true</code> or <code>false</code>, according to what these mean to the {@link Rule}
     *   object that produced the result
     */
    boolean evaluateResult();

    /**
     * Return a string that can indicate to caller that the {@link Rule} produced. As with
     * {@link RuleExecutionResult#evaluateResult()}, implementers return whatever value is appropriate
     * for the result produced by the rule.
     * @return
     * @throws RuleException
     */
    String evaluateResultAsString() throws RuleException;

    /**
     * Return a rule result in {@link Context} format. This is suitable if implementing class wants to return
     * a set of name/value paris that are appropriate in the context of the rule that ran.
     * @return
     */
    Context evaluateResultAsContext();


    /**
     * This signature accepts flag that implementing classes can use to give caller code control
     * over whether or not values not applicable to rule result should be suppressed. By default,
     * behavior can be to suppress such fields by displaying their value as "NOT_APPLICABLE", but via this method
     * caller can request the method to display actual value.
     * @param bSuppressNotApplicableFields
     * @return
     */
    Context evaluateResultAsContext(boolean bSuppressNotApplicableFields);

    /**
     * Meant to produce the time it took, in seconds, for the rule to execute. Can help with
     * performance tuning.
     * @return - Time in seconds to indicate seconds elapsed from rule start to end.
     */
    double totalExecutionTimeInSeconds();


    /**
     * Implementing classes can use this to give any relevant output value in string format.
     * @return
     */
    String getOutput();
}
