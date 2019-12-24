package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.SearchResult;
import com.exsoinn.util.epf.SelectionCriteria;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


/**
 * Created by QuijadaJ on 10/12/2017.
 */
final class SearchPathElem extends AbstractValueElem {
    private final SelectionCriteria selectionCriteria;

    SearchPathElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, pReqElems);
        selectionCriteria = SelectionCriteria.valueOf(super.getValue());
    }



    /**
     * Need to override because need to return the results of the selection criteria search done on the
     * context. The getValue() gives the selection criteria specified in the rule expression, which is of no good
     * to us; we need the search results that the selection criteria translates into.
     *
     * @param pCtx - Context against which selection criteria will be searched.
     * @return - The string that the selection criteria search yielded.
     */
    @Override
    String valueForComputation(Context pCtx) {
        SearchResult sr = pCtx.findElement(selectionCriteria, null);
        Map.Entry<String, Context> found = sr.entrySet().iterator().next();
        return found.getValue().stringRepresentation();
    }




    @Override
    Set<Element> allowedSubElements() {
        return EnumSet.noneOf(Element.class);
    }
}
