package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.Builder;
import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.Filter;
import com.exsoinn.util.epf.SearchPath;
import com.exsoinn.util.epf.TargetElements;

import java.util.Map;

/**
 * Created by QuijadaJ on 10/26/2017.
 */
abstract class AbstractBuilder<T extends Rule> implements Builder<T> {


    // Optional parameters
    private String alternateSourceContextMapKey = null;

    /**
     * Child classes can use this optional parameter to designate the key in the extra params Map
     * passed in {@link ContextAugmentingRule#apply(Context, SearchPath, Filter, TargetElements, Map)} from where
     * to get the Context object to operate on when a rule component is given to get the value of the field to add.
     * @param pKey
     * @return - A {@link ContextAugmentingRule.Builder} object
     */
    public Builder<T> alternateSourceContextMapKey(String pKey) {
        alternateSourceContextMapKey = pKey;
        return this;
    }


    /*
     * Getters
     */
    public String getAlternateSourceContextMapKey() {
        return alternateSourceContextMapKey;
    }
}
