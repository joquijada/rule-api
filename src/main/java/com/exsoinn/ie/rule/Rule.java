package com.exsoinn.ie.rule;


import com.exsoinn.ie.util.IdentifiedByName;
import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.Filter;
import com.exsoinn.util.epf.SearchPath;
import com.exsoinn.util.epf.TargetElements;

import java.util.Map;
import java.util.Set;

/**
 * TODO: Can later add different apply() method signatures depending on the type of object that
 *       the rule should be applied to. For example, applyRuleToJsonElement(String pElemName),
 *       applyRuleToEntity(String pEntityName, String[] pNamesOfElemsWithinEntity), etc
 * Created by QuijadaJ on 4/19/2017.
 */
public interface Rule extends IdentifiedByName {
    RuleExecutionResult apply(String pDataStr,
                                     String pElemSearchPath,
                                     Map<String, String> pElemFilter,
                                     Set<String> pTargetElems,
                                     Map<String, String> pExtraParams) throws RuleException;

    <T extends SearchPath, U extends Filter, V extends TargetElements> RuleExecutionResult apply(Context pContext,
                              T pSearchPath,
                              U pFilter,
                              V pTargetElems,
                              Map<String, String> pExtraParams) throws RuleException;

    Map<String, Rule> getOutputFieldMap();
}
