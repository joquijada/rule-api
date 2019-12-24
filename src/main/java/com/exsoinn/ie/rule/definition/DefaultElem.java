package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;

import java.util.EnumSet;
import java.util.Set;

/**
 * Evaluates two sub-expressions. If expression 1 is NULL, then it returns result of second sub-expression
 * evaluation, else sub-expression 1 evaluation result is returned.
 *
 * Created by QuijadaJ on 11/9/2017.
 */
final class DefaultElem extends AbstractRuleExpression<Object, Context> {
    DefaultElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, pReqElems);
    }

    @Override
    public Object evaluate(Context pInput) {
        Object obj1 = subElements().get(0).evaluate(pInput);
        Object obj2 = subElements().get(1).evaluate(pInput);

        if (null == obj1 || NULL_STR.equalsIgnoreCase(obj1.toString())) {
            return obj2;
        }
        return obj1;
    }

    @Override
    Set<Element> allowedSubElements() {
        return EnumSet.of(Element.CONSTANT, Element.SEARCH_PATH);
    }
}
