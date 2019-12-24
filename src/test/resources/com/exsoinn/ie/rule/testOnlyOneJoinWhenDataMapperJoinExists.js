var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");


/*
 * To test that enforcement happens on limit of only one join when the join is to a data mapper. In this case, the
 * "entry_table" data source joins with both a mapper and another source, which is a no-no, at least for now.
 */
var list_tab = {
   "list_table":[
      {
         "id":"0",
         "list_name":"list0"
      },
      {
         "id":"1",
         "list_name":"list1"
      }
   ]
};


var mapper_tab = {
   "mapper_table":[
      {
         "list_table":"0",
         "entry_table":"0"
      },
      {
         "list_table":"1",
         "entry_table":"1"
      }
   ]
};


var entry_tab = {
   "entry_table":[
      {
         "id":"0",
         "entry":"entry0",
         "some_other_table":"0"
      },
      {
         "id":"1",
         "entry":"entry1",
         "some_other_table":"1"
      }
   ]
};

var some_other_ds = {
   "some_other_table":[
      {
         "id":"0",
         "field":"val0"
      },
      {
         "id":"1",
         "field":"val1"
      }
   ]
};

ruleFactory.createQueryableDataRule(JSON.stringify(list_tab), "listTabJoinLimit");
ruleFactory.createQueryableDataRule(JSON.stringify(mapper_tab), "mapperTabJoinLimit");
ruleFactory.createQueryableDataRule(JSON.stringify(entry_tab), "entryTabJoinLimit");
ruleFactory.createQueryableDataRule(JSON.stringify(some_other_ds), "someOtherTabJoinLimit");