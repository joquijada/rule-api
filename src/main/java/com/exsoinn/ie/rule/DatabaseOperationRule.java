package com.exsoinn.ie.rule;

import com.exsoinn.ie.rule.data.DatabaseSource;
import com.exsoinn.ie.rule.data.SourceException;
import com.exsoinn.ie.rule.data.SqlQuery;
import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.epf.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Component which is meant to be a dedicated object to execute one and only one query against one and only one
 * database source. If you need to execute a different query, instantiate a new object. In other words it's a one-to-one
 * relationship between query and {@link DatabaseOperationRule}, and incidentally data source as well.
 * Created by QuijadaJ on 11/12/2017.
 */
class DatabaseOperationRule extends AbstractRule {
    private final DatabaseSource source;
    private final String query;
    private final SelectionCriteria parameterSelectionCriteria;
    private final boolean unwrapContextArrayResults;
    private final QueryableDataRule selectionCriteriaSource;
    private final Map<String, String> columnTranslator;


    public static class Builder extends AbstractRule.Builder<DatabaseOperationRule, Builder> {
        // Required parameters
        private final String dataSourceName;
        private final String query;

        // Optional parameters
        private String parameterSelectionCriteria = null;
        private Map<String, String> columnTranslator = null;


        Builder(String pName, String pDataSrcName, String pQry) {
            super(pName);
            dataSourceName = pDataSrcName;
            query = pQry;
        }

        public Builder parameterSelectionCriteria(String pSelCrit) {
            parameterSelectionCriteria = pSelCrit;
            return getThis();
        }

        public Builder columnTranslator(Map<String, String> pMap) {
            if (null != pMap) {
                columnTranslator = new HashMap<>(pMap);
            }
            return this;
        }

        @Override
        Builder getThis() {
            return this;
        }

        @Override
        public DatabaseOperationRule build() {
            try {
                Rule r = new DatabaseOperationRule(this);
                storeInRuleByNameCache(r);
                return (DatabaseOperationRule) r;
            } catch (RuleException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }


    DatabaseOperationRule(Builder pBuilder) {
        super(pBuilder.getName());
        try {
            source = lookupDatabaseSourceByName(pBuilder.dataSourceName);
        } catch (RuleNameNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        query = pBuilder.query;
        if (!CommonUtils.stringIsBlank(pBuilder.parameterSelectionCriteria)) {
            parameterSelectionCriteria = SelectionCriteria.valueOf(pBuilder.parameterSelectionCriteria);
        } else {
            parameterSelectionCriteria = null;
        }
        unwrapContextArrayResults = pBuilder.isUnwrapContextArrayResults();
        selectionCriteriaSource = pBuilder.getSelectionCriteriaSource();
        columnTranslator = pBuilder.columnTranslator;
    }


    @Override
    public <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        SelectionCriteria qryParamSc = parameterSelectionCriteria;
        /**
         * When the optional "selectionCriteriaSource" has been specified, then the "parameterSelectionCriteria"
         * is taken to look into "selectionCriteriaSource" to find the selection criteria to use to find
         * the query input parameters.
         */
        if (null != selectionCriteriaSource) {
            SearchResult foundSelCrit = selectionCriteriaSource.context().findElement(parameterSelectionCriteria, null);
            qryParamSc = SelectionCriteria.valueOf(foundSelCrit.entrySet().iterator().next().getValue().stringRepresentation());
        }

        List<String> qryParams = new ArrayList<>();
        SearchResult sr = SearchResult.emptySearchResult();
        if (null != qryParamSc) {
            sr = pContext.findElement(qryParamSc, null);
            qryParams = prepareQueryParameters(sr);
        }

        SqlQuery qryObj = new SqlQuery.Builder(query)
                .parameters(qryParams)
                .columnTranslator(columnTranslator)
                .build();
        Context qryResCtx;
        try {
            qryResCtx = source.retrieveData(qryObj);
        } catch (SourceException e) {
            throw convertToRuleException("Rule component " + name() + "experience a problem during database source operation. "
                    + "The param selection criteria was " + qryParamSc.toString() + ", the source name is "
                    + source.name(), e);
        }

        /**
         * This logic figures out if boolean result flag should be "true" or "false", by basically checking
         * if non-empty results were found.
         */
        final boolean resultFlag;
        if (qryResCtx.isArray() && !qryResCtx.asArray().isEmpty()) {
            resultFlag = true;
        } else if (qryResCtx.isRecursible() && !qryResCtx.entrySet().isEmpty()) {
            resultFlag = true;
        } else if (qryResCtx.isPrimitive()) {
            resultFlag = true;
        } else {
            resultFlag = false;
        }

        /**
         * If the results were given as an array that contains just one entry, unwrap it
         * if this component was configured to do so via "unwrapContextArrayResults"
         */
        final Context finalResCtx;
        if (unwrapContextArrayResults && qryResCtx.isArray() && qryResCtx.asArray().size() == 1) {
            finalResCtx = qryResCtx.asArray().get(0);
        } else {
            finalResCtx = qryResCtx;
        }

        Map<String, String> info = populateCommonResultProperties(
                pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, sr);
        return new AbstractRuleExecutionResult(info, pContext, null) {
            // Ensure Context returned is immutable
            private final Context retCtx = ContextFactory.obtainContext(finalResCtx.stringRepresentation());
            @Override
            public boolean evaluateResult() {
                return resultFlag;
            }

            @Override
            public String evaluateResultAsString() throws RuleException {
                return retCtx.stringRepresentation();
            }

            @Override
            public Context evaluateResultAsContext() {
                return retCtx;
            }
        };
    }


    /**
     * Does come validation on SearchResult passed in, and then builds a list of strings
     * where each represents a query parameter.
     * @param pRes
     * @return
     */
    private List<String> prepareQueryParameters(SearchResult pRes) {
        Set<Map.Entry<String, Context>> srEnts = pRes.entrySet();
        if (srEnts.size() != 1) {
            throw new IllegalArgumentException("Did not get exactly one search result to be used as query input parameter: "
                    + (null != pRes && !pRes.isEmpty() ? pRes.entrySet().stream()
                    .collect(Collectors.toMap(k -> k, v -> v.getValue().stringRepresentation())).entrySet().stream()
                    .map(Map.Entry::toString)
                    .collect(Collectors.joining("\n")) : "ZERO RESULTS"));
        }

        Context resCtx = srEnts.iterator().next().getValue();

        // A single primitive found, OK to return the all clear
        List<String> retList = new ArrayList<>();
        if (resCtx.isPrimitive()) {
            retList.add(resCtx.stringRepresentation());
            return retList;
        }

        /**
         * To simplify things further below, create a list of Context objects, even if results
         * contained just a single complex object. If Context is array, then add those to the list.
         */
        List<Context> ctxList  = new ArrayList<>();
        if (resCtx.isArray()) {
            ctxList.addAll(resCtx.asArray());
        } else {
            ctxList.add(resCtx);
        }


        for (Context ctx : ctxList) {
            Context ctxToAdd;
            if (ctx.isRecursible()) {
                if (ctx.entrySet().size() != 1){
                    throw new IllegalArgumentException("One or more context contained more than one entry: "
                            + ctxList.stream().map(Context::stringRepresentation).collect(Collectors.joining("\n")));
                } else {
                    ctxToAdd = ctx.entrySet().iterator().next().getValue();
                }
            } else if (ctx.isPrimitive()) {
                ctxToAdd = ctx;
            } else {
                throw new IllegalArgumentException("Not sure how to handle Context " + ctx.stringRepresentation()
                        + ". The full Context results are: " + ctxList.stream().map(Context::stringRepresentation)
                        .collect(Collectors.joining("\n")));
            }

            retList.add(ctxToAdd.stringRepresentation());
        }

        return retList;
    }
}
