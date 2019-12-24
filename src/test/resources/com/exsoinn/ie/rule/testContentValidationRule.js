var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");
var selCriteria = Java.type("com.exsoinn.util.epf.SelectionCriteria");
var cvLftOprdOprLbl = "cvLftOprdOpr";
var cvCnfgLbl = "cvConfigVal";
var cntnVldnRulesLbl = "cntnVldnRules";

var tab_left_operand_table = {
                    "cv_left_operand": [
                        {
                            "id":"0",
                            "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[0].NME_ENTR_VW||NULL||NME_TEXT"
                        },
                        {
                            "id":"1",
                            "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].NME_ENTR_VW||STDN_APPL_CD=13135||NME_TEXT"
                        },
                        {
                            "id":"2",
                            "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[2].NME_ENTR_VW||STDN_APPL_CD=24099||NME_TEXT"
                        },
                        {
                            "id":"3",
                            "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15201||CFDC_LVL_VAL"
                        },
                        {
                            "id":"4",
                            "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[1].ADR_ENTR_VW||STDN_APPL_CD=13135||POST_CODE"
                        }
                    ]
                };


var tab_cv_config_table = {
                   "cv_config_table":[
                      {
                         "id":"0",
                         "cv_left_operand":"0",
                         "operator":"STR_HANDLER||EQUALS||LOWER_CASE",
                         "right_operand":"life time wireless inc",
                         "success_output":"9000",
                         "failure_output":"8000"
                      },
                      {
                         "id":"1",
                         "cv_left_operand":"1",
                         "operator":"STR_HANDLER||EQUALS||UPPER_CASE",
                         "right_operand":"LIFE TIME WIRELESS",
                         "success_output":"9000",
                         "failure_output":"8001"
                      },
                      {
                         "id":"2",
                         "cv_left_operand":"3",
                         "operator":"IN",
                         "right_operand":"10,9,8",
                         "success_output":"9000",
                         "failure_output":"8002"
                      },
                      {
                         "id":"3",
                         "cv_left_operand":"3",
                         "operator":"==",
                         "right_operand":"10",
                         "success_output":"9000",
                         "failure_output":"8003"
                      },
                      {
                         "id":"4",
                         "cv_left_operand":"3",
                         "operator":">",
                         "right_operand":"8",
                         "success_output":"9000",
                         "failure_output":"8004"
                      },
                      {
                         "id":"5",
                         "cv_left_operand":"4",
                         "operator":"STR_HANDLER||STRNG_LNGTH_GRTR_THAN||TRIM",
                         "right_operand":"2",
                         "success_output":"9000",
                         "failure_output":"8005"
                      }
                   ]
                };

var tab_cntn_valdn_rules_table = {
                   "cntn_valdn_rules_table":[
                       {
                            "id":"0",
                            "geo_ref_id":"1073",
                            "info_src_cd":"13182",
                            "cv_config_table":"0"
                       },
                       {
                            "id":"1",
                            "geo_ref_id":"1073",
                            "info_src_cd":"13182",
                            "cv_config_table":"1"
                       },
                       {
                            "id":"2",
                            "geo_ref_id":"1073",
                            "info_src_cd":"13182",
                            "cv_config_table":"2"
                       },
                       {
                            "id":"3",
                            "geo_ref_id":"1073",
                            "info_src_cd":"13182",
                            "cv_config_table":"3"
                       },
                       {
                            "id":"4",
                            "geo_ref_id":"1073",
                            "info_src_cd":"13182",
                            "cv_config_table":"4"
                       },
                       {
                            "id":"5",
                            "geo_ref_id":"1073",
                            "info_src_cd":"13182",
                            "cv_config_table":"5"
                       }
                   ]
                };

ruleFactory.createQueryableDataRule(JSON.stringify(tab_left_operand_table), cvLftOprdOprLbl);
ruleFactory.createQueryableDataRule(JSON.stringify(tab_cv_config_table), cvCnfgLbl);

var delim = selCriteria.SEARCH_CRITERIA_DELIM;
var params = new java.util.HashMap(2);
params.put("geo_ref_id", "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW"
  + delim + selCriteria.SEARCH_CRITERIA_NULL + delim + "GEO_REF_ID");
params.put("info_src_cd", "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.ORG_ID.REGN_NBR_ENTR.DNB_DATA_PRVD_CD" + delim
                + selCriteria.SEARCH_CRITERIA_NULL + delim + selCriteria.SEARCH_CRITERIA_NULL);

ruleFactory.ruleSetBuilder(JSON.stringify(tab_cntn_valdn_rules_table), cntnVldnRulesLbl, "allMustPass")
  .ruleSetFilter(params).build();
