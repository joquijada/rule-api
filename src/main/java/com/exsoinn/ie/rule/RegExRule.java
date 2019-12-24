package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.EscapeUtil;
import com.exsoinn.util.epf.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * See the documentation for {@link RegExRule#applyBody(Context, SearchPath, Filter, TargetElements, Map)} to
 * gain an understanding of how this class operates.
 *
 * Created by QuijadaJ on 4/19/2017.
 */
class RegExRule extends AbstractRule {
    private final Pattern pattern;
    private final String targetString;
    final static String GENERIC_CACHE_KEY = "genericRegExRule";



    /*
     * Constructors
     */
    RegExRule(String pRegEx) {
        this(pRegEx, null);
    }

    RegExRule(String pRegEx, String pUniqueName) {
        this(pRegEx, pUniqueName, null);
    }
    RegExRule(String pRegEx, String pUniqueName, String pTargetStr) {
        super(pUniqueName);
        if (CommonUtils.stringIsBlank(pRegEx)) {
            pattern = null;
        } else {
            pattern = Pattern.compile(pRegEx, Pattern.CASE_INSENSITIVE);
        }

        targetString= pTargetStr;
    }



    @Override
    public RuleExecutionResult apply(String pDataStr,
                                     String pElemSearchPath,
                                     Map<String, String> pElemFilter,
                                     Set<String> pTargetElems,
                                     Map<String, String> pExtraParams) throws RuleException {
        Context c = ContextFactory.INSTANCE.obtainContext(pDataStr);
        return apply(c, convertToSearchPath(pElemSearchPath), convertToFilter(pElemFilter),
                convertToTargetElements(pTargetElems), pExtraParams);
    }


    /**
     * Perform a regular expression evaluation. The regular expression can be an instance member, which implies
     * a dedicated object will exist to evaluating only that regular expression, or the regular expression can be passed in
     * via the {@param pExtraParams} of the {@link RegExRule#apply(Context, SearchPath, Filter, TargetElements, Map)} method,
     * by using key {@link RuleConstants#ARG_REGEX_PATTERN}. When the regular expression is an instance member specified
     * during object construction, if a regex is passed in the {@param pExtraParams} Map, then the passed in regular
     * expression takes precedence, and the regular expression instance member gets ignored.
     * The target value against which regular expression gets evaluated can be passed in the same {@param pExtraParams} {@code Map},
     * use key {@link RuleConstants#ARG_REGEX_TARGET_VAL}, or it can come from the passed in {@code Context} object to
     * the {@link RegExRule#apply(Context, SearchPath, Filter, TargetElements, Map)} method. The {@param pSearchPath},
     * {@param pFilter} and {@param @param pTargetElems} of the same method will be used to identify the target string of
     * the regular expression evaluation. When the target value is found in the {@param pExtraParams} param, then
     * this supersedes obtaining it from the {@param pContext} argument.
     * @param pContext - Use this {@code Context} to find the target string of the regex evaluation. Ignored if the target
     *                 string is found in {@param pExtraParams}
     * @param pSearchPath - To build search criteria when {@code Context} is passed in and target string not found
     *                    in {@param pExtraParams}. Read documentation of {@link Context#findElement(SearchPath, Filter, TargetElements, Map)}
     *                    to learn more how to build search criteria, and examples
     * @param pFilter - Like {@param pSearchPath}, used for building search criteria.
     * @param pTargetElems - Like {@param pSearchPath}, used for building search criteria.
     * @param pExtraParams - {@code Map} that contains arguments to use for regex operation. This can contain the regular
     *                     expression and/or the target string to use during evaluation.
     * @return - A {@link BooleanRuleExecutionResult} object that is true if there was a match, false otherwise. Moreover,
     *   whatever matched in the input string can be retrieved obtained via {@link RuleExecutionResult#evaluateResultAsString()}
     *   method, which will contain a {@link Utilities#MULTI_VAL_DELIM} delimited string of all the things that the
     *   regular expression matched against.
     * @throws RuleException - If something goes wrong
     */
    @Override
    public <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        SearchResult targetData;

        // Get the target string, either passed in as argument or stored in instance member
        String targetStr = targetString;
        if (null != pExtraParams && pExtraParams.containsKey(RuleConstants.ARG_REGEX_TARGET_VAL)) {
            targetStr = pExtraParams.get(RuleConstants.ARG_REGEX_TARGET_VAL);
        }
        if (!CommonUtils.stringIsBlank(targetStr)) {
            // Must do this even though we did not do a search, to keep "populateCommonResultProperties()" happy,
            // because it expects a non-null SearchResult in order to report the target data of rule evaluation
            targetData = buildFakeSearchResult(targetStr);
        } else {
            targetData = pContext.findElement(pSearchPath, pFilter, pTargetElems, pExtraParams);
        }

        Set<Map.Entry<String, Context>> entries = targetData.entrySet();
        Context targetVal = entries.iterator().next().getValue();
        if (!targetVal.isPrimitive() || entries.size() > 1) {
            throw new IllegalArgumentException("Expected search results to contain only one primitive, "
                    + ", but instead found: " + targetVal.stringRepresentation());
        }
        String targetStrVal = targetVal.stringRepresentation();
        targetStrVal = targetStrVal.replaceAll(EscapeUtil.ESCAPE_SPACE, " ");

        Pattern patternToUse = pattern;
        /*
         * Get RegEx from pExtraParams if none was specified at object construction.
         */
        if (pExtraParams.containsKey(RuleConstants.ARG_REGEX_PATTERN)) {
            patternToUse = Pattern.compile(pExtraParams.get(RuleConstants.ARG_REGEX_PATTERN), Pattern.CASE_INSENSITIVE);
        }
        if (null == patternToUse) {
            throw new RuleException("A regular expression pattern is required.");
        }

        Matcher m = patternToUse.matcher(targetStrVal);
        Map<String, String> info = populateCommonResultProperties(
                pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, targetData);
        info.put(RuleConstants.MATCH_PATTERN, patternToUse.toString());
        info.put(RuleConstants.LEFT_HAND_OPERAND, targetStrVal);
        info.put(RuleConstants.OPERATION, patternToUse.toString());

        String matchedStrings = matchedStrings(m);
        return new RuleSetOperationResult(!CommonUtils.stringIsBlank(matchedStrings),
                info, matchedStrings, pContext, getOutputFieldMap());
    }


    private String matchedStrings(Matcher pMatcher) {
        List<String> matchedList = new ArrayList<>();
        while(pMatcher.find()) {
            matchedList.add(pMatcher.group());
        }

        if (matchedList.isEmpty()) {
            return null;
        } else {
            return matchedList.stream().collect(Collectors.joining(Utilities.MULTI_VAL_DELIM));
        }
    }
}
