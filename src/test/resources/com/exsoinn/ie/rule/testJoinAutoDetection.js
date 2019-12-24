var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");
var selCriteria = Java.type("com.exsoinn.util.epf.SelectionCriteria");

var tab_last = {"last_table": [{"id": "0", "left_operand": "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15200||CFDC_LVL_VAL", "operator": "IN", "right_operand": "7,8,9,10"},
                               {"id": "1", "left_operand": "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15200||CFDC_LVL_VAL", "operator": "IN", "right_operand": "5,6"},
                               {"id": "2", "left_operand": "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15200||CFDC_LVL_VAL", "operator": "IN", "right_operand": "4,3,2,1,0"}]};

var tab_middle = {"middle_table": [
                           {"id": "0", "record_type": "MSR", "success_output": "H","last_table": "0"},
                           {"id": "1", "record_type": "SSR", "success_output": "H","last_table": "0"},
                           {"id": "2", "record_type": "SSR", "success_output": "M","last_table": "1"},
                           {"id": "3", "record_type": "SSR", "success_output": "L","last_table": "2"},
                           {"id": "4", "record_type": "MSR", "success_output": "L","last_table": "2"},
                           {"id": "5", "record_type": "MSR", "success_output": "M","last_table": "1"}]};

var tab_main = {"main_table": [
                           {"id": "0", "geo_ref_id": "984", "middle_table": "3"},
                           {"id": "1", "geo_ref_id": "892", "middle_table": "5"},
                           {"id": "2", "geo_ref_id": "1073", "middle_table": "5"},
                           {"id": "3", "geo_ref_id": "1021", "middle_table": "1"},
                           {"id": "4", "geo_ref_id": "952", "middle_table": "2"},
                           {"id": "5", "geo_ref_id": "1074", "middle_table": "3"},
                           {"id": "6", "geo_ref_id": "892", "middle_table": "1"},
                           {"id": "7", "geo_ref_id": "984", "middle_table": "0"},
                           {"id": "8", "geo_ref_id": "DFLT", "middle_table": "1"},
                           {"id": "9", "geo_ref_id": "984", "middle_table": "2"},
                           {"id": "10", "geo_ref_id": "1021", "middle_table": "4"},
                           {"id": "11", "geo_ref_id": "880", "middle_table": "4"},
                           {"id": "12", "geo_ref_id": "880", "middle_table": "0"},
                           {"id": "13", "geo_ref_id": "880", "middle_table": "3"},
                           {"id": "14", "geo_ref_id": "1021", "middle_table": "0"},
                           {"id": "15", "geo_ref_id": "984", "middle_table": "5"},
                           {"id": "16", "geo_ref_id": "1074", "middle_table": "1"},
                           {"id": "17", "geo_ref_id": "952", "middle_table": "4"},
                           {"id": "18", "geo_ref_id": "1073", "middle_table": "1"},
                           {"id": "19", "geo_ref_id": "892", "middle_table": "3"},
                           {"id": "20", "geo_ref_id": "952", "middle_table": "1"},
                           {"id": "21", "geo_ref_id": "1021", "middle_table": "3"},
                           {"id": "22", "geo_ref_id": "880", "middle_table": "1"},
                           {"id": "23", "geo_ref_id": "1074", "middle_table": "0"},
                           {"id": "24", "geo_ref_id": "880", "middle_table": "2"},
                           {"id": "25", "geo_ref_id": "1021", "middle_table": "5"},
                           {"id": "26", "geo_ref_id": "DFLT", "middle_table": "2"},
                           {"id": "27", "geo_ref_id": "1074", "middle_table": "2"},
                           {"id": "28", "geo_ref_id": "1073", "middle_table": "2"},
                           {"id": "29", "geo_ref_id": "1074", "middle_table": "5"},
                           {"id": "30", "geo_ref_id": "952", "middle_table": "0"},
                           {"id": "31", "geo_ref_id": "1021", "middle_table": "2"},
                           {"id": "32", "geo_ref_id": "DFLT", "middle_table": "3"},
                           {"id": "33", "geo_ref_id": "880", "middle_table": "5"},
                           {"id": "34", "geo_ref_id": "892", "middle_table": "0"},
                           {"id": "35", "geo_ref_id": "952", "middle_table": "5"},
                           {"id": "36", "geo_ref_id": "1074", "middle_table": "4"},
                           {"id": "37", "geo_ref_id": "DFLT", "middle_table": "4"},
                           {"id": "38", "geo_ref_id": "DFLT", "middle_table": "0"},
                           {"id": "39", "geo_ref_id": "1073", "middle_table": "4"},
                           {"id": "40", "geo_ref_id": "892", "middle_table": "2"},
                           {"id": "41", "geo_ref_id": "984", "middle_table": "4"},
                           {"id": "42", "geo_ref_id": "892", "middle_table": "4"},
                           {"id": "43", "geo_ref_id": "1073", "middle_table": "0"},
                           {"id": "44", "geo_ref_id": "1073", "middle_table": "3"},
                           {"id": "45", "geo_ref_id": "DFLT", "middle_table": "5"},
                           {"id": "46", "geo_ref_id": "952", "middle_table": "3"},
                           {"id": "47", "geo_ref_id": "984", "middle_table": "1"}]};

var tab_list = {"list_table": [{"id": "0", "list_name": "US_List"},
                               {"id": "1", "list_name": "Can_List"}]};

var tab_mapper = {"mapper_table": [{"list_table": "0", "entry_table": "0"},
                                   {"list_table": "0", "entry_table": "1"},
                                   {"list_table": "0", "entry_table": "2"},
                                   {"list_table": "1", "entry_table": "3"},
                                   {"list_table": "1", "entry_table": "4"},
                                   {"list_table": "1", "entry_table": "5"},
                                   {"list_table": "0", "entry_table": "3"},
                                   {"list_table": "1", "entry_table": "0"},
                                   {"list_table": "0", "entry_table": "6"},
                                   {"list_table": "1", "entry_table": "6"},
                                   {"list_table": "0", "entry_table": "7"},
                                   {"list_table": "1", "entry_table": "7"}]};

var tab_entry = {"entry_table": [
                           {"id": "0", "entry": "Google"},
                           {"id": "1", "entry": "Facebook"},
                           {"id": "2", "entry": "VZW"},
                           {"id": "3", "entry": "John Deere"},
                           {"id": "4", "entry": "Nelnet"},
                           {"id": "5", "entry": "Microsoft"},
                           {"id": "6", "entry": "Oracle"},
                           {"id": "7", "entry": "Sun Microsystems"}]};


var tab_list_rule = {"list_rule_table": [
                        {"geo_ref_id": "1073", "list_table": "0"},
                        {"geo_ref_id": "892", "list_table": "1"}]};

var tab_with_two_joins = {"two_joins_table": [
                         {"list_table": "0", "entry_table": "2", "name": "foo"},
                         {"list_table": "1", "entry_table": "3", "name": "bar"}]};


ruleFactory.createQueryableDataRule(JSON.stringify(tab_last), "lastTable");
ruleFactory.createQueryableDataRule(JSON.stringify(tab_middle), "middleTable");
ruleFactory.createQueryableDataRule(JSON.stringify(tab_list), "listTable");
ruleFactory.createQueryableDataRule(JSON.stringify(tab_mapper), "mapperTable");
ruleFactory.createQueryableDataRule(JSON.stringify(tab_entry), "entryTable");

var delim = selCriteria.SEARCH_CRITERIA_DELIM;
var params = new java.util.HashMap(1);
params.put("geo_ref_id", "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW"
  + delim + selCriteria.SEARCH_CRITERIA_NULL + delim + "GEO_REF_ID");

ruleFactory.createQueryableRuleConfigurationDataRule(
                JSON.stringify(tab_main),
                "mainTable",
                params,
                "resultEvaluationOption==anyCanPass",
                null);

ruleFactory.createQueryableRuleConfigurationDataRule(
                JSON.stringify(tab_list_rule),
                "listRuleTable",
                params,
                "resultEvaluationOption==anyCanPass",
                null);