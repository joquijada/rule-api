//Initialize Class Instance
var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");
var ruleName = Java.type("com.exsoinn.ie.rule.RuleName");

//Sample Data From IEDWMOWNER.GENC_DEL_LIST Table
var tab_generic_delete_list = {"genc_del_list": [
                            {"cust_nme": "ADVANCE CONCEPT IN", "exct_mtch_indc": "0", "geo_ref_id": "1073"},
                            {"cust_nme": "BEST PAPER CEMENT", "exct_mtch_indc": "0", "geo_ref_id": "1073"},
                            {"cust_nme": "AMERICA INSPECT TEST IN", "exct_mtch_indc": "0", "geo_ref_id": "1073"},
                            {"cust_nme": "LIFE TIME WIRELESS", "exct_mtch_indc": "1", "geo_ref_id": "1073"}]};

//Sample Data From IEDWMOWNER.GENC_BUS_NME_TRDG_NME Table
var tab_bus_and_trd_delete_list = {"genc_bus_nme_trdg_nme": [
                            {"txt_desc": "LIFE TIME WIRELESS", "geo_ref_id": "1073", "actv_flag": "1"},
                            {"txt_desc": "ICE BFC", "geo_ref_id": "1073", "actv_flag": "0"},
                            {"txt_desc": "FIRE DEPT", "geo_ref_id": "1073", "actv_flag": "1"},
                            {"txt_desc": "EOIR DOJ", "geo_ref_id": "1073", "actv_flag": "0"}]};

//Sample Data From IEDWMOWNER.TRADE_BUS_NME_TRDG_NME Table
var tab_trade_bus_nme_trd_nme = {"trade_bus_nme_trdg_nme": [
                            {"txt_desc": "LIFE TIME WIRELESS", "geo_ref_id": "1073", "actv_flag": "1"},
                            {"txt_desc": "BALTIMORE COUNT", "geo_ref_id": "1073", "actv_flag": "0"},
                            {"txt_desc": "NATIONAL BANK", "geo_ref_id": "1073", "actv_flag": "1"},
                            {"txt_desc": "BROWARD SHERIFF", "geo_ref_id": "1073", "actv_flag": "0"}]};

//Create Table Data Object
ruleFactory.createQueryableDataRule(JSON.stringify(tab_generic_delete_list), ruleName.DL_GENERIC_DELETE_LIST);
ruleFactory.createQueryableDataRule(JSON.stringify(tab_bus_and_trd_delete_list), ruleName.DL_BUS_TRD_DEL_LIST);
ruleFactory.createQueryableDataRule(JSON.stringify(tab_trade_bus_nme_trd_nme), ruleName.DL_TRD_BUS_NME_TRD_NME);