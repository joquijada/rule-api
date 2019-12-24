package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.SelectionCriteria;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Parent abstract class to any element acts upon a collection of items
 *
 * The required parameters are:
 *   - The {@link com.exsoinn.util.epf.SelectionCriteria} of the collection to iterate
 *
 * Created by QuijadaJ on 10/31/2017.
 */
abstract class AbstractCollectionActorElem<T> extends AbstractRuleExpression<T, Context> {
    private static final String COLLECTION_SEL_CRIT_PROP_NAME = "collection";
    private final SelectionCriteria collection;

    AbstractCollectionActorElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx,
                new SetBuilder<>().add(COLLECTION_SEL_CRIT_PROP_NAME).addAll(pReqElems).build());
        collection =
                SelectionCriteria.valueOf(pRuleExpCtx.memberValue(COLLECTION_SEL_CRIT_PROP_NAME).stringRepresentation());
    }


    @Override
    Set<Element> allowedSubElements() {
        return EnumSet.of(Element.AND, Element.OR, Element.NOT, Element.GT_THAN, Element.EQUALS, Element.LESS_THAN,
                Element.GT_THAN_OR_EQ, Element.AT_LEAST, Element.TOTAL);
    }


    /**
     * Gets the collection of Context's to iterate over by
     * @param pInput
     * @return
     */
    List<Context> prepareCollection(Context pInput) {
        return pInput.findElement(getCollection(), null).entrySet().iterator().next().getValue().asArray();
    }


    /*
     * Getters/Setters
     */
    public SelectionCriteria getCollection() {
        return collection;
    }
}
