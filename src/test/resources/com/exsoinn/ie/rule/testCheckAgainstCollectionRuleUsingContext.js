(function executeRule(pData) {
    print("First test case: Check if the top MSR candidate confidence code value exists in a given list of values");

    /*
     * Set up the search criteria to identify top MSR candidate, and return the confidence
     * code value
     */
     // Sets up search path of the node
    var searchPath = com.exsoinn.util.epf.SearchPath.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF");

    // Defines filter that says to get top MSR candidate (rec code = 15201, and cand rank = 1)
    var filter = com.exsoinn.util.epf.Filter.valueOf("CAND_RNK=1;REGN_STAT_CD=15201");


    /*
     * Instruct Java API which field to return back to pass to the rule object, which
     * the rule object can just blindly get from the search results, namely
     * confidence level value
     */
    var selectField = com.exsoinn.util.epf.TargetElements.valueOf("CFDC_LVL_VAL");


   /*
    * Build the range that confidence level code will be checked against
    */
    var range = new java.util.ArrayList(3);
    range.add("8");
    range.add("9");
    range.add("10");

    var rule = com.exsoinn.ie.rule.AbstractRule.createCheckAgainstCollectionRule(range);
    var ruleResult = rule.apply(pData, searchPath, filter, selectField, null);
    print("Rule result is " + ruleResult.evaluateResult());
    print("Rule result info is " + ruleResult.toString());
    print("\n\n");


    /*
     * Run again, but this time give a range that we know will fail
     */
    print("Second test case: Check if the top MSR candidate confidence code value exists in a given list of values"
    + " which we know will fail.");
    var badRange = new java.util.ArrayList(3);
    badRange.add("7");
    badRange.add("8");
    badRange.add("9");
    rule = com.exsoinn.ie.rule.AbstractRule.createCheckAgainstCollectionRule(badRange);
    ruleResult = rule.apply(pData, searchPath, filter, selectField, null);
    print("Second rule result is " + ruleResult.evaluateResult());
    print("Second rule result info is " + ruleResult.toString());
});