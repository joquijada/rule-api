var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");
var selCriteria = Java.type("com.exsoinn.util.epf.SelectionCriteria");

var ruleNameDecLogicMatrix = "dlDecisionMatrix";
var ruleNameCalcTopSsrConfLvl = "calculateTopSsrConfLvl";
var ruleNameCalcTopMsrConfLvl = "calculateTopMsrConfLvl";
var dsRange = "dlRangeTable";
var dsRecTypeConfLvl = "dlRecTypeConfLvlConfig";

var tab_range = {
                   "range_table":[
                      {
                         "id":"0",
                         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15200||CFDC_LVL_VAL",
                         "operator":"IN",
                         "right_operand":"7,8,9,10",
                         "failure_output":"false"
                      },
                      {
                         "id":"1",
                         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15200||CFDC_LVL_VAL",
                         "operator":"IN",
                         "right_operand":"5,6",
                         "failure_output":"false"
                      },
                      {
                         "id":"2",
                         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15200||CFDC_LVL_VAL",
                         "operator":"IN",
                         "right_operand":"4,3,2,1,0",
                         "failure_output":"false"
                      }
                   ]
                };

var tab_rec_type_conf_lvl_config = {"rec_type_conf_lvl_config_table": [
                           {"id": "0", "record_type": "MSR", "success_output": "H","range_table": "0"},
                           {"id": "1", "record_type": "SSR", "success_output": "H","range_table": "0"},
                           {"id": "2", "record_type": "SSR", "success_output": "M","range_table": "1"},
                           {"id": "3", "record_type": "SSR", "success_output": "L","range_table": "2"},
                           {"id": "4", "record_type": "MSR", "success_output": "L","range_table": "2"},
                           {"id": "5", "record_type": "MSR", "success_output": "M","range_table": "1"}]};

var tab_geo_to_conf_lvl_config = {"geo_to_conf_lvl_config_table": [
                           {"id": "0", "geo_ref_id": "984", "rec_type_conf_lvl_config_table": "3"},
                           {"id": "1", "geo_ref_id": "892", "rec_type_conf_lvl_config_table": "5"},
                           {"id": "2", "geo_ref_id": "1073", "rec_type_conf_lvl_config_table": "5"},
                           {"id": "3", "geo_ref_id": "1021", "rec_type_conf_lvl_config_table": "1"},
                           {"id": "4", "geo_ref_id": "952", "rec_type_conf_lvl_config_table": "2"},
                           {"id": "5", "geo_ref_id": "1074", "rec_type_conf_lvl_config_table": "3"},
                           {"id": "6", "geo_ref_id": "892", "rec_type_conf_lvl_config_table": "1"},
                           {"id": "7", "geo_ref_id": "984", "rec_type_conf_lvl_config_table": "0"},
                           {"id": "8", "geo_ref_id": "DFLT", "rec_type_conf_lvl_config_table": "1"},
                           {"id": "9", "geo_ref_id": "984", "rec_type_conf_lvl_config_table": "2"},
                           {"id": "10", "geo_ref_id": "1021", "rec_type_conf_lvl_config_table": "4"},
                           {"id": "11", "geo_ref_id": "880", "rec_type_conf_lvl_config_table": "4"},
                           {"id": "12", "geo_ref_id": "880", "rec_type_conf_lvl_config_table": "0"},
                           {"id": "13", "geo_ref_id": "880", "rec_type_conf_lvl_config_table": "3"},
                           {"id": "14", "geo_ref_id": "1021", "rec_type_conf_lvl_config_table": "0"},
                           {"id": "15", "geo_ref_id": "984", "rec_type_conf_lvl_config_table": "5"},
                           {"id": "16", "geo_ref_id": "1074", "rec_type_conf_lvl_config_table": "1"},
                           {"id": "17", "geo_ref_id": "952", "rec_type_conf_lvl_config_table": "4"},
                           {"id": "18", "geo_ref_id": "1073", "rec_type_conf_lvl_config_table": "1"},
                           {"id": "19", "geo_ref_id": "892", "rec_type_conf_lvl_config_table": "3"},
                           {"id": "20", "geo_ref_id": "952", "rec_type_conf_lvl_config_table": "1"},
                           {"id": "21", "geo_ref_id": "1021", "rec_type_conf_lvl_config_table": "3"},
                           {"id": "22", "geo_ref_id": "880", "rec_type_conf_lvl_config_table": "1"},
                           {"id": "23", "geo_ref_id": "1074", "rec_type_conf_lvl_config_table": "0"},
                           {"id": "24", "geo_ref_id": "880", "rec_type_conf_lvl_config_table": "2"},
                           {"id": "25", "geo_ref_id": "1021", "rec_type_conf_lvl_config_table": "5"},
                           {"id": "26", "geo_ref_id": "DFLT", "rec_type_conf_lvl_config_table": "2"},
                           {"id": "27", "geo_ref_id": "1074", "rec_type_conf_lvl_config_table": "2"},
                           {"id": "28", "geo_ref_id": "1073", "rec_type_conf_lvl_config_table": "2"},
                           {"id": "29", "geo_ref_id": "1074", "rec_type_conf_lvl_config_table": "5"},
                           {"id": "30", "geo_ref_id": "952", "rec_type_conf_lvl_config_table": "0"},
                           {"id": "31", "geo_ref_id": "1021", "rec_type_conf_lvl_config_table": "2"},
                           {"id": "32", "geo_ref_id": "DFLT", "rec_type_conf_lvl_config_table": "3"},
                           {"id": "33", "geo_ref_id": "880", "rec_type_conf_lvl_config_table": "5"},
                           {"id": "34", "geo_ref_id": "892", "rec_type_conf_lvl_config_table": "0"},
                           {"id": "35", "geo_ref_id": "952", "rec_type_conf_lvl_config_table": "5"},
                           {"id": "36", "geo_ref_id": "1074", "rec_type_conf_lvl_config_table": "4"},
                           {"id": "37", "geo_ref_id": "DFLT", "rec_type_conf_lvl_config_table": "4"},
                           {"id": "38", "geo_ref_id": "DFLT", "rec_type_conf_lvl_config_table": "0"},
                           {"id": "39", "geo_ref_id": "1073", "rec_type_conf_lvl_config_table": "4"},
                           {"id": "40", "geo_ref_id": "892", "rec_type_conf_lvl_config_table": "2"},
                           {"id": "41", "geo_ref_id": "984", "rec_type_conf_lvl_config_table": "4"},
                           {"id": "42", "geo_ref_id": "892", "rec_type_conf_lvl_config_table": "4"},
                           {"id": "43", "geo_ref_id": "1073", "rec_type_conf_lvl_config_table": "0"},
                           {"id": "44", "geo_ref_id": "1073", "rec_type_conf_lvl_config_table": "3"},
                           {"id": "45", "geo_ref_id": "DFLT", "rec_type_conf_lvl_config_table": "5"},
                           {"id": "46", "geo_ref_id": "952", "rec_type_conf_lvl_config_table": "3"},
                           {"id": "47", "geo_ref_id": "984", "rec_type_conf_lvl_config_table": "1"}]};
var tab_decision_matrix = {"ruled_based_flow_decision_table": [
                           {"decision": "DM", "calculateTopSsrConfLvl": "H", "calculateTopMsrConfLvl": "H"},
                           {"decision": "DM", "calculateTopSsrConfLvl": "H", "calculateTopMsrConfLvl": "M"},
                           {"decision": "FB", "calculateTopSsrConfLvl": "H", "calculateTopMsrConfLvl": "L"},
                           {"decision": "DM", "calculateTopSsrConfLvl": "M", "calculateTopMsrConfLvl": "H"},
                           {"decision": "DM", "calculateTopSsrConfLvl": "M", "calculateTopMsrConfLvl": "M"},
                           {"decision": "FB", "calculateTopSsrConfLvl": "M", "calculateTopMsrConfLvl": "L"},
                           {"decision": "DM", "calculateTopSsrConfLvl": "L", "calculateTopMsrConfLvl": "H"},
                           {"decision": "DM", "calculateTopSsrConfLvl": "L", "calculateTopMsrConfLvl": "M"},
                           {"decision": "FB", "calculateTopSsrConfLvl": "L", "calculateTopMsrConfLvl": "L"}]};

ruleFactory.createQueryableDataRule(JSON.stringify(tab_range), dsRange);
ruleFactory.createQueryableDataRule(JSON.stringify(tab_rec_type_conf_lvl_config), dsRecTypeConfLvl);
//ruleFactory.createQueryableDataRule(JSON.stringify(tab_geo_to_conf_lvl_config), ruleName.DL_GEO_TO_CONF_LVL_CONF_TABLE);
//ruleFactory.createQueryableDataRule(JSON.stringify(tab_decision_matrix), ruleName.DL_DECISION_MATRIX_TABLE);



/*
 * Build a Map of elements which values have to match the configured rule data in order to pull
 * rule sets applicable to current IE record. The keys in the Map are field names in the rule data,
 * and the values are the search criteria to be able to search the element value in the underlying
 * IE record.
 */
var delim = selCriteria.SEARCH_CRITERIA_DELIM;
var params = new java.util.HashMap(1);
params.put("geo_ref_id", "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW"
  + delim + selCriteria.SEARCH_CRITERIA_NULL + delim + "GEO_REF_ID");


ruleFactory.ruleSetBuilder(JSON.stringify(tab_geo_to_conf_lvl_config), ruleNameCalcTopSsrConfLvl, "anyCanPass")
  .ruleSetFilter(params).build();

ruleFactory.ruleSetBuilder(JSON.stringify(tab_geo_to_conf_lvl_config), ruleNameCalcTopMsrConfLvl,"anyCanPass")
  .ruleSetFilter(params)
  .leftOperandValueOverride("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15201||CFDC_LVL_VAL")
  .build();

// Because this matrix has dependency on rules above, must be defined after
ruleFactory.createConfigurableRuleBasedMatrixRule(JSON.stringify(tab_decision_matrix), ruleNameDecLogicMatrix);