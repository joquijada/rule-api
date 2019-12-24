package com.exsoinn.ie.rule;

import com.exsoinn.util.epf.*;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.Map;

/**
 * This rule component takes a JavaScript function in its constructor. When invoked via its
 * {@link JavaScriptFunctionRule#applyBody(Context, SearchPath, Filter, TargetElements, Map)} method,
 * it calls whatever code was supplied in the JavaScript function body.
 * This class is meant to provide a flexible mechanism to execute complex rule logic when the other rule components
 * fall short of that goal.
 *
 * The class will check that indeed a JavaScript function was given, else it throws {@link BadJavaScriptFunctionException}.
 * Nashorn internally converts JavaScript functions to {@link ScriptObjectMirror} when passed to Java code, and that's one
 * of the way we checks for bad input. The other way is to invoke {@link ScriptObjectMirror#isFunction()} method.
 * Created by QuijadaJ on 4/19/2017.
 */
public class JavaScriptFunctionRule extends AbstractRule {
    private final ScriptObjectMirror function;

    JavaScriptFunctionRule(Object pFunc, String pUniqueName) throws RuleException {
        super(pUniqueName);
        if (!(pFunc instanceof ScriptObjectMirror) || !((ScriptObjectMirror) pFunc).isFunction()) {
            throw new BadJavaScriptFunctionException(pUniqueName, pFunc);
        }

        function = (ScriptObjectMirror) pFunc;
        function.seal();
    }

    @Override
    public  <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        Object ret = function.call(null, pContext);
        Map<String, String> info = populateCommonResultProperties(pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, null);
        return new RuleSetOperationResult(true, info, ret.toString(), pContext, null);
    }
}