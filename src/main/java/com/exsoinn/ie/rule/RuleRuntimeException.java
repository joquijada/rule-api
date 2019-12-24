package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Context;

/**
 * Created by QuijadaJ on 7/25/2017.
 */
public class RuleRuntimeException extends RuntimeException {
    private final Context context;
    RuleRuntimeException(Context pCtx, Throwable pThrown) {
        super(pThrown);
        context = pCtx;
    }


    /*
     * Getters/Setters
     */
    public Context getContext() {
        return context;
    }
}
