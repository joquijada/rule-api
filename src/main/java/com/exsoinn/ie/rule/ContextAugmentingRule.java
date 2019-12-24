package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.epf.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by QuijadaJ on 9/12/2017.
 */
class ContextAugmentingRule extends AbstractRule {
    private final Map<String, Object> nameValuePairsToAdd;
    private final String alternateSourceContextMapKey;
    private final Map<String, String> ruleOutputChosenField;

    /**
     * The required parameters are this rule's unique name, and the name/value pairs to add. The value could be prefixed
     * with {@link RuleConstants#PREF_RULE}, in which case the rule gets executed and that output of that rule is uses
     * as the value.
     * Optionally can give the key to get the source context to operate on when obtaining a value. The key must exist
     * in the extra parameters map that's passed to {@link ContextAugmentingRule#applyBody(Context, SearchPath, Filter, TargetElements, Map)}.
     * The code will then use that as the context to use to read the values. This arose from need to have separate
     * contexts, one is the supplier of the values to use to augment the target context, and the other one (the one passed to
     * {@link ContextAugmentingRule#applyBody(Context, SearchPath, Filter, TargetElements, Map)}) is the receiver/target
     * of the name/value pairs. If this parameter is not specified, then the the context object passed to
     * {@link ContextAugmentingRule#applyBody(Context, SearchPath, Filter, TargetElements, Map)} serves as both the supplier
     * and receiver of the name/value pairs.
     */
    public static class Builder extends AbstractRule.Builder<ContextAugmentingRule, ContextAugmentingRule.Builder> {
        // Required parameters
        private final Map<String, Object> nameValuePairsToAdd;

        // Optional parameters
        private Map<String, String> ruleOutputChosenField = null;


        Builder(String pName, Map<String, String> pNameValPairs) {
            super(pName);
            /**
             * Must do this, in case client code (I.e. JavaScript) is re-using
             * the passed in for multiple purposes. Doing {@link Collections#unmodifiableMap(Map)}
             * won't caught it, because the backing Map passed by client prior to wrapping in
             * unmodifiable Map can still be modified.
             */
            nameValuePairsToAdd = new HashMap<>(pNameValPairs);
        }


        /**
         * When the value comes from a rule, and that rule produces a complex Context object, this Map is used to tell this
         * component which field to pick from the output Context of the value-supplying rule.
         * This parameter is a Map of rule name to a field name that should exist in the Context produced by that rule.
         * Oh yeah, and by specifying this parameter, you're implicitly saying that the rule which output is used to augment
         * the context, is actually something that can be successfully parsed into a Context object, otherwise runtime
         * exception will get thrown. So for example, let's say that {@link this#nameValuePairsToAdd} contains
         * "fldName -> RULE:fooBar" entry, and it produces a Context like '{fld1:val1, fld2:val2}', then you can tell
         * this component to pick "fld1" by adding entry to "ruleOutputChosenField" map like so:
         *
         * "fooBar -> fld1"
         *
         * @param pMap
         * @return
         */
        public Builder ruleOutputChosenField(Map<String, String> pMap) {
            ruleOutputChosenField = new HashMap<>(pMap);
            return getThis();
        }


        @Override
        public ContextAugmentingRule build() {
            try {
                Rule r = new ContextAugmentingRule(this);
                storeInRuleByNameCache(r);
                return (ContextAugmentingRule) r;
            } catch (RuleException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        Builder getThis() {
            return this;
        }
    }


    ContextAugmentingRule(Builder pBuilder) {
        super(pBuilder);
        nameValuePairsToAdd = pBuilder.nameValuePairsToAdd;
        alternateSourceContextMapKey = pBuilder.getAlternateSourceContextMapKey();
        ruleOutputChosenField = pBuilder.ruleOutputChosenField;
    }



    @Override
    public <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        String ctxStr = null;
        MutableContext mc;
        String altSrcCtxStr = null;
        try {
            /**
             * Read documentation of {@link AbstractBuilder#alternateSourceContextMapKey(String)} to learn about
             * the purpose of alternateSourceContextMapKey.
             */
            if (!CommonUtils.stringIsBlank(alternateSourceContextMapKey)) {
                altSrcCtxStr = pExtraParams.get(alternateSourceContextMapKey);
            }
            ctxStr = pContext.stringRepresentation();
            mc = ContextFactory.obtainMutableContext(ctxStr);
            Set<Map.Entry<String, Object>> ents = nameValuePairsToAdd.entrySet();
            Context srcCtx = !CommonUtils.stringIsBlank(altSrcCtxStr) ? ContextFactory.obtainContext(altSrcCtxStr)
                    : ContextFactory.obtainContext(mc.stringRepresentation());
            for (Map.Entry<String, Object> e : ents) {
                final String destFldName = e.getKey();
                /**
                 * If the field value comes from a rule, the {@link AbstractRule#handleRulePrefixValue(String, Context, Map)}
                 * call below will execute it; we have passed to it the same <code>Context</code> object we have received
                 * here. Else regard it as a constant.
                 */
                Object valToAdd = e.getValue();
                String ruleName = null;
                if (valToAdd instanceof String && ((String) valToAdd).indexOf(RuleConstants.PREF_RULE) == 0) {
                    ruleName = valToAdd.toString().substring(RuleConstants.PREF_RULE.length());
                    valToAdd = handleRulePrefixValue((String) valToAdd, srcCtx, pExtraParams);
                }

                /**
                 * If ruleOutputChosenField has been specified, then for the rule names in that Map
                 * pick the associated field from the output Context it produces, and that will be the value
                 * augmented into the target context
                 */
                if (!CommonUtils.stringIsBlank(ruleName) && null != ruleOutputChosenField
                        && ruleOutputChosenField.containsKey(ruleName)) {
                    Context ruleRes = ContextFactory.obtainContext(valToAdd.toString());
                    valToAdd = ruleRes.memberValue(ruleOutputChosenField.get(ruleName)).stringRepresentation();
                }
                mc.addMember(destFldName, ContextFactory.obtainContext(valToAdd));
            }
        } catch (Exception e) {
            throw convertToRuleException("Problem encountered while augmenting context. Context augmenter rule name is '" + name()
                    + "'. Target context was " + ctxStr + ". Values being added were "
                    + nameValuePairsToAdd.entrySet().stream().map(Map.Entry::toString).collect(Collectors.joining(";")), e);
        }

        Map<String, String> info = populateCommonResultProperties(
                pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, SearchResult.emptySearchResult());
        return new TextOutputRuleExecutionResult(
                true,
                info,
                mc.stringRepresentation(),
                pContext,
                getOutputFieldMap()) {
            @Override
            public Context evaluateResultAsContext() {
                return ContextFactory.obtainMutableContext(mc.stringRepresentation());
            }
        };
    }
}
