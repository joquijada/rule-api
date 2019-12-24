package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.SearchResult;
import java.util.List;
import java.util.Set;

/**
 * From the Context in the list, selects the one which contains the maximum value (assumed to be a number)
 * in the element that the {@link this#ELEM_PROP_NAME} selection criteria yields.
 * Optionally this element can contain a sub-expression which yields a boolean, in which case only the collection
 * entries for which the sub-expression evaluates to true are considered towards the final max calculation.
 *
 * Created by QuijadaJ on 11/9/2017.
 */
final class MaxElem extends AbstractReductionElem<Context> {
    MaxElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, new SetBuilder<>().add(ELEM_PROP_NAME).addAll(pReqElems).build());
    }

    @Override
    public Context evaluate(Context pInput) {
        // Get the collection of Context's to iterate over
        List<Context> ctxList = prepareCollection(pInput);

        int maxThusFar = 0;
        Context winnerCtx = null;

        /**
         * Iterate over each context in the list, and evaluate the sub-expression if any, then keep
         * track of the highest number seen thus far, and the context associated with such a number. This context
         * will be the one selected.
         */
        for (Context c : ctxList) {
            Context ctxToEval = c;
            Boolean res;
            if (!subElements().isEmpty()) {
                Object resObj = subElements().get(0).evaluate(ctxToEval);
                validateResultIsBoolean(resObj);
                res = (Boolean) resObj;
            } else {
                res = Boolean.TRUE;
            }
            if (res) {
                SearchResult sr = c.findElement(getElement(), null);
                Context ctxRes = sr.firstResult();
                if (!ctxRes.isPrimitive()) {
                    throw new IllegalArgumentException("Element '" + getElementName() +"' with content "
                            + getElementContent().stringRepresentation() + " yielded a non-primitive value for element selection criteria "
                            + getElement() + ". Expecting only a number, instead got " + ctxRes.stringRepresentation());
                }

                /**
                 * If the below evaluates to true, we have a new max. Note: Use of {@link Integer#intValue()} avoids
                 * penalty incurring auto-unboxing
                 */
                int curNum;
                if ((curNum = Integer.valueOf(ctxRes.stringRepresentation()).intValue()) > maxThusFar) {
                    winnerCtx = c;
                    maxThusFar = curNum;
                }
            }
        }

        // If no element was greater than '0', then NULL context gets returned
        return winnerCtx;
    }
}
