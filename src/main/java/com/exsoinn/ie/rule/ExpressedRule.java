package com.exsoinn.ie.rule;

import com.exsoinn.ie.rule.definition.AbstractRuleExpression;
import com.exsoinn.ie.rule.definition.Parser;
import com.exsoinn.ie.rule.definition.RuleExpression;
import com.exsoinn.util.epf.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by QuijadaJ on 10/11/2017.
 */
class ExpressedRule extends AbstractRule implements Parser<RuleExpression, Context> {
    private final RuleExpression<Object, Context> ruleExpression;

    @Override
    public RuleExpression parse(Context pRuleSrc) {
        return AbstractRuleExpression.parse(pRuleSrc);
    }

    /**
     * See comment in {@link JoinOutputRule} regarding why this class has to be
     * declared public. Same applies here.
     */
    public static class Builder implements com.exsoinn.ie.util.Builder<ExpressedRule> {
        // Required parameters
        private final String name;
        private final Object ruleSource;


        // Optional parameters
        private List<String> outputFields = null;

        Builder(String pName, Object pRuleSrc) {
            name = pName;
            ruleSource = pRuleSrc;
        }


        public Builder outputFields(List<String> pOutFlds) {
            outputFields = null == pOutFlds ? null : new ArrayList<>(pOutFlds);
            return this;
        }

        @Override
        public ExpressedRule build() {
            try {
                Rule r = new ExpressedRule(ruleSource, name, outputFields);
                storeInRuleByNameCache(r, name);
                return (ExpressedRule) r;
            } catch (RuleException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }


    ExpressedRule(Object pObj, String pUniqueName, List<String> pOutFlds)
            throws RuleException {
        super(pUniqueName, pOutFlds);
        ruleExpression = parse(ContextFactory.INSTANCE.obtainContext(pObj));
    }



    @Override
    public <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        Object obj = ruleExpression.evaluate(pContext);
        Map<String, String> info = populateCommonResultProperties(
                pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, SearchResult.emptySearchResult());
        info.put(RuleConstants.RULE_EXPRESSION,
                ((AbstractRuleExpression)ruleExpression).getElementContent().stringRepresentation());
        boolean resIsBool = obj instanceof Boolean;
        return new TextOutputRuleExecutionResult(resIsBool ? (Boolean) obj : true,
                info, obj.toString(), pContext, getOutputFieldMap()) {
            @Override
            public Context evaluateResultAsContext() {
                Context retCtx;
                if (obj instanceof Context) {
                    retCtx = (Context) obj;
                } else {
                    MutableContext mc = ContextFactory.obtainMutableContext("{}");
                    mc.addMember("result", ContextFactory.obtainContext(obj.toString()));
                    retCtx = ContextFactory.obtainContext(mc.stringRepresentation());
                }
                return retCtx;
            }
        };
    }
}
