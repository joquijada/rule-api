package com.exsoinn.ie.rule;

/**
 * Created by QuijadaJ on 8/11/2017.
 */
public class BadJavaScriptFunctionException extends RuleException {
    BadJavaScriptFunctionException(String pName, Object pObj) {
        super("Passed in object for rule name '" + pName + "' is not a JavaScript function."
                + " Instead was given object type " + pObj.getClass().getName() + ", and the string representation "
                + " is " + pObj.toString(), null);
    }
}
