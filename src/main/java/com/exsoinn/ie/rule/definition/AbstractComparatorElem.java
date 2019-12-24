package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;

import java.util.EnumSet;
import java.util.Set;

/**
 * This class can be extended by any element which deals with comparison operations like "equals",
 * "less than", "greater than or equals", etc... The {@link this#compareObjects(Object, Object)} throws runtime
 * {@link IllegalArgumentException} if the objects to compare do not both implement the {@link Comparable} interface.
 *
 * Created by QuijadaJ on 10/16/2017.
 */
abstract class AbstractComparatorElem extends AbstractRuleExpression<Boolean, Context>  {
    AbstractComparatorElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, pReqElems);
    }


    @Override
    Set<Element> allowedSubElements() {
        return EnumSet.of(Element.CONSTANT, Element.SEARCH_PATH, Element.TOTAL, Element.DEFAULT);
    }



    /**
     * Encapsulates all the inelegant logic to ascertain that both passed in elements are comparable, and if so,
     * do the comparisons. The return value is the same that {@link Comparable#compareTo(Object)} would return,
     * read that method's documentation for details.
     *
     * @param pObj1 - Object 1 to use in comparison, must implement {@link Comparable}
     * @param pObj2 - Object 2 to use in comparison, must implement {@link Comparable}
     * @return - Same as what {@link Comparable#compareTo(Object)} would return.
     */
    int compareObjects(Object pObj1, Object pObj2) {
        /**
         * Make sure both objects implement the {@link Comparable} interface.
         */
        if (!(pObj1 instanceof Comparable) || !(pObj2 instanceof Comparable)) {
            StringBuilder sb = new StringBuilder();
            sb.append(pObj1 instanceof Comparable ? pObj2.toString() + ", of class " + pObj2.getClass().getName()
                    : "");
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(!(pObj1 instanceof Comparable) ? pObj1.toString() + ", of class " + pObj1.getClass().getName()
                    : "");
            sb.append(" does/do not implement " + Comparable.class.getName());
            throw new IllegalArgumentException("Both objects need to implement the " + Comparable.class.getName()
                    + " interface. " + sb.toString());
        }

        Comparable<Object> comp1 = (Comparable<Object>) pObj1;
        Comparable<Object> comp2 = (Comparable<Object>) pObj2;
        return comp1.compareTo(comp2);
    }

}
