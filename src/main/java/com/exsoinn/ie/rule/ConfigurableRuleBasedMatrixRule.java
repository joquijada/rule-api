package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.epf.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by QuijadaJ on 5/22/2017.
 */
class ConfigurableRuleBasedMatrixRule extends QueryableDataRule {
    private final Map<String, Rule> rulesToExecute;
    private final String decisionFieldName;

    // When not provided, use a default field name that's expected to be defined in the matrix
    private static final String DEFAULT_DECISION_FIELD_NAME = "decision";


    /*
     * Constructors
     */
    ConfigurableRuleBasedMatrixRule(
            Object pObj,
            String pUniqueName,
            String pDecisionFldName) throws RuleException {
        super(pObj, pUniqueName);
        if (CommonUtils.stringIsBlank(pDecisionFldName)) {
            decisionFieldName = DEFAULT_DECISION_FIELD_NAME;
        } else {
            decisionFieldName = pDecisionFldName;
        }

        /*
         * Build list of Rules. The rule names are assumed to be any field name which is not
         * the decisionFieldName
         */
        List<String> fieldNames = fieldNames();

        /*
         * Load the rule objects for the rule names configured in this matrix.
         */
        rulesToExecute = fieldNames.stream().filter(e -> !decisionFieldName.equals(e))
                .collect(Collectors.toMap(String::toString, e -> {
                    try {
                        return AbstractRule.lookupRuleByName(e);
                    } catch (RuleNameNotFoundException exc) {
                        logError(exc);
                    }
                    return null;
                }));
    }


    @Override
    public  <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {

        return AbstractRule.evaluateRuleBasedMatrix(name(), pContext);
    }



    /*
     * Getters
     */
    public Map<String, Rule> getRulesToExecute() {
        return rulesToExecute;
    }
    public String getDecisionFieldName() {
        return decisionFieldName;
    }

}
