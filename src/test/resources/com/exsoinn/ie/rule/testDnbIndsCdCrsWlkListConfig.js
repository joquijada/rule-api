var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");

var tab_industry_code_crosswalk_list_config = {
   "industry_code_crosswalk_list_config_table":[
      {
         "id":"0",
         "list_name":"geoRefId1073-indsCdCrsWlkLst-outField:dnb_inds_cd"
      }
   ]
};


ruleFactory.createQueryableDataRule(JSON.stringify(tab_industry_code_crosswalk_list_config), "indsCdCrsWlkLstCnfgTbl", true);