package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * Created by QuijadaJ on 10/12/2017.
 */
final class ConstantElem extends AbstractValueElem {


    ConstantElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, pReqElems);
    }


    @Override
    Set<Element> allowedSubElements() {
        return EnumSet.of(Element.NULL);
    }
}
