var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");

var tab_inds_code_desc_entry_lists = {
   "inds_code_desc_entry_lists":[
        {
            "id":"0",
            "list_name":"geoRefId1073-indsCdDescTblLst"
        }
        /*{
            "id":"1",
            "list_name":"geoRefId1073--matchOption:partial-type:indsCdDescTblList"
        }*/
   ]
};

ruleFactory.createQueryableDataRule(JSON.stringify(tab_inds_code_desc_entry_lists), "indsCdDescEntryLstTbl", true);