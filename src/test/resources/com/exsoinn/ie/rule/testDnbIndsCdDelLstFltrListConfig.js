var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");

var tab_inds_code_del_list_fltr_config = {
   "inds_code_del_list_fltr_config_table":[
      {
         "id":"0",
         "list_name":"geoRefId1073-infoSrcCd15362-indsCdDelLstFltr"
      },
      {
         "id":"1",
         "list_name":"geoRefId1073-infoSrcCdAll-indsCdDelLstFltr"
      }
   ]
};


ruleFactory.createQueryableDataRule(JSON.stringify(tab_inds_code_del_list_fltr_config), "indsCdDelLstFltrCnfgTbl", true);