package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by QuijadaJ on 4/19/2017.
 */
class RelOpRule extends AbstractRule {
    private final Operation operation;
    private final int leftOperand;
    private final int rightOperand;
    final static int IGNORE_INT = RuleConstants.IGNORE_INT;
    final static String GENERIC_CACHE_KEY = "genericRelOpRule";

    RelOpRule(String pOp) {
        this(pOp, null);
    }

    /**
     * Because the left and right operand are primitive "int" type, can't assign to NULL, so used
     * alternative which is to set to a reserved value that simulates NULL int, the reserved value
     * being RuleConstants.IGNORE_INT.
     * @param pOp
     * @param pUniqueName
     */
    RelOpRule(String pOp, String pUniqueName) {
        this(pOp, pUniqueName, IGNORE_INT, IGNORE_INT);
    }
    RelOpRule(String pOp, String pUniqueName, int pLeftOperand, int pRightOperand) {
        super(pUniqueName);
        if (null != pOp) {
            operation = Operation.fromSymbol(pOp);
        } else {
            operation = null;
        }
        leftOperand = pLeftOperand;
        rightOperand = pRightOperand;
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
        try {
            SearchResult targetData;
            // Get the left operand
            int leftHandOperand = leftOperand;
            if (leftHandOperand == IGNORE_INT) {
                if (null != pExtraParams && pExtraParams.containsKey(RuleConstants.ARG_LEFT_OPERAND)) {
                    // Left operand passed as argument...
                    leftHandOperand = Integer.valueOf(pExtraParams.get(RuleConstants.ARG_LEFT_OPERAND));
                    targetData = buildFakeSearchResult(Integer.toString(leftHandOperand));
                } else {
                    // ...otherwise get left operand from input record
                    targetData = pContext.findElement(pSearchPath, pFilter, pTargetElems, pExtraParams);
                }
            } else {
                // Get left operand from instance member initialized during rule object construction (assigned as
                // default value to local variable "leftHandOperand" above)
                targetData = buildFakeSearchResult(Integer.toString(leftHandOperand));
            }

            Set<Map.Entry<String, Context>> entries = targetData.entrySet();
            leftHandOperand = convertToIntegerObject(entries.iterator().next().getValue().stringRepresentation());


            // Figure out the right operand to use
            int rightHandOperand = rightOperand;
            if (rightHandOperand == IGNORE_INT) {
                // Right operand passed as argument, otherwise use the one defined in instance member
                rightHandOperand = convertToIntegerObject(pExtraParams.get(RuleConstants.ARG_RIGHT_OPERAND));
            }


            // Get the OP
            Operation op = operation;
            if (null != pExtraParams && pExtraParams.containsKey(RuleConstants.ARG_OPERATION)) {
                op = Operation.fromSymbol(pExtraParams.get(RuleConstants.ARG_OPERATION));
            }


            boolean res;
            /*
             * It would be nice if Java provided, w/o the need for JavaScript, the capability to
             * dynamically evaluate a text expression, which would avoid these case statements. For now going
             * with it as is.
             */
            switch(op) {
                case EQUALS:
                    res = leftHandOperand == rightHandOperand;
                    break;
                case GT:
                    res = leftHandOperand > rightHandOperand;
                    break;
                case GT_OR_EQUALS:
                    res = leftHandOperand >= rightHandOperand;
                    break;
                case LT:
                    res = leftHandOperand < rightHandOperand;
                    break;
                case LT_OR_EQUALS:
                    res = leftHandOperand <= rightHandOperand;
                    break;
                case NOT_EQUALS:
                    res = leftHandOperand != rightHandOperand;
                    break;
                default:
                    throw new RuleException("Encountered unsupported operation: " + op);
            }

            Map<String, String> info = populateCommonResultProperties(
                    pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, targetData);
            info.put(RuleConstants.OPERATION, op.toString());
            info.put(RuleConstants.LEFT_HAND_OPERAND, String.valueOf(leftHandOperand));
            info.put(RuleConstants.RIGHT_HAND_OPERAND, String.valueOf(rightHandOperand));
            return new RuleSetOperationResult(res, info, null, pContext, getOutputFieldMap());
        } catch (Throwable thrown) {
            throw new RuleException("Problem executing relational operation rule", thrown);
        }
    }


    /*
     * Encapsulates all the operations supported.
     */
    enum Operation {
        EQUALS("=="),
        GT(">"),
        LT("<"),
        GT_OR_EQUALS(">="),
        LT_OR_EQUALS("<="),
        NOT_EQUALS("!=");

        String symbol() {
            return symbol;
        }

        private String symbol;


        Operation(String pSymbol) {
            symbol = pSymbol;
        }

        static Operation fromSymbol(String pSymbol) {
            for (Operation c: Operation.values()) {
                if (c.symbol().equals(pSymbol)) {
                    return c;
                }
            }
            throw new IllegalArgumentException("Invalid/unsupported operation specified, got: " + pSymbol);
        }
    }

    private int convertToIntegerObject(String str) throws NumberFormatException {
        return Integer.valueOf(str);

    }
}
