package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;

import java.util.EnumSet;
import java.util.Set;

/**
 * Top most element in a rule expressions. Entry point to evaluate expression begins
 * here.
 *
 * Created by QuijadaJ on 10/11/2017.
 */
final class RootElem extends AbstractRuleExpression<Object, Context> {
    RootElem(String pElementName, Context pRuleExpCtx) {
        super(pElementName, pRuleExpCtx, null);
    }

    @Override
    public Object evaluate(Context pRuleExpCtx) {
        return subElements().get(0).evaluate(pRuleExpCtx);
    }

    @Override
    Set<Element> allowedSubElements() {
        return EnumSet.allOf(Element.class);
    }
}
