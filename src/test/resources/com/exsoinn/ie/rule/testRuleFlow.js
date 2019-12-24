var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");

/*
 * The rule flow below depends on rule components defined elsewhere, namely in "testQueryableConfigRule.js"
 * and "testRuleBasedMatrixDecision.js"
 */
var rule_flow = {
                  "flow": {
                     "ccMgpEvalMatrix": {
                        "cc true, mgp false|cc false, mgp true": {
                           "dlDecisionMatrix": {
                              "DM": "ccEvalRule",
                              "FB": "mgpEvalRule"
                           },
                        }
                     }
                  }
               };

ruleFactory.createRuleFlow(JSON.stringify(rule_flow), "testRuleFlow", null);