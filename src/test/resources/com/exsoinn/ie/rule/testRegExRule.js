(function executeRule(pDataStr) {
    print("First test case: Check that postal code is 5 consecutive digits");
    var searchPath = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.POST_CODE";
    var rule = com.exsoinn.ie.rule.AbstractRule.createRegExRule("^\\d{5}$");
    var ruleResult = rule.apply(pDataStr, searchPath, null, null, null);
    print("Rule result is " + ruleResult.evaluateResult());
    print("Rule result info is " + ruleResult.toString());
    print("\n\n");

    print("Second test case: Test some other value that is not 5 digits from start to finish, re-use"
      + "the same rule block created above (though even if we invoke create() again, the Java"
      + "has cached based on RegEx pattern, and will return same one");
    var searchPath2 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.TLCM_INFO.TLCM_ENTR.TEL_NBR";
    var rule2 = com.exsoinn.ie.rule.AbstractRule.createRegExRule("^\\d{5}$");
    var ruleResult2 = rule2.apply(pDataStr, searchPath2, null, null, null);
    print("Second rule result is " + ruleResult2.evaluateResult());
    print("Second rule result info is " + ruleResult2.toString());
    print("\n\n");

    print("Third test case: Test MGP pattern match against the top SSR candidate");
    var searchPath3 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF";

    // Defines filter that says to get top SSR candidate (rec code = 15200, and cand rank = 1)
    var filter = new java.util.HashMap(1);
    filter.put("CAND_RNK", "1");
    filter.put("REGN_STAT_CD", "15200");

    /*
     * Instruct Java API which field to return back to pass to the rule object, which
     * the rule object can just blindly get from the search results, namely
     * MGP
     */
    var selectField = new java.util.HashSet(1);
    selectField.add('MTCH_GRD_TEXT');

    /*
     * Create rule for a different RegEx, factory should return a brand new Java object
     */
    var rule3 = com.exsoinn.ie.rule.AbstractRule.createRegExRule("^AAAAAZAAFBA$");
    var ruleResult3 = rule3.apply(pDataStr, searchPath3, filter, selectField, null);
    print("Third rule result is " + ruleResult3.evaluateResult());
    print("Third rule result info is " + ruleResult3.toString());
    print("\n\n");

    print("Fourth test case: Try an MGP RegEx that will work");
    var rule4 = com.exsoinn.ie.rule.AbstractRule.createRegExRule("^BFFAAZZBFZF$");
    var ruleResult4 = rule4.apply(pDataStr, searchPath3, filter, selectField, null);
    print("Fourth rule result is " + ruleResult4.evaluateResult());
    print("Fourth rule result info is " + ruleResult4.toString());
});