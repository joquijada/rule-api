package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;

import java.util.Set;

/**
 * Created by QuijadaJ on 10/11/2017.
 */
final class NotElem extends AbstractOperatorElem<Boolean, Context> {
    NotElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, pReqElems);
    }

    @Override
    public Boolean evaluate(Context pInput) {
        RuleExpression e = subElements().get(0);
        Object resObj = e.evaluate(pInput);
        validateResultIsBoolean(resObj);
        Boolean res = (Boolean) resObj;
        if (res) {
            return Boolean.FALSE;
        }

        return Boolean.FALSE;
    }
}

