package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Filter;
import com.exsoinn.util.epf.SearchResult;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by QuijadaJ on 6/30/2017.
 */
public class MatrixCouldNotComputeDecisionException extends RuleException {
    MatrixCouldNotComputeDecisionException(String pMsg, Throwable t) {
        super(pMsg, t);
    }

    MatrixCouldNotComputeDecisionException(IQueryableDataRule pRule, Filter pDecisionInput, SearchResult pSearchRes) {
        this("Matrix decision gave invalid output, expected only one primite value, check matrix configuration for '"
                + pRule.name() + "'. Matrix used was "
                + pRule.context().stringRepresentation() + ", and the decision input was "
                + pDecisionInput.toString() + ". Results, if any were "
                + (null != pSearchRes ? pSearchRes.entrySet().stream().map(Map.Entry::toString).collect(Collectors.joining(";")) : "NONE"), null);
    }
}
