package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;

import java.util.List;
import java.util.Set;

/**
 * Simply gets the total number of the collection entries for which the expression
 * evaluates to true.
 *
 * Created by QuijadaJ on 11/3/2017.
 */
final class TotalElem extends AbstractReductionElem<Integer> {
    TotalElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, pReqElems);
    }

    @Override
    public Integer evaluate(Context pInput) {
        // Get the collection of Context's to iterate over
        List<Context> ctxList = prepareCollection(pInput);

        int passCnt = 0;
        for (Context c : ctxList) {
            Context ctxToEval = c;
            Object resObj = subElements().get(0).evaluate(ctxToEval);
            validateResultIsBoolean(resObj);
            Boolean res = (Boolean) resObj;
            if (res) {
                ++passCnt;
            }
        }

        return Integer.valueOf(passCnt);
    }
}
