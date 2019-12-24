var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");

var tab_duns_nbr_pool_list_config = {
   "duns_nbr_pool_list_config_table":[
      {
         "id":"0",
         "list_name":"geoRefId1073-dunsNbrPoolTbl-outField:duns_nbr"
      },
      {
         "id":"1",
         "list_name":"geoRefId892-dunsNbrPoolTbl-outField:duns_nbr"
      },
      {
         "id":"2",
         "list_name":"geoRefId1067-dunsNbrPoolTbl-outField:duns_nbr"
      }
   ]
};


ruleFactory.createQueryableDataRule(JSON.stringify(tab_duns_nbr_pool_list_config), "dunsNbrPoolLstCnfgTbl", true);