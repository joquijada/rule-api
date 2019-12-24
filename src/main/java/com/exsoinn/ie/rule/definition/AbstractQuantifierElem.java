package com.exsoinn.ie.rule.definition;

import com.exsoinn.util.epf.Context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by QuijadaJ on 11/2/2017.
 */
abstract class AbstractQuantifierElem extends AbstractCollectionActorElem<Boolean> {
    private final int number;
    private final static String NUMBER_PROP_NAME = "number";
    AbstractQuantifierElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, new SetBuilder().add(NUMBER_PROP_NAME).addAll(pReqElems).build());
        number = Integer.valueOf(pRuleExpCtx.memberValue(NUMBER_PROP_NAME).stringRepresentation()).intValue();
    }


    /*
     * Getters/Setters
     */
    public int getNumber() {
        return number;
    }

}
