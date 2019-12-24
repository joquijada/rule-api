/*
* Checks If Business Name Is Of Trade Source
*/
function isTradeInfoSrcCd(jCntxt) {
    print("JMQ: isTradeInfoSrcCd");
    var spClass = Java.type('com.exsoinn.util.epf.SearchPath');
    var searchPath = spClass.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].DNB_DATA_PRVD_CD");
    var selectFld = com.exsoinn.util.epf.TargetElements.valueOf("DNB_DATA_PRVD_CD");

    var trdInfoSrcCdLst = new java.util.ArrayList(7);
    trdInfoSrcCdLst.add("15346");
    trdInfoSrcCdLst.add("19555");
    trdInfoSrcCdLst.add("18019");
    trdInfoSrcCdLst.add("18014");
    trdInfoSrcCdLst.add("18013");
    trdInfoSrcCdLst.add("18018");
    trdInfoSrcCdLst.add("18068");

    var isTradeInfoSrcCdRl = com.exsoinn.ie.rule.AbstractRule.createCheckAgainstCollectionRule(trdInfoSrcCdLst);
    var isTradeInfoSrcCdRlRslt = isTradeInfoSrcCdRl.apply(jCntxt, searchPath, null, selectFld, null);

    return isTradeInfoSrcCdRlRslt.evaluateResult();
}

/*
* Checks If Business Name In The Delete List
*/
function isNameInDeleteList(jCntxt,busNmeNodePath,
            geoRefId,spClass,abstractRuleClass,
            matrixDecInpClass,searchPath,selectFld,delLstTbleNme){
    print("JMQ: isNameInDeleteList")
    if (isUSGeoRefCode(jCntxt)){
        var dataSearchRes = jCntxt.findElement(searchPath, null, selectFld, null);
        var inpBusNmeVal = dataSearchRes.get("NME_TEXT").stringRepresentation();

        var delLstWordFlg = abstractRuleClass.matrixDecision(
                          delLstTbleNme,
                          matrixDecInpClass.valueOf("txt_desc="+inpBusNmeVal+";geo_ref_id="+geoRefId),
                          "actv_flag");

        if (delLstWordFlg!=null && delLstWordFlg!=0){
            return true;
        }
    }
    return false;
}

/*
* Checks If The Record Is US Market
*/
function isUSGeoRefCode(jCntxt){
    print("JMQ: isUSGeoRefCode")
    var spClass = Java.type('com.exsoinn.util.epf.SearchPath');
    var searchPath = spClass.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[1].ADR_ENTR_VW");
    var selectFld = com.exsoinn.util.epf.TargetElements.valueOf("GEO_REF_ID");
    var geoRefRange = new java.util.ArrayList(3);
    geoRefRange.add("1073");
    geoRefRange.add("1021");
    geoRefRange.add("1074");

    var isUSGeoRefCodeRl = com.exsoinn.ie.rule.AbstractRule.createCheckAgainstCollectionRule(geoRefRange);
    var isUSGeoRefCodeRlRslt = isUSGeoRefCodeRl.apply(jCntxt, searchPath, null, selectFld, null);

    return isUSGeoRefCodeRlRslt.evaluateResult();
}

/*
* Gets The GEO REF ID Value From Address Name Tag
*/
function getGeoRefIdVal(jCntxt) {
    print("JMQ: getGeoRefIdVal")
    var spClass = Java.type('com.exsoinn.util.epf.SearchPath');
    var searchPath = spClass.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[1].ADR_ENTR_VW");
    var selectFld = com.exsoinn.util.epf.TargetElements.valueOf("GEO_REF_ID");
    var dataSearchRes = jCntxt.findElement(searchPath, null, selectFld, null);

    return dataSearchRes.get("GEO_REF_ID");
}

/*
* Checks If The Business Name In Delete List
*/
function testWordAgainstDeleteList(jCntxt,busNmeNodePath,
            geoRefId,spClass,abstractRuleClass,
            matrixDecInpClass,searchPath,selectFld) {
    print("JMQ: testWordAgainstDeleteList")
    print(">>>BUSINESS_NAME_DELETE_LIST_VALIDATION<<<");

    if (isUSGeoRefCode(jCntxt)){
        var dataSearchRes = jCntxt.findElement(searchPath, null, selectFld, null);
        var inpBusNmeVal = dataSearchRes.get("NME_TEXT").stringRepresentation();
        var strCls = new java.lang.String(inpBusNmeVal);
        var frmtBusNme = strCls.toUpperCase().replaceAll("[\\W_]", " ");
        var cnflWordExtMtch = abstractRuleClass.matrixDecision(
                          com.exsoinn.ie.rule.RuleName.DL_GENERIC_DELETE_LIST,
                          matrixDecInpClass.valueOf("exct_mtch_indc=1;geo_ref_id="+geoRefId),
                          "cust_nme");
        var cnflWordPartlMtch = abstractRuleClass.matrixDecision(
                                  com.exsoinn.ie.rule.RuleName.DL_GENERIC_DELETE_LIST,
                                  matrixDecInpClass.valueOf("exct_mtch_indc=0;geo_ref_id="+geoRefId),
                                  "cust_nme");

        if ((cnflWordExtMtch!=null && (strCls.toUpperCase()==cnflWordExtMtch ||
                frmtBusNme==cnflWordExtMtch)) ||
                (cnflWordPartlMtch!=null && (strCls.toUpperCase().contains(cnflWordPartlMtch)||
                frmtBusNme.toUpperCase().contains(cnflWordPartlMtch)))) {
            return true;
        }
    }
    return false;
}

/*
* Checks If The Business Name Starts With Zero
*/
function strtWthZero(jCntxt,busNmeNodePath){
    print("JMQ: strtWthZero")
    print(">>>STARTS_WITH_00_BUSINESS_NAME<<<");
    var strtsWithZeroRl = com.exsoinn.ie.rule.AbstractRule.createRegExRule("\\b[0][0]|[0]\\s[0]");
    var vldtnRslt = strtsWithZeroRl.apply(jCntxt, busNmeNodePath, null, null, null);
    print("Rule result is " + vldtnRslt.evaluateResult());
    print("Rule result info is " + vldtnRslt.toString());
}

/*
* Checks The Allowable Number Of Digits In Business Name
*/
function allowableNmbrsInNme(jCntxt, busNmeNodePath) {
    print("JMQ: allowableNmbrsInNme")
    print(">>>ALLOWABLE_NUMBERS_IN_BUSINESS_NAME<<<");
    var extraParams = new java.util.HashMap(1);
    extraParams.put(com.exsoinn.ie.rule.RuleConstants.ARG_RIGHT_OPERAND, "4");

    var allowableNmbrsInNmeRl = com.exsoinn.ie.rule.AbstractRule.createStringHandlerRule("STRNG_LNGTH_GRTR_THAN","RETAIN_NUMBERS_ONLY");
    var vldtnRslt = allowableNmbrsInNmeRl.apply(jCntxt, busNmeNodePath, null, null, extraParams);
    print("Rule result is " + vldtnRslt.evaluateResult());
    print("Rule result info is " + vldtnRslt.toString());
}

/*
* Checks If The Business Name Has Particular Sequence Of Digits
*/
function hasSeqOfDigits(jCntxt, busNmeNodePath) {
    print("JMQ: hasSeqOfDigits")
    print(">>>SEQUENCE_OF_DIGITS_BUSINESS_NAME<<<");
    var hasFirstFewDigitsRl = com.exsoinn.ie.rule.AbstractRule.createRegExRule("(^\d{9})|"+
                                                                            "([0-9]{3}-[0-9]{2}-[0-9]{4})|"+
                                                                            "([0-9]{2}-[0-9]{3}-[0-9]{4})|"+
                                                                            "([0-9]{3}\s[0-9]{2}\s[0-9]{4})");
    var hasFirstFewDigitsRlRslt = hasFirstFewDigitsRl.apply(jCntxt, busNmeNodePath, null, null, null);
    print("Rule result is " + hasFirstFewDigitsRlRslt.evaluateResult());
    print("Rule result info is " + hasFirstFewDigitsRlRslt.toString());
}

/*
* Checks If The Business Name Has Numbers And Punctuations Only
*/
function hasNumbersAndPunctOnly(jCntxt,busNmeNodePath){
    print("JMQ: hasNumbersAndPunctOnly")
    print(">>>NUMBERS_AND_PUNCTUATIONS_ONLY_BUSINESS_NAME<<<");
    var hasAlphaRl = com.exsoinn.ie.rule.AbstractRule.createStringHandlerRule("CONTAINS_ALPHA_CHARS");
    var hasAlphaRlRslt = hasAlphaRl.apply(jCntxt, busNmeNodePath, null, null, null);
    var hasNumbersRl = com.exsoinn.ie.rule.AbstractRule.createStringHandlerRule("CONTAINS_NUMERICS");
    var hasNumbersRlRslt = hasNumbersRl.apply(jCntxt, busNmeNodePath, null, null, null);
    var hasPunctRl = com.exsoinn.ie.rule.AbstractRule.createStringHandlerRule("CONTAINS_SPL_CHARS");
    var hasPunctRlRslt = hasPunctRl.apply(jCntxt, busNmeNodePath, null, null, null);

    if (!hasAlphaRlRslt.evaluateResult() && hasNumbersRlRslt.evaluateResult() && hasPunctRlRslt.evaluateResult()) {
        print("Rule result is " + ": true");
        print("Rule result info is " + ": has numbers and punctuations only");
    } else {
        print("Rule result is " + ": false");
        print("Rule result info is " + ": does not have numbers and punctuations only");
    }
}

/*
* Checks If The Business Name Has Numbers Only
*/
function hasNumbersOnly(jCntxt,busNmeNodePath){
    print("JMQ: hasNumbersOnly")
    print(">>>NUMBERS_ONLY_BUSINESS_NAME<<<");
    var hasNumbersOnlyRl = com.exsoinn.ie.rule.AbstractRule.createRegExRule("^\\d+$");
    var vldtnRslt = hasNumbersOnlyRl.apply(jCntxt, busNmeNodePath, null, null, null);
    print("Rule result is " + vldtnRslt.evaluateResult());
    print("Rule result info is " + vldtnRslt.toString());
}

/*
* Checks If The Business Name Has Only Punctuations
*/
function hasOnlyPunct(jCntxt,busNmeNodePath){
    print("JMQ: hasOnlyPunct")
    print(">>>PUNCTUATIONS_ONLY_BUSINESS_NAME<<<");
    var hasOnlyPunctRl = com.exsoinn.ie.rule.AbstractRule.createRegExRule("^[^a-zA-Z0-9]+$");
    var vldtnRslt = hasOnlyPunctRl.apply(jCntxt, busNmeNodePath, null, null, null);
    print("Rule result is " + vldtnRslt.evaluateResult());
    print("Rule result info is " + vldtnRslt.toString());
}

/*
* Checks If The Business Name Is Less Than The Configured Characters
*/
function lessThanConfiguredChars(jCntxt,busNmeNodePath) {
    print("JMQ: lessThanConfiguredChars")
    print(">>>BUSINESS_NAME_LENGTH<<<");
    var extraParams = new java.util.HashMap(1);
    extraParams.put(com.exsoinn.ie.rule.RuleConstants.ARG_RIGHT_OPERAND, "3");

    var lessThanConfigCharsRl = com.exsoinn.ie.rule.AbstractRule.createStringHandlerRule("STRNG_LNGTH_LESS_THAN","REMOVE_SPACE");
    var vldtnRslt = lessThanConfigCharsRl.apply(jCntxt, busNmeNodePath, null, null, extraParams);
    print("Rule result is " + vldtnRslt.evaluateResult());
    print("Rule result info is " + vldtnRslt.toString());
}

/*
* Checks If The Business Name Is Null
*/
function isNull(jCntxt,busNmeNodePath){
    print("JMQ: isNull")
    print(">>>NULL_BUSINESS_NAME<<<");
    var isNullRl = com.exsoinn.ie.rule.AbstractRule.createStringHandlerRule("IS_NULL");
    var vldtnRslt = isNullRl.apply(jCntxt, busNmeNodePath, null, null, null);
    print("Rule result is " + vldtnRslt.evaluateResult());
    print("Rule result info is " + vldtnRslt.toString());
}

/*
* Evaluate Business Name Rules
*/
function evaluate(jCntxt,rawBusNmeNodePath,normBusNmeNodePath,stdBusNmeNodePath){
    print("JMQ: Evaluate Business Name Validation");

    var jsonRuleConfigLookupEval = "{\"genc_lookup_eval_rules\" : [\n " +
            "  {\"geo_ref_id\" : \"1073\", \"left_operand\" : \"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].NME_ENTR_VW||NULL||NME_TEXT\", \"operator\" : \"LU||EXACT_MATCH\", \"right_operand\" : \"luLookupListCatg-US_GENC_DEL_LST\", \"success_output\":\"passed\", \"failure_output\":\"failed\"}, " +
            "  {\"geo_ref_id\" : \"892\", \"left_operand\" : \"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].NME_ENTR_VW||STDN_APPL_CD=13135||NME_TEXT\", \"operator\" : \"LU||PARTIAL_MATCH\", \"right_operand\" : \"luLookupListCatg-CA_GENC_DEL_LST\", \"success_output\":\"passed\", \"failure_output\":\"failed\"}, " +
            "  {\"geo_ref_id\" : \"1073\", \"left_operand\" : \"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].NME_ENTR_VW||STDN_APPL_CD=13135||NME_TEXT\", \"operator\" : \"LU||PARTIAL_MATCH\", \"right_operand\" : \"luLookupListCatg-US_DRTY_WRD_LST\", \"success_output\":\"passed\", \"failure_output\":\"failed\"}, " +
            "  {\"geo_ref_id\" : \"892\", \"left_operand\" : \"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].NME_ENTR_VW||STDN_APPL_CD=13135||NME_TEXT\", \"operator\" : \"LU||EXACT_MATCH\", \"right_operand\" : \"luLookupListCatg-CA_DRTY_WRD_LST\", \"success_output\":\"passed\", \"failure_output\":\"failed\"}]}";

    var lookupRuleName = "lookupEvalRuleBiz";
    var geoRefId = getGeoRefIdVal(jCntxt);
    var spClass = Java.type('com.exsoinn.util.epf.SearchPath');
    var abstractRuleClass = Java.type('com.exsoinn.ie.rule.AbstractRule');
    var matrixDecInpClass = Java.type('com.exsoinn.ie.rule.MatrixDecisionInput');
    var searchPath = spClass.valueOf(rawBusNmeNodePath);
    var selectFld = com.exsoinn.util.epf.TargetElements.valueOf("NME_TEXT");

    isNull(jCntxt,rawBusNmeNodePath);
    lessThanConfiguredChars(jCntxt,rawBusNmeNodePath);
    hasOnlyPunct(jCntxt,rawBusNmeNodePath);
    hasNumbersOnly(jCntxt,rawBusNmeNodePath);
    hasNumbersAndPunctOnly(jCntxt,rawBusNmeNodePath);
    hasSeqOfDigits(jCntxt, rawBusNmeNodePath);
    allowableNmbrsInNme(jCntxt, rawBusNmeNodePath);
    strtWthZero(jCntxt,normBusNmeNodePath);

    print(">>>LOOKUP SERVICE EVALUATION<<<");
    var map = new java.util.HashMap(3);
    map.put("geo_ref_id","VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW||NULL||GEO_REF_ID");
    com.exsoinn.ie.rule.AbstractRule.createQueryableRuleConfigurationDataRule(
      jsonRuleConfigLookupEval,lookupRuleName,map,"resultEvaluationOption==allMustPass", null);
    var res = com.exsoinn.ie.rule.AbstractRule.executeConfigurableRule(lookupRuleName, jCntxt);
    print("Lookup Result: "+res);
}

/*
* Business Name Validation Invoker
*/
function businessNameValidation(jCntxt){
    var rawBusNmeNodePath = com.exsoinn.util.epf.SearchPath.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[0].NME_ENTR_VW.NME_TEXT");
    var normBusNmeNodePath = com.exsoinn.util.epf.SearchPath.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].NME_ENTR_VW.NME_TEXT");
    var stdBusNmeNodePath = com.exsoinn.util.epf.SearchPath.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[2].NME_ENTR_VW.NME_TEXT");

    evaluate(jCntxt,rawBusNmeNodePath,normBusNmeNodePath,stdBusNmeNodePath);
}
