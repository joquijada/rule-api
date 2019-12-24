package com.exsoinn.ie.rule.data;

import com.exsoinn.util.epf.Context;

import java.util.Map;

/**
 * Created by QuijadaJ on 9/27/2017.
 */
public class ContextMapSource implements Source<UniqueKeyQuery, ContextResult> {
    private final Map<String, Context> dataMap;

    ContextMapSource(Map<String, Context> pDataMap) {
        dataMap = pDataMap;
    }

    @Override
    public ContextResult retrieveData(UniqueKeyQuery pQry) {
        return new ContextResult(dataMap.get(pQry.key()));
    }

    @Override
    public ContextResult insertData(UniqueKeyQuery pQry) {
        return null;
    }

    @Override
    public String name() {
        return null;
    }
}
