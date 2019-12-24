package com.exsoinn.ie.rule;


import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.epf.*;

import java.util.*;


/**
 * Use this class to iterate over an array-type Context, applying the given rule(s) to each array Context member,
 * getting back a result in Context form, and then storing the array of Context results back into the original Context
 * given.
 *
 * These are the the input parameters:
 *   - The Context object.
 *   - The {@link com.exsoinn.util.epf.SelectionCriteria} to the array within the Context, which will be another Context where
 *   {@link Context#isArray()} is expected to return true. If it is not an array (I.e. {@link Context#isArray()}
 *   returns false) then exception is thrown. If you prefix this input parameter with {@link RuleConstants#PREF_RULE},
 *   then that rule gets executed and the context is obtained by executing {@link AbstractRuleExecutionResult#evaluateResultAsContext()}
 *   off of the result object produced by the execute rule.
 *   - A list of rules to be applied to each member of the Context array. <strong>Each of these rules is expected to
 *   return a context that can be accessed via {@link AbstractRuleExecutionResult#evaluateResultAsContext()}. The
 *   developer is responsible for using rule objects that make sense.</strong>
 *   - The name of the member to use to store the array of Contexts produced from applying the rules above.
 *
 * Created by QuijadaJ on 10/22/2017.
 */
class IteratorRule extends AbstractRule {
    private final SelectionCriteria arraySelectionCriteria;
    private final Rule arraySelectionCriteriaRule;
    private final List<Rule> rulesToApply = new ArrayList<>();
    private final String destinationMemberNameForResult;
    private final Rule preRule;

    public static class Builder extends AbstractRule.Builder<IteratorRule, Builder> {
        // Required parameters
        private final String arraySelectionCriteria;
        private final List<String> rulesToApply;
        private final String destinationMemberNameForResult;

        // Optional parameters
        private String preRule = null;

        Builder(String pName, String pArySelCrit, List<String> pRulesToApply, String pDestKey) {
            super(pName);
            arraySelectionCriteria = pArySelCrit;
            rulesToApply = new ArrayList<>(pRulesToApply);
            destinationMemberNameForResult = pDestKey;
        }


        /**
         * This rule, when supplied, gets applied to the Context prior to the beginning of iterator function. The output context
         * of this rule is the input Context used by this {@link IteratorRule}
         * @param pRuleName
         * @return
         */
        public Builder preRule(String pRuleName) {
            preRule = pRuleName;
            return getThis();
        }


        @Override
        Builder getThis() {
            return this;
        }

        @Override
        public IteratorRule build() {
            try {
                Rule r = new IteratorRule(this);
                storeInRuleByNameCache(r, getName());
                return (IteratorRule) r;
            } catch (RuleException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    IteratorRule(Builder pBuilder) throws RuleNameNotFoundException {
        super(pBuilder.getName());
        if (pBuilder.arraySelectionCriteria.indexOf(RuleConstants.PREF_RULE) == 0) {
            arraySelectionCriteria = null;
            arraySelectionCriteriaRule =
                    lookupRuleByName(pBuilder.arraySelectionCriteria.substring(RuleConstants.PREF_RULE.length()));
        } else {
            arraySelectionCriteria = SelectionCriteria.valueOf(pBuilder.arraySelectionCriteria);
            arraySelectionCriteriaRule = null;
        }
        for (String r : pBuilder.rulesToApply) {
            rulesToApply.add(lookupRuleByName(r));
        }

        if (!CommonUtils.stringIsBlank(pBuilder.preRule)) {
            preRule = lookupRuleByName(pBuilder.preRule);
        } else {
            preRule = null;
        }
        destinationMemberNameForResult = pBuilder.destinationMemberNameForResult;
    }


    @Override
    public <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        if (null == pExtraParams) {
            pExtraParams = new HashMap<>();
        }


        /**
         * Parent class method {@link AbstractRule#apply(Context, SearchPath, Filter, TargetElements, Map)}
         * took care of saving calling rule's (if any) parent context, hence safe to overwrite
         * with ours here. The same parent class method will also restore back that parent context
         * once this rule executes. See finally block in {@link AbstractRule#apply(Context, SearchPath, Filter, TargetElements, Map)}
         */
        pExtraParams.put(RuleConstants.ARG_PARENT_CONTEXT, pContext.stringRepresentation());

        if (null != preRule) {
            pContext = preRule.apply(pContext, pSearchPath, pFilter, pTargetElems, pExtraParams).evaluateResultAsContext();
        }

        Context aryCtx;
        String selCritAsString;
        if (null != arraySelectionCriteriaRule) {
            RuleExecutionResult res = arraySelectionCriteriaRule.apply(pContext, null, null, null, null);
            aryCtx = res.evaluateResultAsContext();
            selCritAsString = arraySelectionCriteriaRule.name();
        } else {
            SearchResult searchRes = pContext.findElement(arraySelectionCriteria, pExtraParams);
            aryCtx = searchRes.entrySet().iterator().next().getValue();
            selCritAsString = arraySelectionCriteria.toString();
        }
        if (!aryCtx.isArray()) {
            throw new IllegalArgumentException("Rule with name " + this.name() + " and class "
                    + IteratorRule.class.getName() + " expects an array Context. Instead the selection criteria provided, "
                    + selCritAsString + " yielded Context " + aryCtx.stringRepresentation()
                    + ", which is not an array context.");
        }

        List<Context> ctxList = aryCtx.asArray();
        MutableContext destAryCtx = ContextFactory.obtainMutableContext("[]");
        if (null == pExtraParams) {
            pExtraParams = new HashMap<>();
        }


        for (Context c : ctxList) {
            Context resCtx = c;
            /**
             * Apply the rules to this context, at each point using the context returned by the last rule
             * as the input to the next rule. This has a cumulative effect where final context has the outputs
             * of all the rules.
             */
            for (Rule r : rulesToApply) {
                RuleExecutionResult result = r.apply(resCtx, pSearchPath, pFilter, pTargetElems, pExtraParams);
                resCtx = result.evaluateResultAsContext();
            }


            destAryCtx.addEntryToArray(resCtx);
        }

        final MutableContext finalCtx = ContextFactory.obtainMutableContext(pContext.stringRepresentation());
        finalCtx.addMember(destinationMemberNameForResult, destAryCtx);

        Map<String, String> info = populateCommonResultProperties(
                pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, SearchResult.emptySearchResult());

        return new AbstractRuleExecutionResult(info, pContext, null) {
            private final Context context = ContextFactory.obtainContext(finalCtx.stringRepresentation());
            @Override
            public boolean evaluateResult() {
                return true;
            }

            @Override
            public String evaluateResultAsString() throws RuleException {
                return context.stringRepresentation();
            }

            @Override
            public Context evaluateResultAsContext() {
                return context;
            }
        };
    }
}
