(function executeRule(pData) {
    /*
     * Set up the search criteria to identify the piece of data that our rule
     * should operate on
     */
     // Sets up search path of the node
    var searchPath = com.exsoinn.util.epf.SearchPath.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR");

    // Defines filter that says to get normalized STD_STRG_VW node
    var filter = com.exsoinn.util.epf.Filter.valueOf("STD_STRG_VW.STDN_APPL_CD=13135");

    // Lastly, add more granularity to get at the field which value we want to use in
    // the comparison, 'DNB_DATA_PRVD_CD' in this case
    var selectField = com.exsoinn.util.epf.TargetElements.valueOf("DNB_DATA_PRVD_CD");


    // Set the value we want to compare to (the right hand side operand)
    var extraParams = new java.util.HashMap(1);
    extraParams.put(com.exsoinn.ie.rule.RuleConstants.ARG_RIGHT_OPERAND, "19555");

    var rule = com.exsoinn.ie.rule.AbstractRule.createRelOpRule("==");
    var ruleResult = rule.apply(pData, searchPath, filter, selectField, extraParams);
    print("Rule result is " + ruleResult.evaluateResult());
    print("Rule result info is " + ruleResult.toString());
});