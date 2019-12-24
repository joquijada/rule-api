package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.SelectionCriteria;

import java.util.Set;

/**
 * This class is the parent to all elements that perform a reduction operation on a collection. What does reduction
 * operation mean? In general terms it's an operation that takes a collection of items as inputs, and performs an operation
 * which output is a single value, for example calculating the average of a list of numbers, or calculating
 * the total number of items in a list, or the sum of all the quantities yielded by an element from each list entry, etc. This
 * idea is borrowed from Java's, located <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/package-summary.html#Reduction">here</a>
 * Created by QuijadaJ on 11/9/2017.
 */
abstract class AbstractReductionElem<T> extends AbstractCollectionActorElem<T> {
    static final String ELEM_PROP_NAME = "element";

    private final SelectionCriteria element;
    AbstractReductionElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, pReqElems);
        if (pRuleExpCtx.containsElement(ELEM_PROP_NAME)) {
            element = SelectionCriteria.valueOf(pRuleExpCtx.memberValue(ELEM_PROP_NAME).stringRepresentation());
        } else {
            element = null;
        }
    }

    /*
     * Getters
     */
    SelectionCriteria getElement() {
        return element;
    }
}
