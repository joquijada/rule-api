package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.IdentifiedByName;

/**
 * Created by QuijadaJ on 5/9/2017.
 */
public class DuplicateCacheEntryException extends RuleException {
    public DuplicateCacheEntryException(String pMsg, Throwable t) {
        super(pMsg, t);
    }

    public <T extends IdentifiedByName> DuplicateCacheEntryException(String pObjName, T pFoundObj) {
        this("Object name " + pObjName + ", of type " + pFoundObj.getClass().getName() + " is already in use by object "
                + pFoundObj.name(), (Throwable)null);
    }
}
