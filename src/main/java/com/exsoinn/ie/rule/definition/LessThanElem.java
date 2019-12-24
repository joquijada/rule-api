package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;

import java.util.Set;

/**
 * Created by QuijadaJ on 10/17/2017.
 */
final class LessThanElem extends AbstractComparatorElem {
    LessThanElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, pReqElems);
    }

    @Override
    public Boolean evaluate(Context pInput) {
        Object obj1 = subElements().get(0).evaluate(pInput);
        Object obj2 = subElements().get(1).evaluate(pInput);
        int comparisonRes = compareObjects(obj1, obj2);
        return comparisonRes < 0 ? Boolean.TRUE : Boolean.FALSE;
    }
}
