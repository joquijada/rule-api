package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.EscapeUtil;
import com.exsoinn.util.epf.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by MohideenS on 4/26/2017.
 *
 * EQUALS:
 * NOT_EQUALS:
 * EQUALS_IGNORE_CASE:
 * IS_UPPER:
 * IS_LOWER:
 * IS_CAMEL_CASE:
 * IS_TITLE_CASE:
 * IS_NULL:
 * IS_NOT_NULL:
 * STRNG_LNGTH_LESS_THAN:
 * STRNG_LNGTH_LESS_THAN_EQLS:
 * STRNG_LNGTH_GRTR_THAN:
 * STRNG_LNGTH_GRTR_THAN_EQLS:
 * STRNG_LNGTH_EQUALS:
 * STRNG_LNGTH_LESS_THAN_IGNR_SPACE:`
 * STRNG_LNGTH_LESS_THAN_EQLS_IGNR_SPACE:
 * STRNG_LNGTH_GRTR_THAN_IGNR_SPACE:
 * STRNG_LNGTH_GRTR_THAN_EQLS_IGNR_SPACE:
 * STRNG_LNGTH_EQUALS_IGNR_SPACE:
 * STRNG_LNGTH_LESS_THAN_TRIM:
 * STRNG_LNGTH_LESS_THAN_EQLS_TRIM:
 * STRNG_LNGTH_GRTR_THAN_TRIM:
 * STRNG_LNGTH_GRTR_THAN_EQLS_TRIM:
 * STRNG_LNGTH_EQUALS_TRIM:
 *
 */
class StringHandlerRule extends AbstractRule{
    private final Operator operator;
    private final StringOperator stringOperator;
    final static String GENERIC_CACHE_KEY = "genericStringHandlerRule";
    final static String SUB_STR = "SUB_STR";
    private final int subStrBeginIndex, subStrEndIndex;

    /*
     * Constructors
     */
    StringHandlerRule(String pOperator, String pStrOperator) {
        this(pOperator, pStrOperator, null);
    }

    StringHandlerRule(String pOperator, String pStrOperator, String pUniqueName) {
        super(pUniqueName);

        int beginIndex = 0, endIndex = 0;
        if (null!=pStrOperator && pStrOperator.startsWith(SUB_STR)) {
            String[] valArr = pStrOperator.split("_");
            beginIndex = (valArr[2]!=null ? Integer.parseInt(valArr[2]) : 0);
            endIndex = (valArr[3]!=null ? Integer.parseInt(valArr[3]) : 0);
            pStrOperator = SUB_STR;
        }

        this.subStrBeginIndex = beginIndex;
        this.subStrEndIndex = endIndex;
        this.operator = Operator.fromString(pOperator);
        this.stringOperator = StringOperator.getStringOperator(pStrOperator);
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

    @Override
    public  <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        StringBuffer sb;
        String leftHandOperand;
        String rightHandOperand;
        SearchResult targetData = null;
        Set<Map.Entry<String, Context>> entries;

        /**
         * Figure out from where to get the left hand operand. Passed in arguments take precedence.
         */
        if (null != pExtraParams
                && pExtraParams.containsKey(RuleConstants.ARG_LEFT_OPERAND)) {
            leftHandOperand = pExtraParams.get(RuleConstants.ARG_LEFT_OPERAND);
        } else if (null != pContext) {
            targetData = pContext.findElement(pSearchPath, pFilter, pTargetElems, pExtraParams);
            entries = targetData.entrySet();
            leftHandOperand = entries.iterator().next().getValue().stringRepresentation();
        } else {
            throw new RuleException("Was not given a left hand operand, component name is " + name());
        }

        rightHandOperand = (null == pExtraParams || pExtraParams.isEmpty())
                ? null : pExtraParams.get(RuleConstants.ARG_RIGHT_OPERAND);

        leftHandOperand = applyStringChangingOperation(leftHandOperand);
        boolean res = applyStringOperation(leftHandOperand, rightHandOperand);

        Map<String, String> info = populateCommonResultProperties(
                pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, targetData);
        sb = new StringBuffer();
        sb.append("leftHandOperand: ");
        sb.append(leftHandOperand);
        sb.append(", rightHandOperand: ");
        sb.append(rightHandOperand);
        sb.append(", operator: ");
        sb.append(operator);
        sb.append(", string operation: ");
        sb.append(stringOperator);
        info.put(RuleConstants.STRING_HANDLER_VAL, sb.toString());
        info.put(RuleConstants.LEFT_HAND_OPERAND, leftHandOperand);
        info.put(RuleConstants.OPERATION, operator.toString());
        info.put(RuleConstants.RIGHT_HAND_OPERAND, rightHandOperand);
        if (null != stringOperator) {
            info.put(RuleConstants.LEFT_OPERAND_PREPROCESSOR, stringOperator.toString());
        }

        /**
         * Why are we giving the left operand to use as the output value when calling evaluateResultAstring()? In case
         * a left op pre-processing operation was requested, we want to see the result of doing so. Remember
         * that the changed string got stored in leftHandOperand.
         */
        return new RuleSetOperationResult(res, info, leftHandOperand, pContext, getOutputFieldMap());
    }

    private String applyStringChangingOperation(final String pIn) throws RuleException {
        if (null == stringOperator) {
            return pIn;
        }

        String out;
        switch (stringOperator) {
            case TRIM:
                out = pIn.trim();
                break;
            case LOWER_CASE:
                out = pIn.toLowerCase();
                break;
            case UPPER_CASE:
                out = pIn.toUpperCase();
                break;
            case REMOVE_SPACE:
                out = pIn.replaceAll("\\s", "");
                break;
            case REMOVE_ALPHA:
                out = pIn.replaceAll("[A-Za-z]", "");
                break;
            case REMOVE_ALPHA_AND_SPACE:
                out = pIn.replaceAll("[A-Za-z\\s]", "");
                break;
            case REMOVE_NUMERIC:
                out = pIn.replaceAll("[0-9]", "");
                break;
            case REMOVE_NUMERIC_AND_SPACE:
                out = pIn.replaceAll("[0-9\\s]", "");
                break;
            case REMOVE_ALPHANUMERIC:
                out = pIn.replaceAll("[A-Za-z0-9]", "");
                break;
            case REMOVE_SPECIAL_CHARS:
                out = pIn.replaceAll("[^A-Za-z0-9]", "");
                break;
            case RETAIN_NUMBERS_ONLY:
                out = pIn.replaceAll("[^0-9]+", "");
                break;
            case REMOVE_SPL_CHARS_WITH_SPACE:
                out = pIn.replaceAll("[^\\w]", " ");
                break;
            case REMOVE_SPL_CHARS_COLLAPSE_SPACE:
                /**
                 * Does the following: Trim, remove special characters, collapse 2 or more spaces to a single space
                 * Note to developer: Do NOT alter order of sequence of these statements, otherwise
                 *   it will break. Order has significance depending on where in the string the special characters
                 *   are located!!!
                 */
                out = pIn.replaceAll("_", "");

                // Save the spaces first, otherwise the \W replacement further below will eat them
                out = out.replaceAll("\\s", EscapeUtil.ESCAPE_SPACE);
                out = out.replaceAll("(" + EscapeUtil.ESCAPE_SPACE + "){2,}", EscapeUtil.ESCAPE_SPACE);
                out = out.replaceAll("\\W", "");

                /**
                 * After removing special characters, can end up with consecutive spaces, re-apply
                 * multi-space collapsing op
                 */
                out = out.replaceAll("(" + EscapeUtil.ESCAPE_SPACE + "){2,}", EscapeUtil.ESCAPE_SPACE);

                // Finally restore the spaces
                out = out.replaceAll(EscapeUtil.ESCAPE_SPACE, " ");
                out = out.trim();
                break;
            case STR_PERM_COMB:
                List<String> listResult = getWordCombinations(Arrays.asList(pIn.split("\\s")));
                Collections.reverse(listResult);
                out = listResult.stream().collect(Collectors.joining(RuleConstants.MULTI_VAL_STR_DELIM));
                break;
            case SORT_STRINGS_NUMERICALLY:
                List<String> words = splitIntoWords(pIn);
                if (words.size() <= 1) {
                    // Only one or zero words passed in, nothing to sort, therefore return input as is
                    out = pIn;
                    break;
                }

                /**
                 * Find out what the delimiter was in the string as it was passed. Need this delimiter further below
                 * when sending back the sorted numbers. This is done by building regular expression that contains
                 * the first word in the passed in input, and then seeing what the non-word character was
                 * that immediately follows this word.
                 * Note: I tried combining first word of passed pIn with \W, but for some reason Java API gives the entire
                 *   thing as matched group, even though I was putting only \W in parenthesis. The only way I could get
                 *   it to work was by having (\W) by itself in the RegEx. Then I have to get first matched element,
                 *   in case input had more than one word separator
                 */
                TextOutputRuleExecutionResult delimRes =
                        (TextOutputRuleExecutionResult) AbstractRule.evaluateRegularExpression(pIn, "(\\W)");
                String delimiter = delimRes.evaluateResultAsString().split(Utilities.MULTI_VAL_DELIM)[0];
                /**
                 *  Now sort the assumed to be numbers. Any error formatting will get thrown as runtime
                 *  {@link NumberFormatException}. Use the same delimiter that was contained in the raw
                 *  input (the delimiter was captured above)
                 */
                out = words.stream().map(e -> Integer.valueOf(e)).sorted().map(e -> e.toString())
                                .collect(Collectors.joining(delimiter));
                break;
            case LAST_WORD:
                List<String> tmplList = splitIntoWords(pIn);
                out = "";
                if (!tmplList.isEmpty()) {
                    out = tmplList.get(tmplList.size() - 1);
                }
                break;
            case FIRST_WORD:
                out = "";
                List<String> tmplList2 = splitIntoWords(pIn);
                if (!tmplList2.isEmpty()) {
                    out = tmplList2.get(0);
                }
                break;
            case SECOND_WORD:
                out = "";
                List<String> tmplList3 = splitIntoWords(pIn);
                if (tmplList3.size() > 1) {
                    out = tmplList3.get(1);
                }
                break;
            case SUB_STR:
                out = pIn.substring(subStrBeginIndex,subStrEndIndex);
                break;
            default:
                throw new RuleException("Encountered unsupported string operator: " + stringOperator);
        }

        return out;
    }


    /**
     * Apply a regex that will split the passed in string into words, if possible. Else it returns a list with only
     * one member, namely the passed in string {@param pStr}.
     * @param pStr
     * @return
     * @throws RuleException
     */
    private List<String> splitIntoWords(final String pStr) throws RuleException {
        // Use regex below to obtain all the whole words found in the passed in string.
        StringBuilder regExSb = new StringBuilder();
        regExSb.append("\\b");
        regExSb.append("(\\w+?)");
        regExSb.append("\\b");
        TextOutputRuleExecutionResult res =
                (TextOutputRuleExecutionResult) AbstractRule.evaluateRegularExpression(pStr, regExSb.toString());

        List<String> words =  Arrays.asList(res.evaluateResultAsString().split(Pattern.quote(Utilities.MULTI_VAL_DELIM)));
        return words;
    }

    private boolean applyStringOperation(String pLeftHandOperand, String pRightHandOperand) throws RuleException {
        boolean res;
        switch(operator) {
            case EQUALS:
                res = pLeftHandOperand.equals(pRightHandOperand);
                break;
            case NOT_EQUALS:
                res = !pLeftHandOperand.equals(pRightHandOperand);
                break;
            case EQUALS_IGNORE_CASE:
                res = pLeftHandOperand.equalsIgnoreCase(pRightHandOperand);
                break;
            case IS_UPPER:
                res = pLeftHandOperand.equals(pLeftHandOperand.toUpperCase());
                break;
            case IS_LOWER:
                res = pLeftHandOperand.equals(pLeftHandOperand.toLowerCase());
                break;
            case IS_CAMEL_CASE:
                res = pLeftHandOperand.matches("([a-z]+[A-Z]+\\\\w+)+");
                break;
            case IS_TITLE_CASE:
                res = checkIfTitleCase(pLeftHandOperand);
                break;
            case IS_BLANK:
                res = CommonUtils.stringIsBlank(pLeftHandOperand);
                break;
            case IS_NULL:
                res = (null == pLeftHandOperand);
                break;
            case IS_NOT_NULL:
                res = (null != pLeftHandOperand);
                break;
            case STRNG_LNGTH_LESS_THAN:
                res = (pLeftHandOperand.length() < Integer.parseInt(pRightHandOperand));
                break;
            case STRNG_LNGTH_LESS_THAN_EQLS:
                res = (pLeftHandOperand.length() <= Integer.parseInt(pRightHandOperand));
                break;
            case STRNG_LNGTH_GRTR_THAN:
                res = (pLeftHandOperand.length() > Integer.parseInt(pRightHandOperand));
                break;
            case STRNG_LNGTH_GRTR_THAN_EQLS:
                res = (pLeftHandOperand.length() >= Integer.parseInt(pRightHandOperand));
                break;
            case STRNG_LNGTH_EQUALS:
                res = (pLeftHandOperand.length() == Integer.parseInt(pRightHandOperand));
                break;
            case STRNG_LNGTH_NOT_EQUALS:
                res = (pLeftHandOperand.length() != Integer.parseInt(pRightHandOperand));
                break;
            case CONTAINS_ALPHA_CHARS:
                res = (pLeftHandOperand.replaceAll("[A-Za-z]", "").length() != pLeftHandOperand.length());
                break;
            case CONTAINS_NUMERICS:
                res = (pLeftHandOperand.replaceAll("[0-9]", "").length() != pLeftHandOperand.length());
                break;
            case CONTAINS_SPL_CHARS:
                res = (pLeftHandOperand.replaceAll("[^a-zA-Z0-9]", "").length() != pLeftHandOperand.length());
                break;
            case STARTS_WITH:
                res = pLeftHandOperand.startsWith(pRightHandOperand);
                break;
            case ENDS_WITH:
                res = pLeftHandOperand.endsWith(pRightHandOperand);
                break;
            case NO_OP:
                res = true;
                break;
            default:
                throw new RuleException("Encountered unsupported operator: " + operator);
        }

        return res;
    }

    enum StringOperator {
        UPPER_CASE,
        LOWER_CASE,
        REMOVE_SPACE,
        TRIM,
        REMOVE_ALPHA,
        REMOVE_ALPHA_AND_SPACE,
        REMOVE_NUMERIC,
        REMOVE_NUMERIC_AND_SPACE,
        REMOVE_ALPHANUMERIC,
        REMOVE_SPECIAL_CHARS,
        RETAIN_NUMBERS_ONLY,
        REMOVE_SPL_CHARS_WITH_SPACE,
        REMOVE_SPL_CHARS_COLLAPSE_SPACE,
        STR_PERM_COMB,
        SORT_STRINGS_NUMERICALLY,
        LAST_WORD,
        FIRST_WORD,
        SECOND_WORD,
        SUB_STR;

        static StringOperator getStringOperator(String strOpr) {
            if (null != strOpr) {
                for (StringOperator c : StringOperator.values()) {
                    if (c.name().equals(strOpr)) {
                        return c;
                    }
                }
            } else {
                return null;
            }
            throw new IllegalArgumentException("Invalid/unsupported string operator specified, got: " + strOpr);
        }
    }

    enum Operator {
        EQUALS,
        NOT_EQUALS,
        EQUALS_IGNORE_CASE,
        IS_UPPER,
        IS_LOWER,
        IS_CAMEL_CASE,
        IS_TITLE_CASE,
        IS_BLANK,
        IS_NULL,
        IS_NOT_NULL,
        STRNG_LNGTH_LESS_THAN,
        STRNG_LNGTH_LESS_THAN_EQLS,
        STRNG_LNGTH_GRTR_THAN,
        STRNG_LNGTH_GRTR_THAN_EQLS,
        STRNG_LNGTH_EQUALS,
        STRNG_LNGTH_NOT_EQUALS,
        CONTAINS_ALPHA_CHARS,
        CONTAINS_NUMERICS,
        CONTAINS_SPL_CHARS,
        STARTS_WITH,
        ENDS_WITH,
        NO_OP;

        static Operator fromString(String opr) {
            for (Operator c: Operator.values()) {
                if (c.name().equals(opr)) {
                    return c;
                }
            }
            throw new IllegalArgumentException("Invalid/unsupported operator specified, got: " + opr);
        }
    }

    private static boolean checkIfTitleCase(String str) {
        String[] strArr = str.split("\\s+");

        for (String strVal : strArr) {
            boolean flag = false;
            for (char chars : strVal.toCharArray()){
                if (!flag && Character.isUpperCase(chars)) {
                    flag = true;
                } else if (!Character.isLowerCase(chars)){
                    return false;
                }
            }
        }
        return true;
    }

    /*
    * Get meaningful word combinations of the string value passed (Left to Right)
    */
    private static List<String> getWordCombinations(List<String> inputStringParts) {
        List<String> wordComboLst = new LinkedList<String>();
        int size = inputStringParts.size();
        int threshold = Double.valueOf(Math.pow(2, size)).intValue() - 1;

        for (int i = 1; i <= threshold; ++i) {
            LinkedList<String> indvComboLst = new LinkedList<>();
            int count = 0;
            int clonedI = i;

            while (count <= (size - 1)) {
                if ((clonedI & 1) != 0) {
                    indvComboLst.addLast(inputStringParts.get(count));
                }

                clonedI = clonedI >>> 1;
                ++count;
            }

            List list = indvComboLst;
            String result = String.join(" ", list);
            wordComboLst.add(result);

        }

        return wordComboLst;
    }

}
