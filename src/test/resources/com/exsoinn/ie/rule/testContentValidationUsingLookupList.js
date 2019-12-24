var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");
var ruleName = Java.type("com.exsoinn.ie.rule.RuleName");
var selCriteria = Java.type("com.exsoinn.util.epf.SelectionCriteria");


var tab_lu_entry ={
   "lookup_entry_table":[
      {
         "id":"0",
         "entry":"a bad word"
      },
      {
         "id":"1",
         "entry":"bad word phrase"
      },
      {
         "id":"2",
         "entry":"naughty phrase"
      },
      {
         "id":"3",
         "entry":"Biz Delete Phrase I"
      },
      {
         "id":"4",
         "entry":"Biz Delete Phrase II"
      },
      {
         "id":"5",
         "entry":"new bad word"
      }
   ]
};


var tab_lu_list = {
   "lookup_list_table":[
      /*
       * Below two lists test that in same market (US), one info source code (12345) can do exact match, while another (54321)
       * can do partial (sub-string) match, on the same term
       */
      {
         "id":"0",
         "list_name":"geoRefId1073infoSrcCd12345-matchOption:exact-type:dirty"
      },
      {
         "id":"1",
         "list_name":"geoRefId1073infoSrcCd54321-matchOption:partial-type:dirty"
      },
      {
         // Below list is for Canada, because it wants to do partial match but on whole words only, not sub-string matches
         "id":"2",
         "list_name":"geoRefId892-matchOption:partialWholePhrase-type:dirty"
      },
      {
         // US source "99999" wants to do match at beginning of input only
         "id":"3",
         "list_name":"geoRefId1073infoSrcCd99999-matchOption:startsWith-type:dirty"
      },
      {
         // US source "88888" wants to do match at end of input only
         "id":"4",
         "list_name":"geoRefId1073infoSrcCd88888-matchOption:endsWith-type:dirty"
      },
      {
         // *All* markets want to match delete word at beginning of input...
         "id":"5",
         "list_name":"geoRefIdAll-matchOption:startsWith-type:delete"
      },
      {
         // ...but Mexico wants to check at end of input, not the beginning
         "id":"6",
         "list_name":"geoRefId984-matchOption:endsWith-type:delete"
      }
   ]
};




var tab_lu_list_entry_mapper = {
   "lookup_mapper_table":[
      {
         "lookup_list_table":[0, 1, 2],
         "lookup_entry_table":"0"
      },
      {
         "lookup_list_table":[0, 1, 2],
         "lookup_entry_table":"1"
      },
      {
         "lookup_list_table":[0, 1, 2, 3, 4],
         "lookup_entry_table":"2"
      },
      {
         "lookup_list_table":[5,6],
         "lookup_entry_table":"3"
      },
      {
         "lookup_list_table":[5,6],
         "lookup_entry_table":"4"
      },
      {
         "lookup_list_table":[2],
         "lookup_entry_table":"5"
      }
   ]
};


var tab_content_vld_rule_set_dirty_word = {
   "content_vld_rule_set_dirty_word":[
      // US source "12345" wishes to do exact match on the configured list values
      {
         "geo_ref_id":"1073",
         "info_src_cd":"12345",
         "left_operand":"VER_ORG.BIZ_NAME||NULL||NULL",
         "operator":"LU||LIST_DEFINED",
         "right_operand":"contentVldList-geoRefId1073infoSrcCd12345-matchOption:exact-type:dirty",
         "success_output":"passed",
         "failure_output":"failed"
      },
      // US source "54321" wishes to do partial match anywhere in the string of the configured list values
      {
         "geo_ref_id":"1073",
         "info_src_cd":"54321",
         "left_operand":"VER_ORG.BIZ_NAME||NULL||NULL",
         "operator":"LU||LIST_DEFINED",
         "right_operand":"contentVldList-geoRefId1073infoSrcCd54321-matchOption:partial-type:dirty",
         "success_output":"passed",
         "failure_output":"failed"
      },
      /* Canada wishes to do partial match anywhere in the string, but the match has to be on whole
       *  words only. This apples to *all* sources within Canada sources
       */
      {
         "geo_ref_id":"892",
         "info_src_cd":".*",
         "left_operand":"VER_ORG.BIZ_NAME||NULL||NULL",
         "operator":"LU||LIST_DEFINED",
         "right_operand":"contentVldList-geoRefId892-matchOption:partialWholePhrase-type:dirty",
         "success_output":"passed",
         "failure_output":"failed"
      },
      {
         "geo_ref_id":"1073",
         "info_src_cd":"99999",
         "left_operand":"VER_ORG.BIZ_NAME||NULL||NULL",
         "operator":"LU||LIST_DEFINED",
         "right_operand":"contentVldList-geoRefId1073infoSrcCd99999-matchOption:startsWith-type:dirty",
         "success_output":"passed",
         "failure_output":"failed"
      },
      {
         "geo_ref_id":"1073",
         "info_src_cd":"88888",
         "left_operand":"VER_ORG.BIZ_NAME||NULL||NULL",
         "operator":"LU||LIST_DEFINED",
         "right_operand":"contentVldList-geoRefId1073infoSrcCd88888-matchOption:endsWith-type:dirty",
         "success_output":"passed",
         "failure_output":"failed"
      }
   ]
};


var tab_content_vld_rule_set_delete_word = {
   "content_vld_rule_set_delete_word":[
      // Mexico market checks delete word match at end of input business name
      {
         "geo_ref_id":"984",
         "info_src_cd":".*",
         "left_operand":"VER_ORG.BIZ_NAME||NULL||NULL",
         "operator":"LU||LIST_DEFINED",
         "right_operand":"contentVldList-geoRefId984-matchOption:endsWith-type:delete",
         "success_output":"passed",
         "failure_output":"failed"
      },
      // Rest of markets do delete word match at beginning of business name
      {
         "geo_ref_id":"DEFAULT",
         "info_src_cd":"DEFAULT",
         "left_operand":"VER_ORG.BIZ_NAME||NULL||NULL",
         "operator":"LU||LIST_DEFINED",
         "right_operand":"contentVldList-geoRefIdAll-matchOption:startsWith-type:delete",
         "success_output":"passed",
         "failure_output":"failed"
      }
   ]
};


var delim = selCriteria.SEARCH_CRITERIA_DELIM;
var geoRefInfSrCdFilter = new java.util.HashMap(3);
geoRefInfSrCdFilter.put("geo_ref_id", "VER_ORG.GEO_REF_ID"
  + delim + selCriteria.SEARCH_CRITERIA_NULL + delim + selCriteria.SEARCH_CRITERIA_NULL);
geoRefInfSrCdFilter.put("info_src_cd", "VER_ORG.INFO_SRC_CD"
  + delim + selCriteria.SEARCH_CRITERIA_NULL + delim + selCriteria.SEARCH_CRITERIA_NULL);


ruleFactory.createQueryableDataRule(JSON.stringify(tab_lu_list), "contentVldList", true);
ruleFactory.createQueryableDataRule(JSON.stringify(tab_lu_entry), "contentVldEntry");
ruleFactory.createQueryableDataRule(JSON.stringify(tab_lu_list_entry_mapper), "contentVldListEntryMapper");

ruleFactory.ruleSetBuilder(JSON.stringify(tab_content_vld_rule_set_dirty_word), "luContentVldRuleDirtyWord", "allMustPass")
  .ruleSetFilter(geoRefInfSrCdFilter).build();

ruleFactory.ruleSetBuilder(JSON.stringify(tab_content_vld_rule_set_delete_word), "luContentVldRuleDeleteWord", "allMustPass")
  .ruleSetFilter(geoRefInfSrCdFilter).build();