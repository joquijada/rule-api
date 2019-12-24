package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.SelectionCriteria;

import java.util.EnumSet;
import java.util.Set;

/**
 * Evaluates the sub-expression, which is expected to be a Context (otherwise
 * a {@link IllegalArgumentException} gets thrown), and applies the specified element selection criteria
 * to the sub-expression result, and returns that to the caller.
 *
 * Created by QuijadaJ on 11/9/2017.
 */
class SelectorElem extends AbstractRuleExpression<Context, Context> {
    private final SelectionCriteria element;
    SelectorElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx,
                new SetBuilder().addAll(pReqElems).add(AbstractReductionElem.ELEM_PROP_NAME).build());
        element = SelectionCriteria.valueOf(pRuleExpCtx.memberValue(AbstractReductionElem.ELEM_PROP_NAME)
                .stringRepresentation());
    }

    @Override
    Set<Element> allowedSubElements() {
        return EnumSet.of(Element.MAX);
    }

    @Override
    public Context evaluate(Context pInput) {
        Object obj = subElements().get(0).evaluate(pInput);
        if (!(obj instanceof Context)) {
            throw new IllegalArgumentException("Element '" + getElementName() + "' expects sub-expression "
                    + getElementContent() + " to yield a Context. Instead got "
                    + (obj == null ? "NULL" : obj.getClass().getName()));
        }
        Context ctx = (Context) obj;
        return ctx.findElement(element, null).firstResult();
    }
}