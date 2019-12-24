var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");

var json_sp = {
   "search_path":[
      {
         "id":"0",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15200||CFDC_LVL_VAL"
      },
      {
         "id":"1",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15200||MTCH_GRD_TEXT"
      }
   ]
};

// TODO: Support more than one join per table, with constraint that only unique row ID's are expected.
// TODO: Question: If above is supported, should we restrict that none of the joins can be for a data mapper, meaning that
// TODO: a data source that joins with a data mapper cannot have more than join. Reason is, when not all joins
// TODO: contain same number of rows, then logic to merge becomes too complex, unless we implement "limiting
// TODO: agent" concept like in Stoichiometry
var json_operation = {
   "lu_operation":[
      {
         "id":"0",
         "operator":">"
      },
      {
           "id":"1",
           "operator":"<"
      },
      {
            "id":"2",
            "operator":"=="
      },
      {
           "id":"3",
           "operator":">"
      }
   ]
};


var json_right_op = {
   "lu_right_operand":[
      {
         "id":"0",
         "right_operand":"1"
      },
      {
         "id":"1",
         "right_operand":"4"
      },
      {
         "id":"2",
         "right_operand":"5"
      }
   ]
};


ruleFactory.createQueryableDataRule(JSON.stringify(json_sp), "luSearchPath");
ruleFactory.createQueryableDataRule(JSON.stringify(json_right_op), "luRightOperand");
ruleFactory.createQueryableDataRule(JSON.stringify(json_operation), "luOperation");