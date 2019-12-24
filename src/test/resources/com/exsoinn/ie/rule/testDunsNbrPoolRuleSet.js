var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");
var selCriteria = Java.type("com.exsoinn.util.epf.SelectionCriteria");

var tab_duns_nbr_pool_us_rl_cnfg = {
    "duns_nbr_pool_us_cnfg_table": [
            {
                "geo_ref_id":"1073",
                "left_operand_table":"89",
                "operator":"LU||EXACT_MATCH",
                "right_operand":"dunsNbrPoolLstCnfgTbl-geoRefId1073-dunsNbrPoolTbl-outField:duns_nbr",
                "lu_output_field_name":"duns_nbr",
                "success_output":"TRUE",
                "failure_output":"FALSE",
                "ignore_element_not_found_error":true
            }
        ]
    };

var tab_duns_nbr_pool_ca_rl_cnfg = {
    "duns_nbr_pool_ca_cnfg_table": [
           {
                "geo_ref_id":"892",
                "left_operand_table":"89",
                "operator":"LU||EXACT_MATCH",
                "right_operand":"dunsNbrPoolLstCnfgTbl-geoRefId892-dunsNbrPoolTbl-outField:duns_nbr",
                "lu_output_field_name":"duns_nbr",
                "success_output":"TRUE",
                "failure_output":"FALSE",
                "ignore_element_not_found_error":true
           }
        ]
    };

var tab_duns_nbr_pool_uk_rl_cnfg = {
    "duns_nbr_pool_uk_cnfg_table": [
            {
                "geo_ref_id":"1067",
                "left_operand_table":"89",
                "operator":"LU||EXACT_MATCH",
                "right_operand":"dunsNbrPoolLstCnfgTbl-geoRefId1067-dunsNbrPoolTbl-outField:duns_nbr",
                "lu_output_field_name":"duns_nbr",
                "success_output":"TRUE",
                "failure_output":"FALSE",
                "ignore_element_not_found_error":true
            }
        ]
    };


var delim = selCriteria.SEARCH_CRITERIA_DELIM;
var dunsPoolMap = new java.util.HashMap(1);
dunsPoolMap.put("geo_ref_id", "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW"
    + delim + selCriteria.SEARCH_CRITERIA_NULL + delim + "GEO_REF_ID");

ruleFactory.createQueryableRuleConfigurationDataRule(
                JSON.stringify(tab_duns_nbr_pool_us_rl_cnfg),
                "dunsNbrPoolUsRlCnfg",
                dunsPoolMap,
                "resultEvaluationOption==anyCanPass&successOutputFieldName==lu_output_field_name",
                null);

ruleFactory.createQueryableRuleConfigurationDataRule(
                JSON.stringify(tab_duns_nbr_pool_ca_rl_cnfg),
                "dunsNbrPoolCaRlCnfg",
                dunsPoolMap,
                "resultEvaluationOption==anyCanPass&successOutputFieldName==lu_output_field_name",
                null);

ruleFactory.createQueryableRuleConfigurationDataRule(
                JSON.stringify(tab_duns_nbr_pool_uk_rl_cnfg),
                "dunsNbrPoolUkRlCnfg",
                dunsPoolMap,
                "resultEvaluationOption==anyCanPass&successOutputFieldName==lu_output_field_name",
                null);