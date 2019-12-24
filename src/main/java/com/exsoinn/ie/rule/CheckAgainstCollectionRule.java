package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.epf.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Checks a value against a list of values
 * Created by QuijadaJ on 4/19/2017.
 */
class CheckAgainstCollectionRule extends AbstractRule {
    private final List<String> range;
    private final Operation operation;
    private final String valueToCheck;
    final static String GENERIC_CACHE_KEY = "genericCheckAgainstCollectionRule";


    CheckAgainstCollectionRule() {
        this(null, null, null, null);
    }
    CheckAgainstCollectionRule(List<String> pRange, String pUniqueName, String pOp, String pValToCheck) {
        super(pUniqueName);
        /*
         * Make defensive copy of mutable passed in range List. Remember we want to keep this class immutable. A client
         * could maliciously or unwittingly pass a list and then modify it, which would violate the immutability contract.
         */
        if (null != pRange) {
            range = new ArrayList<>(pRange);
        } else {
            range = new ArrayList<>();
        }
        if (!CommonUtils.stringIsBlank(pOp)) {
            operation = Operation.fromSymbol(pOp);
        } else {
            // Assign IN operation as default  if none provided
            operation = Operation.IN;
        }
        valueToCheck = pValToCheck;
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
        SearchResult targetData;

        /*
         * The value to check can be configured as instance member, or can be passed in as argument in the
         * pExtraParams Map
         */
        String valToCheck = valueToCheck;
        if (null != pExtraParams && pExtraParams.containsKey(RuleConstants.ARG_VAL_TO_CHECK)) {
            valToCheck = pExtraParams.get(RuleConstants.ARG_VAL_TO_CHECK);
        }

        /*
         * If "valToCheck" is blank, it means it wasn't defined during init and wasn't passed in
         * as argument either, in which case search for it in pContext (I.e. the input record)
         */

        if (!CommonUtils.stringIsBlank(valToCheck)) {
            // Must do this even though we did not do a search, to keep "populateCommonResultProperties()" happy,
            // because it expects a non-null SearchResult in order to report the target data of rule evaluation
            targetData = buildFakeSearchResult(valToCheck);
        } else {
            if (null == pContext) {
                throw new RuleException("Was given a NULL/blank value to check, and Context object to search "
                        + "was NULL as well. Check rule configurations");
            }
            targetData = pContext.findElement(pSearchPath, pFilter, pTargetElems, pExtraParams);
        }


        // Collect operator; either use the one specified at object construction, or get from method argument
        Operation op = operation;
        if (null != pExtraParams && pExtraParams.containsKey(RuleConstants.ARG_OPERATION)) {
            op = Operation.fromSymbol(pExtraParams.get(RuleConstants.ARG_OPERATION));
        }

        // Get the range
        List<String> rangeToUse = range;
        if (null != pExtraParams && pExtraParams.containsKey(RuleConstants.ARG_RANGE)) {
            rangeToUse = Arrays.stream(pExtraParams.get(RuleConstants.ARG_RANGE).split(",")).collect(Collectors.toList());
        }


        Set<Map.Entry<String, Context>> entries = targetData.entrySet();
        Context valueToCheck = entries.iterator().next().getValue();

        boolean res;
        switch(op) {
            case IN:
                res = rangeToUse.contains(valueToCheck.stringRepresentation());
                break;
            case NOT_IN:
                res = !rangeToUse.contains(valueToCheck.stringRepresentation());
                break;
            default:
                throw new RuleException("Encountered unsupported operation: " + op);
        }

        Map<String, String> info = populateCommonResultProperties(
                pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, targetData);
        info.put(RuleConstants.RANGE, rangeToUse.toString());
        return new BooleanRuleExecutionResult(res, info, pContext, getOutputFieldMap());
    }

    /*
     * Encapsulates all the operations supported.
     */
    enum Operation {
        IN("IN"),
        NOT_IN("NOT_IN");

        String symbol() {
            return symbol;
        }

        private String symbol;


        Operation(String pSymbol) {
            symbol = pSymbol;
        }

        /*
         * Checks which is the operation being requested, if it can't be explictily determined, then exception
         * gets thrown
         */
        static Operation fromSymbol(String pSymbol) {
            for (Operation c: Operation.values()) {
                if (c.symbol().equals(pSymbol)) {
                    return c;
                }
            }

            throw new IllegalArgumentException("Invalid/unsupported operation specified, got: " + pSymbol);
        }
    }
}
