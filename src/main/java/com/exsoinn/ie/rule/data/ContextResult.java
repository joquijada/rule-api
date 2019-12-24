package com.exsoinn.ie.rule.data;

import com.exsoinn.util.epf.Context;

/**
 * Created by QuijadaJ on 9/27/2017.
 */
public class ContextResult implements Result<Context> {
    private final Context context;

    ContextResult(Context pCtx) {
        context = pCtx;
    }

    @Override
    public Context obtainData() {
        return null;
    }
}
