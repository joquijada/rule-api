package com.exsoinn.ie.rule.definition;


import com.exsoinn.util.epf.Context;
import java.util.EnumSet;
import java.util.Set;

/**
 * Created by QuijadaJ on 10/10/2017.
 */
abstract class AbstractOperatorElem<T, U> extends AbstractRuleExpression<T, U> {

    AbstractOperatorElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, pReqElems);
    }

    @Override
    Set<Element> allowedSubElements() {
        return EnumSet.allOf(Element.class);
    }

}
