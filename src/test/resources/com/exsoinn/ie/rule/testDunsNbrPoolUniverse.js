var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");

var tab_duns_nbr_pool = {
	"duns_nbr_pool_entry_table":[
        //US
        {
            "id": "0",
            "entry": "1073",
            "duns_nbr":"234534567"
        },
        //CA
        {
            "id": "1",
            "entry": "892",
            "duns_nbr":"564534231"
        },
        //UK
        {
            "id": "2",
            "entry": "1067",
            "duns_nbr":"453423675"
        }
	]
};

ruleFactory.createQueryableDataRule(JSON.stringify(tab_duns_nbr_pool), "dunsNbrPoolUniverseTbl", true);