var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");


/*
 * The two operations below are mutually exclusive, meaning they will never both execute for the same
 * input data. They exclude each other by checking geo_ref_id of different value. The first operation
 * always passes because it's checking 10 == 10. The second will always fail because it checks if 0 == 10
 */
var tab_js_function_test_rule_set = {
  "js_function_test_rule_set_table":[
    {
       "geo_ref_id":"1073",
       "left_operand":"NO-EVAL:10",
       "operator":"==",
       "right_operand":"10",
       "success_output": "pass",
       "failure_output": "fail"
    },
    {
       "geo_ref_id":"892",
       "left_operand":"NO-EVAL:0",
       "operator":"==",
       "right_operand":"10",
       "success_output": "pass",
       "failure_output": "fail"
    }
  ]};

/*
 * Load the rule set into memory
 */
ruleFactory.createElementFinderRule("testFinderGeoRefId",
  "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW||NULL||GEO_REF_ID");
var selCrit = new java.util.HashMap(1);
selCrit.put("geo_ref_id", "RULE:testFinderGeoRefId");

ruleFactory.createQueryableRuleConfigurationDataRule(
  JSON.stringify(tab_js_function_test_rule_set),
  "jsFunctionTestRuleSet",
  selCrit,
  "resultEvaluationOption==anyCanPass",
  null);


/*
 * Define a JavaScript function that executes the rule(s) defined above, with an
 * if/else statement that returns a value based on the rule result.
 */
var myFunc = function(pData) {
    var r = ruleFactory.executeConfigurableRule("jsFunctionTestRuleSet", pData);
    if (r.getOutput() === 'pass') {
        return "Passed";
    } else if (r.getOutput() === 'fail') {
        return "Failed";
    }
  };


/*
 * Load the JavaScript function into memory with the
 * given name so it can be referred later on.
 */
ruleFactory.createJavaScriptFunctionRule("testFunc1", myFunc);