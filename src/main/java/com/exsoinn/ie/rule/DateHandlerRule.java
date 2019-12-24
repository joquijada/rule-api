package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.*;

import java.util.Map;
import java.util.Set;

/**
 * Created by QuijadaJ on 10/16/2017.
 */
public class DateHandlerRule extends AbstractRule {
    DateHandlerRule(String pUniqueName) {
        super(pUniqueName);
    }


    @Override
    public  <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        return null;
    }


    private OperandAndSearchResult fetchOperand(Context pContext,
                                SearchPath pSearchPath,
                                Filter pFilter,
                                TargetElements pTargetElems,
                                Map<String, String> pExtraParams,
                                                String pKey) throws RuleException {
        SearchResult targetData = null;
        String operand;
        Set<Map.Entry<String, Context>> entries;

        if (null != pExtraParams
                && pExtraParams.containsKey(pKey)) {
            operand = pExtraParams.get(pKey);
        } else if (null != pContext) {
            targetData = pContext.findElement(pSearchPath, pFilter, pTargetElems, pExtraParams);
            entries = targetData.entrySet();
            operand = entries.iterator().next().getValue().stringRepresentation();
        } else {
            throw new RuleException("Unable to obtain operand " + pKey + ", component name is " + name());
        }

        return new OperandAndSearchResult(operand, targetData);
    }

    private static class OperandAndSearchResult {
        private final String operand;
        private final SearchResult searchResult;

        OperandAndSearchResult(String pOp, SearchResult pSr) {
            operand = pOp;
            searchResult = pSr;
        }
    }

    enum Operator {
        EQUALS,
        NOT_EQUALS,
        GT_THAN,
        LESS_THAN;

        static Operator fromString(String opr) {
            for (Operator c: Operator.values()) {
                if (c.name().equals(opr)) {
                    return c;
                }
            }
            throw new IllegalArgumentException("Invalid/unsupported operator specified, got: " + opr);
        }
    }
}
