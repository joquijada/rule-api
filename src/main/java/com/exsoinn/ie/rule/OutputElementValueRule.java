package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.EscapeUtil;
import com.exsoinn.util.epf.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class just simply uses the {@link SelectionCriteria} criteria provided at object construction time,
 * searches tha {@code Context} passed in {@link OutputElementValueRule#apply(Context, SearchPath, Filter, TargetElements, Map)},
 * and outputs the result of the finding. If the results found is not exactly 1 in size, {@code RuleException}
 * gets thrown.
 *
 * If the {@link OutputElementValueRule#selectionCriteriaSource} member was initialized, then that will be used as
 * the source of where to get the selection criteria from, to then search the underlying input data. When that's the case,
 * then member {@link OutputElementValueRule#selectionCriteria} is assume to be the key that will yield the selection
 * criteria string to use to search the input data.
 *
 * TODO: Should this component ignore {@link IncompatibleSearchPathException} that {@link AbstractContext} throws
 *   when the specified search path (also the search path in filter and target elements) is not found in the
 *   {@link Context} object in question? Currently the code that calls on this rule determines this behavior, namely
 *   by passing appropriate ignore flags in the extra params Map. See {@link QueryableRuleConfigurationData#prepareQueryReturnObject(Context, Context, Map, SearchResult, Map)}
 *   for an example, search for "handleRulePrefixValue", notice the 3rd argument, which is the Map that I talk
 *   about.
 *
 * Created by QuijadaJ on 6/22/2017.
 */
class OutputElementValueRule extends AbstractRule {
    private final SelectionCriteria selectionCriteria;
    private final QueryableDataRule selectionCriteriaSource;
    private final Rule postProcessor;
    private final boolean ignoreElementNotFoundError;
    private final boolean convertResultToDelimitedString;
    private final String alternateSourceContextMapKey;


    /**
     * See comment in {@link JoinOutputRule} regarding why this class has to be
     * declared public. Same applies here.
     */
    public static class Builder extends AbstractBuilder<OutputElementValueRule> {
        // Required parameters
        private final String name;
        private final SelectionCriteria selectionCriteria;

        // Optional parameters
        private QueryableDataRule selectionCriteriaSource = null;
        private Rule postProcessor = null;
        private boolean ignoreElementNotFoundError = false;
        private boolean convertResultToDelimitedString = true;


        /*
         * Constructors
         */
        Builder(String pName, String pSelectCriteria) {
            name = pName;
            selectionCriteria = SelectionCriteria.valueOf(pSelectCriteria);
        }


        public Builder selectionCriteriaSource(String pSrcName) throws RuleNameNotFoundException {
            QueryableDataRule qdr = (QueryableDataRule) lookupRuleByName(pSrcName);
            selectionCriteriaSource = qdr;
            return this;
        }

        public Builder postProcessor(String pName) {
            try {
                postProcessor = lookupRuleByName(pName);
            } catch (RuleNameNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
            return this;
        }


        public Builder ignoreElementNotFoundError(boolean pFlag) {
            ignoreElementNotFoundError = pFlag;
            return this;
        }

        public Builder convertResultToDelimitedString(boolean pFlag) {
            convertResultToDelimitedString = pFlag;
            return this;
        }

        @Override
        public OutputElementValueRule build() {
            try {
                String[] keyAry;
                keyAry = new String[]{selectionCriteria.toString(),
                        null != selectionCriteriaSource ? selectionCriteriaSource.name() : "",
                        null != postProcessor ? postProcessor.name() : "",
                        Boolean.valueOf(ignoreElementNotFoundError).toString()
                };

                String cacheKey = generateCacheKey(keyAry);
                Rule r = getRuleCache().get(cacheKey);

                /**
                 * Grab from cache if an finder object was already created
                 * earlier for the selectionCriteria specified, and store it in
                 * rule-by-name cache further below using the passed in {@link name}
                 */
                if (null == r) {
                    r = new OutputElementValueRule(this);
                    r = uniqueRule(r, cacheKey);
                }
                storeInRuleByNameCache(r, name);
                return (OutputElementValueRule) r;
            } catch (RuleException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }


    OutputElementValueRule(Builder pBuilder) {
        super(pBuilder.name);
        selectionCriteria = pBuilder.selectionCriteria;
        selectionCriteriaSource = pBuilder.selectionCriteriaSource;
        postProcessor = pBuilder.postProcessor;
        ignoreElementNotFoundError = pBuilder.ignoreElementNotFoundError;
        convertResultToDelimitedString = pBuilder.convertResultToDelimitedString;
        alternateSourceContextMapKey = pBuilder.getAlternateSourceContextMapKey();
    }


    @Override
    public <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        SearchResult sr;
        SelectionCriteria ctxSelCrit = selectionCriteria;
        /**
         * If "selectionCriteriaSource" is not null, then it means we need to do an extra hop
         * to get the selection criteria string to use by querying "selectionCriteriaSource". In those cases,
         * variable "selectionCriteria" is used to first search "selectionCriteriaSource",
         * which then yields the final selection criteria to search "pContext" ("pContext" is nothing more than the input
         * data that was given to Rule API to work on).
         * Otherwise we skip "if()" below and just use the provided "selectionCriteria" further below directly to
         * search the underlying input data.
         */
        if (null != selectionCriteriaSource) {
            SearchResult foundSelCrit = selectionCriteriaSource.context().findElement(selectionCriteria, null);
            ctxSelCrit = SelectionCriteria.valueOf(foundSelCrit.entrySet().iterator().next().getValue().stringRepresentation());
        }

        if (ignoreElementNotFoundError) {
            if (null == pExtraParams) {
                pExtraParams = new HashMap<>();
            }
            pExtraParams.put(Context.IGNORE_INCOMPATIBLE_SEARCH_PATH_PROVIDED_ERROR, "1");
            pExtraParams.put(Context.IGNORE_INCOMPATIBLE_TARGET_ELEMENT_PROVIDED_ERROR, "1");
        }

        /**
         * Read documentation of {@link AbstractBuilder#alternateSourceContextMapKey(String)} to learn about
         * the purpose of alternateSourceContextMapKey.
         */
        if (!CommonUtils.stringIsBlank(alternateSourceContextMapKey)) {
            pContext = ContextFactory.obtainContext(pExtraParams.get(alternateSourceContextMapKey));
        }
        sr = pContext.findElement(ctxSelCrit, pExtraParams);


        if (sr.size() > 1) {
            throw new RuleException("Found more than one result, not sure what to do."
                    + " SelectionCriteria was " + selectionCriteria.toString());
        }

        if (sr.size() < 1) {
            sr = buildFakeSearchResult(RuleConstants.BLANK_TOKEN);
        }

        String foundVal;
        /**
         * If we got multiple values in the search results, then build a delimited string of those values. When
         * the result is an array of complex objects, method {@link Utilities#toDelimitedString(List)} will convert
         * <strong>the values (not the keys)</strong> not a delimited string, as long as each complex object in the array
         * contains one and only one name/value pair - other cases throw exception.
         */
        final Context foundCtx = sr.entrySet().iterator().next().getValue();
        if (convertResultToDelimitedString && foundCtx.isArray()) {
            try {
                foundVal = Utilities.toDelimitedString(sr.entrySet().iterator().next().getValue().asArray());
            } catch (Exception e) {
                throw convertToRuleException("Problem converting array results to delimited string. See all exception details "
                        + "for more details. SelectionCriteria was " + selectionCriteria.toString() + ". Search results (if any)"
                        + " were " + sr.entrySet().iterator().next().getValue().stringRepresentation() + ". This rule's name is " + name()
                        + " (Hint: If it's the entire Context output you're after, set optional parameter 'convertResultToDelimitedString'"
                        + " to 'false' in the configuration of this component.)", e);
            }
        } else {
            foundVal = foundCtx.stringRepresentation();
        }


        /**
         * If a post processor rule was defined, apply it to the output before
         * finally giving it to the caller. We give the processor the value to operate on
         * by putting it in key {@link RuleConstants#ARG_LEFT_OPERAND} of params
         * map. This means that the post processor must know to get the value from there, else
         * this will not have any effect. Check the rule class of the post processor you're trying to use
         * to see where it expects to input.
         */
        if (null != postProcessor) {
            Map<String, String> m = new HashMap<>();
            foundVal = EscapeUtil.unescapeSpecialCharacters(foundVal);
            m.put(RuleConstants.ARG_LEFT_OPERAND, foundVal);
            RuleExecutionResult res = postProcessor.apply(pContext, null, null, null, m);
            foundVal = res.evaluateResultAsString();
        }

        Map<String, String> info = populateCommonResultProperties(
                pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, sr);
        return new TextOutputRuleExecutionResult(
                true,
                info,
                foundVal,
                pContext,
                getOutputFieldMap()) {
            @Override
            public Context evaluateResultAsContext() {
                return foundCtx;
            }
        };
    }
}
