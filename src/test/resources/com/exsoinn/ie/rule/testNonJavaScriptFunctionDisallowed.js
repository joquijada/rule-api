var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");

/*
 * Try loading something which is not a JavaScript function. the application will
 * reject it.
 */
var bogusFunc = {};
ruleFactory.createJavaScriptFunctionRule("bogusFunc", bogusFunc);