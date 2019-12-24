package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.Context;

import java.util.Map;

/**
 * Created by QuijadaJ on 5/11/2017.
 */
public interface SearchResultRuleExecutionResult extends RuleExecutionResult {
    Map<String, Context> getSearchResult();
}
