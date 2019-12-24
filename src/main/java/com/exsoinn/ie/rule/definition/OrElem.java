package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;

import java.util.Set;


/**
 * Created by QuijadaJ on 10/10/2017.
 */
final class OrElem extends AbstractOperatorElem<Boolean, Context> {


    OrElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, pReqElems);
    }

    @Override
    public Boolean evaluate(Context pInput) {
        for (RuleExpression e : subElements()) {
            Object resObj = e.evaluate(pInput);
            validateResultIsBoolean(resObj);
            Boolean res = (Boolean) resObj;
            if (res) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

}
