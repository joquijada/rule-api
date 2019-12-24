package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;
import java.util.List;
import java.util.Set;

/**
 * Iterates over a collection, and returns true if at least the specified number of entries
 * exist in the collection that satisfy the given expression.
 *
 * Created by QuijadaJ on 11/2/2017.
 */
final class AtLeastElem extends AbstractQuantifierElem {
    AtLeastElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, pReqElems);
    }

    @Override
    public Boolean evaluate(Context pInput) {
        // Get the collection of Context's to iterate over
        List<Context> ctxList = pInput.findElement(getCollection(), null).entrySet().iterator().next().getValue().asArray();

        int passCnt = 0;
        for (Context c : ctxList) {
            Context ctxToEval = c;
            Object resObj = subElements().get(0).evaluate(ctxToEval);
            validateResultIsBoolean(resObj);
            Boolean res = (Boolean) resObj;
            if (res) {
                ++passCnt;
            }

            if (passCnt >= getNumber()) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }
}
