package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.EscapeUtil;
import com.exsoinn.util.epf.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 *   When looking up a value, you have two options of how to specify the matching/filter behavior: can either modify
 *   the filter/matching behavior on the value being looked up, by specifying the desired lookup operation, or on the
 *   configured list terms themselves:
 *     - Configuring match behavior on the lookup value: You pick one of the available lookup match operations in the
 *       ({@link Operation} enum (following the correct syntax of course of "LU||<MATCH_OP>"). The former will do
 *       exact comparison between lookup value and each term configured in the list, the latter does a partial match
 *       by putting wildcard at both the beginning and end of the lookup value.
 *       In both cases only the first match is returned, even when multiple matching list terms/values were found.
 *     - Modifying matching behavior on configured list values themselves (for this, the lookup operation
 *       should be defined as {@link Operation#LIST_DEFINED}): Note: When this behavior is specified,
 *       then the match behavior on the lookup value is completely ignored, and superseded by this one instead!
 *       You configure the list value match (aka filter) behavior in the list name itself. The code will
 *       search for keyword "matchStyle" in the list name, followed by a colon, and then a valid and supported
 *       match option. Currently the match options supported are
 *       * exact: The passed in value must be exact the same as the configured list value in order for a result
 *         to be found
 *       * partial: Means that as long as the configured list value is found <strong>anywhere</strong> in the input
 *         string, then it's considered a match.
 *       * partialWholePhrase: Like partial above, except that the configured list term/value mast match a whole
 *         phrase in the input. We use the term "phrase" instead of "word" because configured list values van be
 *         made up of more than just one word as well.
 *       As a las step, the lookup operation should be LU||LIST_DEFINED" when the matching behavior is configured
 *       using the list name,
 *   TODO: To simplify and consolidate, can just use {@link Operation} for all lookup operations, and then the
 *   TODO: specific application configurations can generate list buckets to map list entries to the specific match behavior
 *   TODO: desired, then a rule set operation can specify the correct lookup {@link Operation} for that bucket created by the user.
 *
 * Created by MohideenS on 5/31/2017.
 */
class LookupRuleExecutor extends AbstractRule implements RuleExecutor<QueryableLookupRule> {
    private static final String WILD_CARD = "*";
    final static String GENERIC_CACHE_KEY = "genericLookupRuleExecutor";
    private final static String REFER_MSG = "Refer to executed operation trail.";
    private static final String BLANK = "BLANK";
    private static final List<String> blankValueList;
    private final boolean ignoreListNotExistsError;

    static {
        blankValueList = new ArrayList<>();
        blankValueList.add(BLANK);
    }


    /*
     * Builder
     */
    public static class Builder implements com.exsoinn.ie.util.Builder<LookupRuleExecutor> {
        // Required parameters
        private final String listName;

        // Optional parameters
        private String name = null;
        private boolean ignoreListNotExistsError = false;
        private List<String> outputFields = null;


        Builder(String pListName) {
            listName = pListName;
        }

        public Builder name(String pName) {
            name = pName;
            return this;
        }

        public Builder ignoreListNotExistsError(boolean pFlag) {
            ignoreListNotExistsError = pFlag;
            return this;
        }

        public Builder outputFields(List<String> pOutFlds) {
            outputFields = pOutFlds;
            return this;
        }

        @Override
        public LookupRuleExecutor build() {
            String cacheKey = generateCacheKey(new String[] {listName});
            Rule rule = getRuleCache().get(cacheKey);
            if (null == rule) {
                rule = new LookupRuleExecutor(this);
                rule = uniqueRule(rule, cacheKey);
            }
            return (LookupRuleExecutor) rule;
        }
    }


    /*
     * Constructor
     */
    LookupRuleExecutor(Builder pBuilder) {
        super(pBuilder.name, pBuilder.outputFields);
        ignoreListNotExistsError = pBuilder.ignoreListNotExistsError;
    }



    @Override
    public RuleExecutionResult apply(String pDataStr,
                                     String pElemSearchPath,
                                     Map<String, String> pElemFilter,
                                     Set<String> pTargetElems,
                                     Map<String, String> pExtraParams) throws RuleException {
        Context c = ContextFactory.obtainContext(pDataStr);
        return apply(c, convertToSearchPath(pElemSearchPath), convertToFilter(pElemFilter),
                convertToTargetElements(pTargetElems), pExtraParams);
    }

    @Override
    public  <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {

        Operation operation = null;
        final String listName;
        String lookupInputModifyingOp = null;
        if (null != pExtraParams && pExtraParams.containsKey(RuleConstants.ARG_LIST_NAME)) {
            listName = pExtraParams.get(RuleConstants.ARG_LIST_NAME);
        } else {
            throw new RuleException("List name cannot be empty requesting a lookup operation.");
        }

        if (null != pExtraParams && pExtraParams.containsKey(RuleConstants.ARG_OPERATION)) {
            String[] luParams = pExtraParams.get(RuleConstants.ARG_OPERATION).split(Pattern.quote("||"));
            operation = Operation.fromSymbol(luParams[1]);

            /**
             * If there needs to be an operation applied on the lookup input value, then there would have
             * been 3 double-pipe delimited arguments specified, the 3rd of which is said input
             * modifying operation. The logic below fetches it.
             */
            if (luParams.length > 2) {
                lookupInputModifyingOp = luParams[2];
            }
        }

        final List<String> valsToCheck = new ArrayList<>();
        if (null != pExtraParams && pExtraParams.containsKey(RuleConstants.ARG_LEFT_OPERAND)
                && !CommonUtils.stringIsBlank(pExtraParams.get(RuleConstants.ARG_LEFT_OPERAND))) {
            if (!CommonUtils.stringIsBlank(lookupInputModifyingOp)) {
                /**
                 * TODO: WARNING: This will cause for lookups to fail that specify a string mod op, and which pass
                 * TODO:  input as "x,y,z" instead of "x, z","y","z"!!!
                 */
                valsToCheck.addAll(Arrays.asList(pExtraParams.get(RuleConstants.ARG_LEFT_OPERAND)));
            } else {
                valsToCheck.addAll(Arrays.asList(pExtraParams.get(RuleConstants.ARG_LEFT_OPERAND)
                        .split(RuleConstants.MULTI_VAL_STR_DELIM, -1)));
            }
        }

        /**
         * Translate passed in blank values to some non-blank
         * token that still says the value is blank.
         */
        List<String> tempValsToCheck = valsToCheck.stream().map(e -> CommonUtils.stringIsBlank(e) ? BLANK : e).collect(Collectors.toList());
        valsToCheck.clear();
        valsToCheck.addAll(tempValsToCheck);

        Rule ruleObj = lookupRuleByName(pExtraParams.get(RuleConstants.ARG_LOOKUP_COMPONENT_NAME));
        if (!(ruleObj instanceof QueryableLookupRule)) {
            throw new RuleException("Component name passed in to this " + this.getClass().getName()
                    + " is of type " + ruleObj.getClass().getName() + ". I can only handle "
                    + QueryableLookupRule.class.getName());
        }


        /**
         * Check before proceeding that the lookup matching characteristics have been
         * provided one way or another, throw exception otherwise.
         * IMPORTANT: We do this even before we check if input value is blank, because we want to be able
         *   to report what the match option requested was. We do this for debugging/troubleshooting/diagnosing purposes.
         */
        QueryableLookupRule lookupRule = (QueryableLookupRule) ruleObj;
        boolean listNotFound = false;
        String tmpMatchStyle = null;
        try {
            tmpMatchStyle = lookupRule.matchStyle(listName);
            if (operation == Operation.LIST_DEFINED && CommonUtils.stringIsBlank(tmpMatchStyle)) {
                throw new RuleException("The lookup operation defined was " + operation.toString()
                        + ", yet the list name itself did not define a match option, unable to proceed. Ensure "
                        + "the list name defines one of the supported lookup match options: "
                        + Arrays.stream(MatchStyle.values()).map(e -> e.matchStyle()).collect(Collectors.joining(",")));
            }
        } catch(ListNotFoundException e) {
            if (ignoreListNotExistsError) {
                listNotFound = true;
            } else {
                throw e;
            }
        }

        final String matchStyle = tmpMatchStyle;


        /**
         * Nothing to lookup, exit.
         * IMPORTANT: FOR PERFORMANCE/OPTIMIZATION REASONS, DO NOT ADD ADDITIONAL LOGIC ABOVE THIS UNLESS ABSOLUTELY
         *   NECESSARY. WE WANT TO EXIT AS EARLY AS POSSIBLE IF THE PASSED IN LOOKUP INPUT IS BLANK.
         * TODO: What happens when list is not found, and rule set option is "ALL MUST PASS"? Below will return
         * TODO:   a false rule result, which means calling code will interpret as not found. Shouldn't we be returning
         * TODO:   failed rule set op status in that scenario? Re-visit, but keep this in mind.
         */
        if (valsToCheck.isEmpty() || listNotFound) {
            String msg = valsToCheck.isEmpty() ? " got blank input, will not do anything"
                    : " got an unrecognized list name";

            logDebug("Lookup rule component " + lookupRule.name() + msg + ". List name is " + listName);
            return prepareResult(pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, SearchResult.emptySearchResult(),
                    listName, lookupRule, operation, matchStyle, valsToCheck.isEmpty() ? blankValueList : valsToCheck,
                    RuleConstants.NOT_APPLICABLE, RuleConstants.NOT_APPLICABLE);
        }

        /**
         * For multi-valued lookup input, replace {@link RuleConstants#BLANK_TOKEN} with nothing. This way
         * the lookup will not fail for those. For single-value lookup entries, leave as is, we already know blanks
         * will not be found against lookup list.
         */
        removeBlanks(valsToCheck);


        /**
         * If so requested as part of the lookup operation, apply any input modifying
         * operation. Hand off to our friend StringHandlerRule to handle that. The resulting value will be
         * the modified input to use for the lookup operation. Do this for every lookup input.
         */
        if (!CommonUtils.stringIsBlank(lookupInputModifyingOp)) {
            List<String> tempList = new ArrayList<>();
            for (String val : valsToCheck) {
                String result = performStringChangingOperation(EscapeUtil.unescapeSpecialCharacters(val),
                        StringHandlerRule.StringOperator.valueOf(lookupInputModifyingOp));
                /**
                 * Some string operations can return a list, such as {@link StringHandlerRule.StringOperator.STR_PERM_COMB},
                 * account for that here, by examining the returned value, and if comma separated string, transform
                 * into a list and add each of the individual list members to valsToCheck
                 */
                List<String> retList = Context.transformArgumentToListObject(result);
                if (null != retList) {
                    tempList.addAll(retList);
                } else {
                    tempList.add(result);
                }
            }
            valsToCheck.clear();
            valsToCheck.addAll(tempList);
        }


        final Context context = lookupRule.assembleListContext(listName, pExtraParams);

        /**
         * Determine what should be the search field in the list to
         * look up against. Search field can be configured in the list name itself. By
         * default it will be {@link QueryableLookupRule#LIST_ENTRY_FLD_NAME}.
         */
        final String searchFieldFromListName = lookupRule.searchField(listName);
        final String searchField = CommonUtils.stringIsBlank(searchFieldFromListName)
                ? QueryableLookupRule.LIST_ENTRY_FLD_NAME : searchFieldFromListName;
        final Filter lookupFilter = prepareLookupFilter(listName, valsToCheck, operation, searchField);

        /**
         * To improve performance, see if this lookup was already seen, and
         * retrieve from cache.
         */
        LookupRuleExecutionResult cachedRes;
        final StringBuilder cacheKeySb = new StringBuilder();
        cacheKeySb.append(listName);
        cacheKeySb.append(lookupFilter.toString());
        cacheKeySb.append(operation.toString());
        final String searchResultCacheKey = cacheKeySb.toString();
        if (null != (cachedRes = lookupRule.checkAndRetrieveFromCache(searchResultCacheKey))) {
          return cachedRes;
        }

        /**
         * IMPORTANT: If this Context has been configured such that the configured list entries
         * behave as regular expressions via {@link QueryableLookupRule#isFieldValuesAreRegEx()},
         * then the lookup operation requested (EXACT_MATCH, PARTIAL_MATCH, etc..., except for LIST_DEFINED) has
         * no effect. See the documentation of {@link Context#findElement(SearchPath, Filter, TargetElements, Map)}
         * regarding pExtraParams Map. When the lookup operation is {@link Operation#LIST_DEFINED}, is the other condition
         * which causes the lookup entries to behave as regular expressions, for the reasons listed in the documentation
         * of {@link this#processMatchStyle(String, List, SearchResult, QueryableDataRule, String)}, refer to that method
         * for details.
         */
        if (lookupRule.isFieldValuesAreRegEx() || operation == Operation.LIST_DEFINED) {
            pExtraParams.put(QueryableDataRule.FOUND_ELEM_VAL_IS_REGEX, "1");
            if (!CommonUtils.stringIsBlank(matchStyle)) {
                pExtraParams.put(QueryableDataRule.PARTIAL_REGEX_MATCH, "1");
            }
        }

        logDebug("Beginning to perform lookup operation, context name is " + lookupRule.dataSourceName()
                + ", rule component name is " + lookupRule.name() + ". Lookup input is " + valsToCheck.toString()
                + ", and lookup list name is " + listName);
        SearchResult targetData = null;
        List<Context> ctxParts = partitionSearchContext(context);

        /**
         * If the search area got broken up as per configurations, search the area in parallel
         * to obtain results faster.
         */
        // Set when a runtime error is because of a list entry that couldn't be processed as a RegEx. This will happen at runtime
        // when list entries behave as RegEx patterns (I.e. lookupRule.isFieldValuesAreRegEx() == true) or LU op is LIST_DEFINED),
        // and the code had trouble compiling the entry as a RegEx pattern. Not to panic though, simply inspect the logs, look for what
        // the entry was that failed (usually will appear in the "PatternSyntaxException" section), escape the character(s)
        // in the list entry universe, then try again.
        Throwable regExThrown = null;
        if (ctxParts.size() > 1) {
            logDebug("Dividing and conquering lookup, by assigning one thread per partition.");
            Collection<Callable<SearchResult>> searchTasks = new ArrayList<>();
            boolean interruptHappened = false;
            for (Context c : ctxParts) {
                /**
                 * Ensure each task gets its own copy of pExtraParams Map, because it is not thread-safe. The other input
                 * arguments are immutable classes, hence can be shared freely w/o having to worry about synchronization.
                 */
                Map<String, String> p = new HashMap<>(pExtraParams);
                searchTasks.add(() -> c.findElement(c.startSearchPath(), lookupFilter, pTargetElems, p));
            }

            try {
                /**
                 * The below will return as soon as something is found. Any other searches going on at
                 * that time will get canceled.
                 * But what about the case when there are NO search hits? What happens then? Well, then
                 * the "targetData" variable below will be empty!!! Therefore we must handle this scenario
                 * appropriately anywhere "targetData" is referenced further below. This is just the nature
                 * of how CommonUtils.runTasksAsynchronouslyAndCancelOnFirstResult() behaves with respect
                 * to future tasks which all fail to yield any results. In {@link ConfigurableRuleExecutor}
                 * we have to deal with this scenario as well.
                 *
                 * Something to keep in mind here, we might get non-deterministic behavior for the same search
                 * being executed more than one time. If a partition were to cause issues, yet results were found before the trouble
                 * area of the partition was reached, no errors would get thrown. But if for exact same search we did reach trouble
                 * area, then we'd see the error thrown. This is not a race condition or anything like that. It means there's
                 * bad data configured in the lookup search context.
                 */
                final ExecutorService es = CommonUtils.autoSizedExecutor();
                targetData = CommonUtils.runTasksAsynchronouslyAndCancelOnFirstResult(es, searchTasks);
            } catch (InterruptedException e) {
                interruptHappened = true;
                throw new RuleException("There was a problem doing lookup on component " + lookupRule.name()
                        + " with list name " + listName, e);
            } catch (PatternSyntaxException e) {
                regExThrown = e;
            } finally {
                if (interruptHappened) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            logDebug("Look up being handled on the same current thread.");
            try {
                targetData = context.findElement(context.startSearchPath(), lookupFilter, pTargetElems, pExtraParams);
            } catch (PatternSyntaxException e) {
                regExThrown = e;
            }
        }

        if (null != regExThrown) {
            throw new RuleException("It appears one of the entries of list '" + listName + "' in component '" + lookupRule.name()
                    + "' could not be successfully parsed as a RegEx pattern. Inspect the log to see what the list entry in question"
                    + " is (Hint: search for 'Caused by: java.util.regex.PatternSyntaxException' somewhere below this "
                    + "message), fix it by escaping the offending character(s), then try again.", regExThrown);
        }

        /**
         * See {@link this#processMatchStyle(String, List, SearchResult, QueryableDataRule, String)} for details
         * on logic below.
         */
        if (operation == Operation.LIST_DEFINED && !CommonUtils.stringIsEmpty(matchStyle)) {
            targetData = processMatchStyle(matchStyle, valsToCheck, targetData, lookupRule, searchField);
        }

        /**
         * Sorting the targetData SearchResult object to match the order of values
         * (This is needed for proper behavior of biz requirement when the Permutation and Combination logic in
         * StringHandler class gets used. However doing for all since it is harmless to do so)
         */
        if (!targetData.isEmpty()) {
            if (targetData.get(lookupRule.dataSourceName()).asArray().size() > 1) {
                targetData = sortTargetElements(valsToCheck, targetData, lookupRule, searchField);
            }
        }

        /*
         * Get the list entry that matched, and store it in the rule result object,
         * along with other pertinent info. When the list has NOT configured an alternate output field,
         * then this and lookupOutputValue will be the same
         */
        String matchedString = (null != targetData && !targetData.isEmpty()) ?
                targetData.get(lookupRule.dataSourceName()).asArray()
                        .get(0).memberValue(searchField).stringRepresentation()
                : null;
        String outFieldName = lookupRule.outField(listName);
        final String outputVal;
        if (!CommonUtils.stringIsBlank(outFieldName)) {
            outputVal = (null != targetData && !targetData.isEmpty()) ?
                    targetData.get(lookupRule.dataSourceName()).asArray()
                            .get(0).memberValue(outFieldName).stringRepresentation()
                    : null;
        } else {
            throw new RuleException("The output field name of a lookup operation cannot be empty. Lookup list is " +
                    listName + ", component is " + lookupRule.name());
        }

        LookupRuleExecutionResult res = prepareResult(pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, targetData,
                listName, lookupRule, operation, matchStyle, valsToCheck, matchedString, outputVal);
        logDebug("LookupRule operation complete, used context '" + lookupRule.dataSourceName()
                + "', rule component name is " + lookupRule.name() + ". Result is " + res.toString());
        lookupRule.cacheResult(searchResultCacheKey, res);
        return res;
    }

    /**
     * This method sorts the targetData SearchResult object in the order in which the input values
     * (valsToCheckArr) is received
     */
    private SearchResult sortTargetElements(List<String> pSortByList,
                                            SearchResult pTargetData,
                                            QueryableLookupRule pLookupRule,
                                            String pSearchField) {
        Map<String, Context> res = new HashMap<>();
        MutableContext mcAry = ContextFactory.INSTANCE.obtainMutableContext("[]");
        if (pTargetData.isEmpty()) {
            return pTargetData;
        }

        for (String v : pSortByList) {
            for (int j = 0; j < pTargetData.get(pLookupRule.dataSourceName()).asArray().size(); j++) {
                if (v.equals(pTargetData.get(pLookupRule.dataSourceName()).asArray().get(j).memberValue(pSearchField).stringRepresentation())) {
                    mcAry.addEntryToArray(pTargetData.get(pLookupRule.dataSourceName()).asArray().get(j));
                    break;
                }
            }
        }
        res.put(pLookupRule.dataSourceName(), ContextFactory.INSTANCE.obtainContext(mcAry.stringRepresentation()));
        return SearchResult.createSearchResult(res);
    }

    /**
     * For multi-valued lookup items in the passed in list, replace {@link RuleConstants#BLANK_TOKEN} with nothing. The
     * {@link RuleConstants#BLANK_TOKEN} is used as a placeholder elsewhere in Rule API to represent Context API
     * element searches without fruition.
     */
    private void removeBlanks(List<String> pList) {
        List<String> tempList = pList.stream().map(e -> e.replaceAll(" " + RuleConstants.BLANK_TOKEN + " ", " "))
                .collect(Collectors.toList());
        pList.clear();
        pList.addAll(tempList);
    }


    private LookupRuleExecutionResult prepareResult(Context pCtx,
                                                    SearchPath pSearchPath,
                                                    Filter pFilter,
                                                    TargetElements pTargetElems,
                                                    Map<String, String> pExtraParams,
                                                    SearchResult pTargetData,
                                                    String pListName,
                                                    Rule pLuRule,
                                                    LookupRuleExecutor.Operation pLuOp,
                                                    String pMatchOpt,
                                                    List<String> pInputVal,
                                                    String pMatchedString,
                                                    String pOutVal) throws RuleException {
        Map<String, String> info = populateCommonResultProperties(
                pCtx, pSearchPath, pFilter, pTargetElems, pExtraParams, pTargetData);
        info.put(RuleConstants.ARG_LIST_NAME, pListName);
        info.put(RuleConstants.ARG_OPERATION, pLuOp.symbol());
        info.put(RuleConstants.MATCH_STYLE, pMatchOpt);
        info.put(RuleConstants.ARG_LEFT_OPERAND, pInputVal.stream().collect(Collectors.joining(",")));
        info.put(RuleConstants.ARG_LOOKUP_COMPONENT_NAME, pLuRule.name());
        info.put(RuleConstants.ARG_LEFT_OPERAND_SRC, pExtraParams.get(RuleConstants.ARG_LEFT_OPERAND_SRC));
        info.put(RuleConstants.LOOKUP_OUTPUT_VALUE, pOutVal);

        LookupRuleExecutionResult res
                = new LookupRuleExecutionResult((null != pTargetData && !pTargetData.isEmpty()),
                info, pMatchedString, pCtx, getOutputFieldMap());

        return res;
    }


    private Filter prepareLookupFilter(String pListName,
                                       List<String> pSearchVals,
                                       Operation pOp,
                                       final String pSearchFld)
            throws RuleException {
        StringBuffer sb = new StringBuffer();
        sb.append(QueryableLookupRule.LIST_NAME_FLD_NAME);
        sb.append("=");
        sb.append(pListName);
        sb.append(";");
        sb.append(pSearchFld);
        sb.append("=");
        List<String> newSearchVals = new ArrayList<>();
        for (String val : pSearchVals) {
            StringBuilder valSb = new StringBuilder();
            switch (pOp) {
                case EXACT_MATCH:
                    valSb.append(val);
                    break;
                case PARTIAL_MATCH:
                    valSb.append(WILD_CARD);
                    valSb.append(val);
                    valSb.append(WILD_CARD);
                    break;
                case BEGINS_WITH:
                    valSb.append(val);
                    valSb.append(WILD_CARD);
                    break;
                case LIST_DEFINED:
                    /**
                     * When operation is LIST_DEFINED, then the input value for the lookup remains
                     * as is. The matching behavior is applied instead on the configured lookup values as per the
                     * matchOption specified on the list name.
                     */
                    valSb.append(val);
                    break;
                default:
                    throw new RuleException("Encountered unsupported lookup match operation: " + pOp);
            }
            newSearchVals.add(valSb.toString());
        }
        sb.append(newSearchVals.stream().collect(Collectors.joining(",")));
        return Filter.valueOf(sb.toString());
    }


    private List<Context> partitionSearchContext(Context pCtx) {
        List<Context> rows = pCtx.memberValue(pCtx.startSearchPath().toString()).asArray();
        int threshold = 1000;
        final String partThreshVal;
        List<Context> partitions = new ArrayList();
        if (!CommonUtils.stringIsBlank(partThreshVal =
                AbstractRulesEngine.fetchConfigurationPropertyValue(RuleConstants.CONFIG_PROP_LU_CONTEXT_PARTITION_THRESHOLD))) {
            threshold = Integer.valueOf(partThreshVal);
        }

        /*
         * We have not met partition threshold, nothing to partition, return
         * a List but with just the single partition in it. Or parallelization as a whole
         * id disabled.
         */
        if (!AbstractRulesEngine.parallelizedRuleExecution() || null == rows || rows.size() < threshold) {
            partitions.add(pCtx);
            return partitions;
        }


        /*
         * We have met partition threshold, go ahead and partition
         */
        int partSize = 100;
        final String partSizeVal;
        if (!CommonUtils.stringIsBlank(partSizeVal =
                AbstractRulesEngine.fetchConfigurationPropertyValue(RuleConstants.CONFIG_PROP_LU_CONTEXT_PARTITION_SIZE))) {
            partSize = Integer.valueOf(partSizeVal);
        }

        MutableContext mc;
        MutableContext rowsMc = null;
        for (int i = 0; i < rows.size(); i++) {
            if (i % partSize == 0) {
                mc = ContextFactory.obtainMutableContext("{}");
                rowsMc = ContextFactory.obtainMutableContext("[]");
                mc.addMember(pCtx.startSearchPath().toString(), rowsMc);
                partitions.add(mc);
            }
            rowsMc.addEntryToArray(rows.get(i));
        }

        logDebug("Look up will be partitioned. Partitions created is " + partitions.size());
        return partitions;
    }


    /**
     * Will apply the match option to the lookup value retrieved from the list. Will return the first successful match only,
     * meaning results will be at most one, otherwise empty results is returned.
     * To further elaborate, when a match option has been specified in the list name, then it's a two-phase approach:
     * 1) Do a partial match of each configured list term anywhere in the passed in value to search (by
     * setting the REGEX flags before calling Context.findElement(), already was done above).
     * 2) Then once we have a candidate term, we apply the actual matchStyle requested to prepare the final result,
     *   which is done below in method "processMatchStyle()".
     *
     * @param pMatchStyle - Match option string to apply, internally will be converted to {@link MatchStyle} <code>enum</code>,
     *                    else throws exception if it's not a valid match option string
     * @param pLookupVals - The list of values to apply the match option to.
     * @param pSearchRes - The <code>SearchResult</code> object from phase 1 of lookup, which contains all the list
     *                   values that partially match in the lookup value.
     * @param pRule - The <code>Rule</code> object that encapsulates the lookup list <code>Context</code> object
     * @return - The results, if any found.
     * @throws RuleException - If the {@param pMatchStyle} string is not valid
     */
    private SearchResult processMatchStyle(String pMatchStyle, List<String> pLookupVals, SearchResult pSearchRes,
                                           QueryableDataRule pRule, final String pSearchFld)
            throws RuleException {

        SearchResult emptySearchRes = SearchResult.createSearchResult(Collections.emptyMap());
        if (null == pSearchRes || pSearchRes.isEmpty()) {
            return emptySearchRes;
        }
        Map<String, Context> res = new HashMap<>();
        MutableContext mc = ContextFactory.obtainMutableContext("{}");
        MutableContext mcAry = ContextFactory.obtainMutableContext("[]");
        mc.addMember(pRule.dataSourceName(), mcAry);
        MatchStyle matchStyle = MatchStyle.fromString(pMatchStyle);
        /**
         * If the lookup value is a comma-separated string, convert it to a list. Even if it's not, it will
         * be transformed into a list, but with just one value in it
         */
        for (Context c : pSearchRes.get(pRule.dataSourceName()).asArray()) {
            /**
             * Variable "listVal" is the lookup list value that matched the input, pLookupVals are the inputs
             * to the lookup.
             * TODO: Is it correct to be applying this to inputs for *all* lookup requests? This can be moved
             * TODO : to the lookup config themselves, E.g. LU||LIST_DEFINED||REMOVE_SPL_CHARS_COLLAPSE_SPACE
             */
            String listVal = c.memberValue(pSearchFld).stringRepresentation();
            listVal = performStringChangingOperation(listVal, StringHandlerRule.StringOperator.REMOVE_SPL_CHARS_COLLAPSE_SPACE);
            boolean match = false;
            for (String luVal : pLookupVals) {
                switch (matchStyle) {
                    case EXACT:
                        if (luVal.equals(listVal)) {
                            match = true;
                        }
                        break;
                    case PARTIAL:
                        if (luVal.indexOf(listVal) >= 0) {
                            match = true;
                        }
                        break;
                    case PARTIAL_WHOLE_PHRASE:
                        /**
                         * Build a regex that will match whole words only
                         */
                        StringBuilder regEx = new StringBuilder();
                        regEx.append("\\b");
                        regEx.append(listVal);
                        regEx.append("\\b");
                        if (AbstractRule.evaluateRegularExpression(luVal, regEx.toString()).evaluateResult()) {
                            match = true;
                        }
                        break;
                    case STARTS_WITH:
                        if (luVal.startsWith(listVal)) {
                            match = true;
                        }
                        break;
                    case ENDS_WITH:
                        if (luVal.endsWith(listVal)) {
                            match = true;
                        }
                        break;
                    default:
                        throw new RuleException("Unsupported list defined match option, was given: " + pMatchStyle);
                }


                /**
                 * Exit for loop at first match.
                 */
                if (match) {
                    break;
                }
            }

            if (match) {
                mcAry.addEntryToArray(c);
                res.put(pRule.dataSourceName(), mcAry);
                return SearchResult.createSearchResult(res);
            }
        }


        /*
         * If we reach here, it means that for the partially matched string during phase 1 of lookup list matching approach,
         * the matched option configured for the list did not yield any results.
         */
        return emptySearchRes;
    }


    /**
     * Encapsulates all the matching operations supported when looking
     * up a a value against a list.
     * {@link Operation#EXACT_MATCH} - the lookup input value has to match exactly a value in the lookup list
     * {@link Operation#PARTIAL_MATCH} - The lookup input value can match anywhere inside an entry of the lookup list. Think
     *   of it as the lookup input having wildcard at begging and end, E.g. "*lookup_input_value*".
     * {@link Operation#BEGINS_WITH} - As the name implies, the lookup input value is matched at beginning of entries of
     *   the lookup list
     * {@link Operation#LIST_DEFINED} - WThis signifies that the matching behavior is obtained by looking at the list name. See
     *   {@link QueryableLookupRule} for details.
     */
    enum Operation {
        EXACT_MATCH("EXACT_MATCH"),
        PARTIAL_MATCH("PARTIAL_MATCH"),
        BEGINS_WITH("BEGINS_WITH"),
        LIST_DEFINED("LIST_DEFINED");

        String symbol() {
            return symbol;
        }

        private String symbol;

        Operation(String pSymbol) {
            symbol = pSymbol;
        }

        /*
         * Checks which is the operation being requested, if it can't be explicitly determined, then exception
         * gets thrown
         */
        static Operation fromSymbol(String pSymbol) throws IllegalArgumentException {
            for (Operation c: Operation.values()) {
                if (c.symbol().equals(pSymbol)) {
                    return c;
                }
            }

            throw new IllegalArgumentException("Invalid/unsupported operation specified, got: " + pSymbol);
        }
    }

    enum MatchStyle {
        EXACT("exact"),
        PARTIAL("partial"),
        PARTIAL_WHOLE_PHRASE("partialWholePhrase"),
        STARTS_WITH("startsWith"),
        ENDS_WITH("endsWith");


        private String matchStyle;

        MatchStyle(String pMatchStyle) {
            matchStyle = pMatchStyle;
        }

        public String matchStyle() {
            return matchStyle;
        }

        static MatchStyle fromString(String pMatchStyle)  {
            for (MatchStyle c: MatchStyle.values()) {
                if (c.matchStyle().equals(pMatchStyle)) {
                    return c;
                }
            }

            throw new IllegalArgumentException("Invalid/unsupported match option specified, got: " + pMatchStyle
                    + ". Only supported match options are "
                    + Arrays.stream(MatchStyle.values()).map(e -> e.matchStyle()).collect(Collectors.joining("','", "'", "'")));
        }
    }


    static class LookupRuleExecutionResult extends TextOutputRuleExecutionResult {

        LookupRuleExecutionResult(
                boolean pVal,
                Map<String, String> pParams,
                String pOutput,
                Context pInputCtx,
                Map<String, Rule> pOutFldMap) throws RuleException {
            super(pVal, pParams, pOutput, pInputCtx, pOutFldMap);
        }


        /**
         * Display name/value pairs relevant to a lookup operation.
         *
         * @return - A {@link Context} object.
         */
        public Context evaluateResultAsContext() {
            return evaluateResultAsContext(true);
        }


        /**
         *
         * @param pSuppressNotApplicableFields
         * @return
         */
        @Override
        public Context evaluateResultAsContext(boolean pSuppressNotApplicableFields) {

            MutableContext mc = ContextFactory.obtainMutableContext("{}");
            try {
                populateOutputData(mc);
            } catch (RuleException e) {
                throw new IllegalArgumentException("Problem populating output fields for component "
                        + getParams().get(RuleConstants.ARG_LOOKUP_COMPONENT_NAME), e);
            }

            String termSource = getParams().get(RuleConstants.ARG_LEFT_OPERAND_SRC);
            StringBuilder listSearched = new StringBuilder();
            listSearched.append(getParams().get(RuleConstants.ARG_LOOKUP_COMPONENT_NAME));
            listSearched.append("-");
            listSearched.append(getParams().get(RuleConstants.ARG_LIST_NAME));
            if (!evaluateResult() && pSuppressNotApplicableFields) {
                termSource = REFER_MSG;
                listSearched = new StringBuilder(REFER_MSG);
            }


            addNameValuePairToContext("listSearched", listSearched.toString(), mc);

            String searchedTerm = REFER_MSG;
            if (evaluateResult() || !pSuppressNotApplicableFields) {
                searchedTerm = getParams().get(RuleConstants.ARG_LEFT_OPERAND);
            }
            addNameValuePairToContext("termSearched", searchedTerm, mc);

            /**
             * Format for display the input elements that were searched against lookup list, and their values. Class
             * {@link QueryableRuleConfigurationData} gave us that info as a long string, with a set of input elements
             * per unique input value found, and within each set, the elements are separated using delimiter
             * {@link RuleConstants#INPUT_ELEMS_DELIM}.
             * We rely on the fact that {@link QueryableRuleConfigurationData#optimizeLookupOperations(List, Context)}
             * imposed the same ordering on the input element sets, as their corresponding values. This way while iterating
             * over input element sets, we can use the index of each input set to get the corresponding value for that input
             * element set.
             */
            String[] inputElemSets = termSource.split(RuleConstants.INPUT_SET_DELIM);
            /**
             * Need to use overloaded version of {@link String#split(String, int)}  because we want to catch
             * any trailing blank space. If we didn't do this, then by design the Java API uses limit = 0, which removes
             * trailing spaces. Read the javadoc of {@link String#split(String, int)} for details.
             */
            String[] searchedTerms = searchedTerm.split(",", -1);

            MutableContext mcInputElemAry = ContextFactory.obtainMutableContext("[]");
            for (int i = 0; i < inputElemSets.length ; i++) {
                /**
                 * Because ordering of input element sets and values is same, use the same index to access
                 * both. See comment above to trace where this ordering was imposed, namely in
                 * {@link QueryableRuleConfigurationData#optimizeLookupOperations(List, Context)}
                 */
                String inputElemSet = inputElemSets[i];
                String inputElemVal = searchedTerms[i];
                String[] inputElems = inputElemSet.split(RuleConstants.INPUT_ELEMS_DELIM);
                for (String inputElem : inputElems) {
                    MutableContext mcInputElemNode = ContextFactory.obtainMutableContext("{}");
                    addNameValuePairToContext("inputElement", inputElem, mcInputElemNode);
                    addNameValuePairToContext("value", CommonUtils.stringIsBlank(inputElemVal) ? BLANK : inputElemVal,
                            mcInputElemNode);
                    mcInputElemAry.addEntryToArray(mcInputElemNode);
                }

            }
            mc.addMember("termSourceElement", mcInputElemAry);

            addNameValuePairToContext("lookupOperation", getParams().get(RuleConstants.ARG_OPERATION), mc);
            String matchOption = getParams().get(RuleConstants.MATCH_STYLE);
            if (!CommonUtils.stringIsBlank(matchOption)) {
                addNameValuePairToContext("matchOption", matchOption, mc);
            }
            addNameValuePairToContext("wasTermFound", (evaluateResult() ? "Yes" : "No"), mc);

            String valueMatched;
            try {
                valueMatched = evaluateResult() ? evaluateResultAsString() : RuleConstants.NOT_APPLICABLE;
            } catch (RuleException e) {
                valueMatched = "error_obtaining_matched_value";
            }
            addNameValuePairToContext("valueMatched", valueMatched, mc);

            String outVal;
            if (!CommonUtils.stringIsBlank(outVal = getParams().get(RuleConstants.LOOKUP_OUTPUT_VALUE))) {
                addNameValuePairToContext(RuleConstants.LOOKUP_OUTPUT_VALUE, outVal, mc);
            }

            if (!pSuppressNotApplicableFields) {
                mc.addMember("executionTimeInSeconds",
                        ContextFactory.obtainContext(String.valueOf(totalExecutionTimeInSeconds())));
            }

            /*
             * Return immutable version of the mutable context.
             */
            return ContextFactory.obtainContext(mc.stringRepresentation());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toString());
            sb.append("\nName of list rule object: ");
            sb.append(getParams().get(RuleConstants.ARG_LOOKUP_COMPONENT_NAME));
            sb.append("\nName of list searched: ");
            sb.append(getParams().get(RuleConstants.ARG_LIST_NAME));
            sb.append("\nTerm searched: \"");
            sb.append(getParams().get(RuleConstants.ARG_LEFT_OPERAND));
            sb.append("\"");
            sb.append("\nOperation performed: ");
            sb.append(getParams().get(RuleConstants.ARG_OPERATION));
            if (getParams().containsKey(RuleConstants.MATCH_STYLE)) {
                sb.append("\nList-defined match option: ");
                sb.append(getParams().get(RuleConstants.MATCH_STYLE));
            }
            sb.append("\nWas it found? ");
            try {
                sb.append(evaluateResult() ? "Yes" : "No");
                if (evaluateResult()) {
                    sb.append("\nValue matched: ");
                    sb.append(evaluateResultAsString());
                }
            } catch (RuleException e) {
                sb.append("Problem displaying whether there was lookup hit or not and displaying matched value: ");
                sb.append(e.toString());
                AbstractRule.logError(e);
            }
            return sb.toString();
        }

        public String lookupOutputValue() throws RuleException {
            String outVal;
            if (CommonUtils.stringIsBlank(outVal = getParams().get(RuleConstants.LOOKUP_OUTPUT_VALUE))) {
                outVal = evaluateResultAsString();
            }

            return outVal;
        }
    }

}
