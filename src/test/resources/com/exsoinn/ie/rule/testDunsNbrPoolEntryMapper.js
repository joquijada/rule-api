var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");

var tab_duns_nbr_pool_entry_mapper = {
    "duns_nbr_pool_entry_mapper_table":[
        {
        "duns_nbr_pool_list_config_table":"0",
        "duns_nbr_pool_entry_table":[
			0
		    ]
		},
        {
        "duns_nbr_pool_list_config_table":"1",
        "duns_nbr_pool_entry_table":[
			1
		    ]
		},
        {
        "duns_nbr_pool_list_config_table":"2",
        "duns_nbr_pool_entry_table":[
			2
		    ]
		}
	]
};

ruleFactory.createQueryableDataRule(JSON.stringify(tab_duns_nbr_pool_entry_mapper),
  "dunsNbrPoolEntryMapper", true);