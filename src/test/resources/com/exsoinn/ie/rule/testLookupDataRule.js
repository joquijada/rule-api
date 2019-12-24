var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");
var ruleName = Java.type("com.exsoinn.ie.rule.RuleName");
var selCriteria = Java.type("com.exsoinn.util.epf.SelectionCriteria");

var tab_ie_genc_lookup_list={"ie_genc_lookup_list_entry_table" : [
                          //Sample Data From IEDWMOWNER.GENC_DEL_LIST Table
                          {"id" : "0", "other_field": "0", "entry" : "BSU CAM CRIME DATE TEST 2"},
                          {"id" : "1", "other_field": "1", "entry" : "LIFE TIME WIRELESS"},
                          {"id" : "2", "other_field": "2", "entry" : "NP REINSLOAD COP TEST 2"},
                          {"id" : "3", "other_field": "3", "entry" : "ADCENTER TEST CUSTOMER 1"},
                          {"id" : "4", "other_field": "4", "entry" : "OCTOBER VERSIONSMY TEST IN TEST CASE 004"},
                          {"id" : "5", "other_field": "5", "entry" : "FL TEST INTERNAL"},
                          {"id" : "6", "other_field": "6", "entry" : "EPHR GENERATE TEST 219"},
                          //Sample Data From IEDWMOWNER.DIRTY_WRD_FLTR Table
                          {"id" : "7", "other_field": "7", "entry" : "HDC LIFE TIME WIRELESS ENTERPRISE"},
                          {"id" : "8", "other_field": "8", "entry" : "DIRTWORD2"},
                          {"id" : "9", "other_field": "9", "entry" : "DIRTWORD3"},
                          {"id" : "10", "other_field": "10", "entry" : "DIRTWORD4"},
                          {"id" : "11", "other_field": "11", "entry" : "DIRTWORD5"},
                          {"id" : "12", "other_field": "12", "entry" : "DIRTWORD6"},
                          //Sample Data From IEDWMOWNER.GENC_BUS_NME_TRDG_NME Table
                          {"id" : "13", "other_field": "13", "entry" : "CIS DHS"},
                          {"id" : "14", "other_field": "14", "entry" : "FBI DOJ"},
                          {"id" : "15", "other_field": "15", "entry" : "FLETC DHS"},
                          {"id" : "16", "other_field": "16", "entry" : "IMM CUSTOM ENFORC AB 7005"},
                          {"id" : "17", "other_field": "17", "entry" : "BURLINTON FINANCE CTYR US IMMIGRATIN CUSTOM"},
                          {"id" : "18", "other_field": "18", "entry" : "I MOTOROLA EMPLOYEE"},
                          //Sample Data From IEDWMOWNER.TRADE_BUS_NME_TRDG_NME Table
                          {"id" : "19", "other_field": "19", "entry" : "ADMINISTRATION"},
                          {"id" : "20", "other_field": "20", "entry" : "AMERICAN AIRLINES"},
                          {"id" : "21", "other_field": "21", "entry" : "BLACK DECKER"},
                          {"id" : "22", "other_field": "22", "entry" : "CHARTER TOWNSHI"},
                          {"id" : "23", "other_field": "23", "entry" : "DECORATING CONSULT"},
                          {"id" : "24", "other_field": "24", "entry" : "JEFFCO PUBLIC SCH"},
                          //Sample Data From IEDWMOWNER.GENC_ADR_FLTR Table
                          {"id" : "25", "other_field": "25", "entry" : "57 BLACKBERRY DR"},
                          {"id" : "26", "other_field": "26", "entry" : "TWO WORLD TRADE"},
                          {"id" : "27", "other_field": "27", "entry" : "101 CONVENTION CTR DR"},
                          {"id" : "28", "other_field": "28", "entry" : "3885 S DECATUR BLVD"},
                          {"id" : "29", "other_field": "29", "entry" : "504 DOROTHY ST"},
                          {"id" : "30", "other_field": "30", "entry" : "5322 FIARVIEW AVE"},
                          {"id" : "31", "other_field": "WORD1", "entry" : "TEST ENTRY"}
                          ]};

var tab_lookup_list_catg={"lookup_list_catg_table" : [
                          //Generic Delete List Catalogue
                          {"id" : "0", "list_name" : "US_GENC_DEL_LST"},
                          {"id" : "1", "list_name" : "CA_GENC_DEL_LST"},
                          {"id" : "2", "list_name" : "UK_GENC_DEL_LST"},
                          {"id" : "3", "list_name" : "BR_GENC_DEL_LST"},
                          {"id" : "4", "list_name" : "MX_GENC_DEL_LST"},
                          //Dirty Word List Catalogue
                          {"id" : "5", "list_name" : "US_DRTY_WRD_LST"},
                          {"id" : "6", "list_name" : "UK_DRTY_WRD_LST"},
                          {"id" : "7", "list_name" : "CA_DRTY_WRD_LST"},
                          {"id" : "8", "list_name" : "BR_DRTY_WRD_LST"},
                          {"id" : "9", "list_name" : "MX_DRTY_WRD_LST"},
                          //Generic Business Name Trade Name List Catalogue
                          {"id" : "10", "list_name" : "US_BUS_NME_TRD_NME_LST"},
                          {"id" : "11", "list_name" : "UK_BUS_NME_TRD_NME_LST"},
                          {"id" : "12", "list_name" : "CA_BUS_NME_TRD_NME_LST"},
                          {"id" : "13", "list_name" : "BR_BUS_NME_TRD_NME_LST"},
                          {"id" : "14", "list_name" : "MX_BUS_NME_TRD_NME_LST"},
                          //Generic Trade Business Name Trade Name List Catalogue
                          {"id" : "15", "list_name" : "US_TRD_BUS_NME_TRD_NME_LST"},
                          {"id" : "16", "list_name" : "UK_TRD_BUS_NME_TRD_NME_LST"},
                          {"id" : "17", "list_name" : "CA_TRD_BUS_NME_TRD_NME_LST"},
                          {"id" : "18", "list_name" : "BR_TRD_BUS_NME_TRD_NME_LST"},
                          {"id" : "19", "list_name" : "MX_TRD_BUS_NME_TRD_NME_LST"},
                          //Generic Address Filter List Catalogue
                          {"id" : "20", "list_name" : "US_GENC_ADR_FLTR_LST"},
                          {"id" : "22", "list_name" : "UK_GENC_ADR_FLTR_LST"},
                          {"id" : "23", "list_name" : "CA_GENC_ADR_FLTR_LST"},
                          {"id" : "24", "list_name" : "BR_GENC_ADR_FLTR_LST"},
                          {"id" : "25", "list_name" : "MX_GENC_ADR_FLTR_LST"},
                          {"id" : "26", "list_name" : "US_GENC_DEL_LST-outField:id"},
                          {"id" : "27", "list_name" : "US_GENC_DEL_LST-searchField:other_field-matchOption:partialWholePhrase"}
                          ]};

var tab_list_catg_entries_mapper={"list_catg_entries_mapper" : [
                          //Generic Delete List Entries - US_GENC_DEL_LST Catalogue
                          {"lookup_list_catg_table" : [0,26,27], "ie_genc_lookup_list_entry_table" : [1,3,4,6]},
                          {"lookup_list_catg_table" : [27], "ie_genc_lookup_list_entry_table" : [31]},
                          {"lookup_list_catg_table" : "1", "ie_genc_lookup_list_entry_table" : "7"},
                          //Generic Delete List Entries - UK_GENC_DEL_LST Catalogue
                          {"lookup_list_catg_table" : "2", "ie_genc_lookup_list_entry_table" : "0"},
                          {"lookup_list_catg_table" : "2", "ie_genc_lookup_list_entry_table" : "2"},
                          {"lookup_list_catg_table" : "2", "ie_genc_lookup_list_entry_table" : "5"},
                          {"lookup_list_catg_table" : "2", "ie_genc_lookup_list_entry_table" : "6"},
                          {"lookup_list_catg_table" : "3", "ie_genc_lookup_list_entry_table" : "8"},
                          {"lookup_list_catg_table" : "4", "ie_genc_lookup_list_entry_table" : "9"},
                          {"lookup_list_catg_table" : "5", "ie_genc_lookup_list_entry_table" : "7"},
                          {"lookup_list_catg_table" : "6", "ie_genc_lookup_list_entry_table" : "12"},
                          //Dirty Word List Entries - CA_DRTY_WRD_LST Catalogue
                          {"lookup_list_catg_table" : "7", "ie_genc_lookup_list_entry_table" : "8"},
                          {"lookup_list_catg_table" : "7", "ie_genc_lookup_list_entry_table" : "10"},
                          {"lookup_list_catg_table" : "7", "ie_genc_lookup_list_entry_table" : "12"},
                          {"lookup_list_catg_table" : "8", "ie_genc_lookup_list_entry_table" : "14"},
                          //Dirty Word List Entries - MX_DRTY_WRD_LST Catalogue
                          {"lookup_list_catg_table" : "9", "ie_genc_lookup_list_entry_table" : "7"},
                          {"lookup_list_catg_table" : "9", "ie_genc_lookup_list_entry_table" : "8"},
                          {"lookup_list_catg_table" : "9", "ie_genc_lookup_list_entry_table" : "11"},
                          {"lookup_list_catg_table" : "9", "ie_genc_lookup_list_entry_table" : "12"},
                          {"lookup_list_catg_table" : [10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25],
                            "ie_genc_lookup_list_entry_table" : "14"}
                          ]};


var tab_ie_geo_ref_id_lst = {
   "genc_lookup_eval_rules":[
      {
         "geo_ref_id":"1073",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].NME_ENTR_VW||NULL||NME_TEXT",
         "operator":"LU||EXACT_MATCH",
         "right_operand":"luLookupListCatg-US_GENC_DEL_LST",
         "success_output":"passed",
         "failure_output":"failed"
      },
      {
         "geo_ref_id":"892",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].NME_ENTR_VW||STDN_APPL_CD=13135||NME_TEXT",
         "operator":"LU||PARTIAL_MATCH",
         "right_operand":"luLookupListCatg-CA_GENC_DEL_LST",
         "success_output":"passed",
         "failure_output":"failed"
      },
      {
         "geo_ref_id":"1073",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].NME_ENTR_VW||STDN_APPL_CD=13135||NME_TEXT",
         "operator":"LU||PARTIAL_MATCH",
         "right_operand":"luLookupListCatg-US_DRTY_WRD_LST",
         "success_output":"passed",
         "failure_output":"failed"
      },
      {
         "geo_ref_id":"892",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].NME_ENTR_VW||STDN_APPL_CD=13135||NME_TEXT",
         "operator":"LU||EXACT_MATCH",
         "right_operand":"luLookupListCatg-CA_DRTY_WRD_LST",
         "success_output":"passed",
         "failure_output":"failed"
      }
   ]
};


var tab_test_exact_match = {
   "test_exact_match_table":[
      {
         "geo_ref_id":"1073",
         "left_operand":"VER_ORG.BIZ_NAME||NULL||NULL",
         "operator":"LU||EXACT_MATCH",
         "right_operand":"luLookupListCatg-US_GENC_DEL_LST",
         "success_output":"passed",
         "failure_output":"failed"
      }
   ]
};


var tab_test_partial_match = {
   "test_partial_match_table":[
      {
         "geo_ref_id":"1073",
         "left_operand":"VER_ORG.BIZ_NAME||NULL||NULL",
         "operator":"LU||PARTIAL_MATCH",
         "right_operand":"luLookupListCatg-US_GENC_DEL_LST",
         "success_output":"passed",
         "failure_output":"failed"
      }
   ]
};

var tab_test_alt_output_fld = {
   "test_alt_output_fld_table":[
      {
         "geo_ref_id":"1073",
         "left_operand":"VER_ORG.BIZ_NAME||NULL||NULL",
         "operator":"LU||PARTIAL_MATCH",
         "right_operand":"luLookupListCatg-US_GENC_DEL_LST-outField:id",
         "success_output":"passed",
         "failure_output":"failed"
      }
   ]
};



var tab_test_alt_search_fld = {
   "test_alt_search_fld_table":[
      {
         "geo_ref_id":"1073",
         "left_operand":"VER_ORG.BIZ_NAME||NULL||NULL",
         "operator":"LU||EXACT_MATCH",
         "right_operand":"luLookupListCatg-US_GENC_DEL_LST-searchField:other_field-matchOption:partialWholePhrase",
         "success_output":"passed",
         "failure_output":"failed"
      },
      {
         "geo_ref_id":"1073",
         "left_operand":"VER_ORG.BIZ_NAME||NULL||NULL",
         "operator":"LU||LIST_DEFINED",
         "right_operand":"luLookupListCatg-US_GENC_DEL_LST-searchField:other_field-matchOption:partialWholePhrase",
         "success_output":"passed",
         "failure_output":"failed"
      }
   ]
};

/*
 * Load into memory the lookup list repository
 */
ruleFactory.createQueryableDataRule(JSON.stringify(tab_ie_genc_lookup_list), "luGencLookupList");
ruleFactory.createQueryableDataRule(JSON.stringify(tab_lookup_list_catg), "luLookupListCatg");
ruleFactory.createQueryableDataRule(JSON.stringify(tab_list_catg_entries_mapper), "luLookupListCatgEntriesMapper");

var delim = selCriteria.SEARCH_CRITERIA_DELIM;
var m = new java.util.HashMap(3);
m.put("geo_ref_id", "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW"
  + delim + selCriteria.SEARCH_CRITERIA_NULL + delim + "GEO_REF_ID");

ruleFactory.createQueryableRuleConfigurationDataRule(
                JSON.stringify(tab_ie_geo_ref_id_lst),
                "lookupEvalRule",
                m,
                "resultEvaluationOption==allMustPass",
                null
  );


var geoRefInfSrCdFilter = new java.util.HashMap(1);
geoRefInfSrCdFilter.put("geo_ref_id", "VER_ORG.GEO_REF_ID"
  + delim + selCriteria.SEARCH_CRITERIA_NULL + delim + selCriteria.SEARCH_CRITERIA_NULL);
ruleFactory.createQueryableRuleConfigurationDataRule(
                JSON.stringify(tab_test_exact_match),
                "luExactMatch",
                geoRefInfSrCdFilter,
                "resultEvaluationOption==allMustPass",
                null
  );


ruleFactory.createQueryableRuleConfigurationDataRule(
                JSON.stringify(tab_test_partial_match),
                "luPartialMatch",
                geoRefInfSrCdFilter,
                "resultEvaluationOption==allMustPass",
                null
  );

ruleFactory.createQueryableRuleConfigurationDataRule(
                JSON.stringify(tab_test_alt_output_fld),
                "luAltOutputFld",
                geoRefInfSrCdFilter,
                "resultEvaluationOption==allMustPass",
                null
  );

ruleFactory.createQueryableRuleConfigurationDataRule(
                JSON.stringify(tab_test_alt_search_fld),
                "luAltSearchFld",
                geoRefInfSrCdFilter,
                "resultEvaluationOption==anyCanPass",
                null
  );

ruleFactory.createQueryableRuleConfigurationDataRule(
                JSON.stringify(tab_test_alt_search_fld),
                "luAltSearchFldUsingTargetRuleOutput",
                geoRefInfSrCdFilter,
                "resultEvaluationOption==anyCanPass&useTargetRuleOutput==true",
                null
  );