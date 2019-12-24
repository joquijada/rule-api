(function checkBusinessNameNullCheck(pJsonStr) {
    var busNmeTxt = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[0].NME_ENTR_VW.NME_TEXT";
    var strHndrRl = com.exsoinn.ie.rule.AbstractRule.createStringHandlerRule("IS_NOT_NULL");
    var ruleResult = strHndrRl.apply(pJsonStr, busNmeTxt, null, null, null);
    print("Rule result is " + ruleResult.evaluateResult());
    print("Rule result info is " + ruleResult.toString());

    if (ruleResult.evaluateResult()) {
        var regExRl = com.exsoinn.ie.rule.AbstractRule.createRegExRule("^[A-z]+$");
        var nmeValidRleRslt = regExRl.apply(pJsonStr, busNmeTxt, null, null, null);
        print("Rule result is " + nmeValidRleRslt.evaluateResult());
        print("Rule result info is " + nmeValidRleRslt.toString());
    }

    if (!nmeValidRleRslt.evaluateResult()) {
        var isUpperRl = com.exsoinn.ie.rule.AbstractRule.createStringHandlerRule("IS_UPPER");
        var busNmeIsUpperRlRslt = isUpperRl.apply(pJsonStr, busNmeTxt, null, null, null);
        print("Rule result is " + busNmeIsUpperRlRslt.evaluateResult());
        print("Rule result info is " + busNmeIsUpperRlRslt.toString());
    }

    if (busNmeIsUpperRlRslt.evaluateResult()) {
        var isTitleCaseRl = com.exsoinn.ie.rule.AbstractRule.createStringHandlerRule("IS_TITLE_CASE");
        var busNmeIsTitleCaseRlRslt = isTitleCaseRl.apply(pJsonStr, busNmeTxt, null, null, null);
        print("Rule result is " + busNmeIsTitleCaseRlRslt.evaluateResult());
        print("Rule result info is " + busNmeIsTitleCaseRlRslt.toString());
    }
});


