var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");
var selCriteria = Java.type("com.exsoinn.util.epf.SelectionCriteria");

/*
 * Note: This data source joins with another defined in "testCommon.js". The right_operand value is actually
 *   the ID in foreign table "right_operand"
 */
var json_cc_eval = {
   "cc_eval_rules":[
      {
         "geo_ref_id":"1073",
         "info_src_cd":"13182",
         "search_path":"0",
         "lu_operation":"0",
         "lu_right_operand":"0",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "geo_ref_id":"1073",
         "info_src_cd":"13182",
         "search_path":"0",
         "lu_operation":"0",
         "lu_right_operand":"1",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "geo_ref_id":"1073",
         "info_src_cd":"13182",
         "search_path":"0",
         "lu_operation":"0",
         "lu_right_operand":"2",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "geo_ref_id":"1067",
         "info_src_cd":"13182",
         "search_path":"0",
         "lu_operation":"0",
         "lu_right_operand":"1",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "geo_ref_id":"1073",
         "info_src_cd":"15555",
         "search_path":"0",
         "lu_operation":"0",
         "lu_right_operand":"2",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "geo_ref_id":"892",
         "info_src_cd":"13182",
         "search_path":"0",
         "lu_operation":"0",
         "lu_right_operand":"2",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "geo_ref_id":"DEFAULT",
         "info_src_cd":"DEFAULT",
         "search_path":"0",
         "lu_operation":"0",
         "lu_right_operand":"2",
         "success_output":"true",
         "failure_output":"false"
      }
   ]
};



var json_patt_list = {
   "pattern_list":[
      {
         "id": "0",
         "right_operand":"BFFAAZZBFZF"
      },
      {
         "id": "1",
         "right_operand":"BFFAAZZBFZ8"
      },
      {
         "id": "2",
         "right_operand":"BFFAAZZBFZ9"
      }
   ]
};


var json_mgp_eval = {
   "mgp_eval_rules":[
      {
         "geo_ref_id":"1073",
         "info_src_cd":"13182",
         "search_path":"1",
         "operator":"REGEX",
         "pattern_list":"0",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "geo_ref_id":"1073",
         "info_src_cd":"13182",
         "search_path":"1",
         "operator":"REGEX",
         "pattern_list":"1",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "geo_ref_id":"1073",
         "info_src_cd":"13182",
         "search_path":"1",
         "operator":"REGEX",
         "pattern_list":"2",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "geo_ref_id":"1067",
         "info_src_cd":"13182",
         "search_path":"1",
         "operator":"REGEX",
         "pattern_list":"0",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "geo_ref_id":"1073",
         "info_src_cd":"15555",
         "search_path":"1",
         "operator":"REGEX",
         "pattern_list":"0",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "geo_ref_id":"892",
         "info_src_cd":"13182",
         "search_path":"1",
         "operator":"REGEX",
         "pattern_list":"0",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "geo_ref_id":"DEFAULT",
         "info_src_cd":"DEFAULT",
         "search_path":"1",
         "operator":"REGEX",
         "pattern_list":"0",
         "success_output":"true",
         "failure_output":"false"
      }
   ]
};


var json_matrix_based_on_rule = {
   "matrix_based_on_rules":[
      {
         "ccEvalRule":"true",
         "mgpEvalRule":"true",
         "decision":"both are true"
      },
      {
         "ccEvalRule":"true",
         "mgpEvalRule":"false",
         "decision":"cc true, mgp false"
      },
      {
         "ccEvalRule":"false",
         "mgpEvalRule":"true",
         "decision":"cc false, mgp true"
      },
      {
         "ccEvalRule":"false",
         "mgpEvalRule":"false",
         "decision":"both are false"
      }
   ]
};


/*
 * To aid in testing rule filter works
 */
var config_rule_set_filter_test = {
   "config_rule_set_filter_test":[
      {
         "cc":[0,1,2,3,4],
         "mgp_eval_rule":"true",
         "left_operand":"NO-EVAL:1",
         "operand":"==",
         "right_operand":"1",
         "success_output":"true",
         "failure_output":"false"
      },
      {
         "cc":[0,1,2,3,4],
         "mgp_eval_rule":"false",
         "left_operand":"NO-EVAL:1",
         "operand":"==",
         "right_operand":"1",
         "success_output":"true",
         "failure_output":"false"
      }
   ]
};

// Load the pattern list
ruleFactory.createQueryableDataRule(JSON.stringify(json_patt_list), "patternList");

var delim = selCriteria.SEARCH_CRITERIA_DELIM;
var m = new java.util.HashMap(2);
m.put("geo_ref_id",
                "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW"
                        + delim + selCriteria.SEARCH_CRITERIA_NULL + delim + "GEO_REF_ID");
m.put("info_src_cd", "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.ORG_ID.REGN_NBR_ENTR.DNB_DATA_PRVD_CD" + delim
                + selCriteria.SEARCH_CRITERIA_NULL + delim + selCriteria.SEARCH_CRITERIA_NULL);


// First create the rule. This rule uses a RegEx rule set
ruleFactory.createQueryableRuleConfigurationDataRule(
  JSON.stringify(json_cc_eval),
  "ccEvalRule",
  m,
  "resultEvaluationOption==anyCanPass",
  null
  );

ruleFactory.createQueryableRuleConfigurationDataRule(
  JSON.stringify(json_mgp_eval),
  "mgpEvalRule",
  m,
  "resultEvaluationOption==allMustPass",
  null
  );

ruleFactory.createConfigurableRuleBasedMatrixRule(JSON.stringify(json_matrix_based_on_rule), "ccMgpEvalMatrix");

// Define a rule which gives the right-operand as output
ruleFactory.createQueryableRuleConfigurationDataRule(
  JSON.stringify(json_cc_eval),
  "ccEvalRuleOutFldOverride",
  m,
  "resultEvaluationOption==anyCanPass&successOutputFieldName==right_operand",
  null);

var testFilter = new java.util.HashMap(2);
testFilter.put("cc",
  "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15200||CFDC_LVL_VAL");
testFilter.put("mgp_eval_rule", "RULE:ccEvalRule");
ruleFactory.createQueryableRuleConfigurationDataRule(
  JSON.stringify(config_rule_set_filter_test),
  "ruleSetFilterTest",
  testFilter,
  "resultEvaluationOption==anyCanPass",
  null
  );