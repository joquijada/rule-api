package com.exsoinn.ie.rule;

import com.exsoinn.ie.test.TestXMLData;
import com.exsoinn.ie.test.TestData;
import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.EscapeUtil;
import com.exsoinn.util.epf.*;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * TODO: Add unit tests for {@link ContextAugmentingRule}
 * TODO: Throws {@link NotExactlyOneEntryComponentAssociationException} when it should
 * TODO: Add tests for: ListNotFoundException, ignore lsit not found error in LookupExecutorRule.java
 * TODO: Add tests for following rule set features:
 * TODO:   - copy field
 * TODO:   - use as template
 * TODO:   - copy field
 * TODO:   - priority
 * TODO: Add {@link IteratorRule} tests
 * Created by QuijadaJ on 4/25/2017.
 */
public class RuleTest {
    private static final Logger _LOGGER = Logger.getLogger(RuleTest.class);
    private static final int IGNORE_INT = -9999;
    private static final String verOrgXml = TestData.verOrgXml;
    private static final String jsonStr = CommonUtils.convertXmlToJson(verOrgXml);
    private static final String jsonStr2 =
            "{\"range_table\": [{\"id\": \"0\", \"range\": [7,8,9,10]},{\"id\": \"1\", \"range\": [5,6]},{\"id\": \"2\", \"range\": [4,3,2,1,0]}]}";
    private static final Context context = ContextFactory.INSTANCE.obtainContext(jsonStr);
    private static final Context context2 =
            ContextFactory.INSTANCE.obtainContext(CommonUtils.convertXmlToJson(TestData.verOrgXml2));
    private static final Context context3 =
            ContextFactory.INSTANCE.obtainContext(CommonUtils.convertXmlToJson(TestData.verOrgXml3));
    private static final Context itrRlSqlDunsFrmMtchCtx = ContextFactory.INSTANCE.obtainContext(CommonUtils.convertXmlToJson(TestData.itrRlSqlDunsFrmMtchXML));
    private static final String jsonStr3 = "{\"geo_to_conf_lvl_config_table\": [{\"id\": \"0\", \"geo_ref_id\": \"984\", \"rec_type_conf_lvl_config_id\": \"3\"},{\"id\": \"1\", \"geo_ref_id\": \"892\", \"rec_type_conf_lvl_config_id\": \"5\"},{\"id\": \"2\", \"geo_ref_id\": \"1073\", \"rec_type_conf_lvl_config_id\": \"5\"},{\"id\": \"3\", \"geo_ref_id\": \"1021\", \"rec_type_conf_lvl_config_id\": \"1\"},{\"id\": \"4\", \"geo_ref_id\": \"952\", \"rec_type_conf_lvl_config_id\": \"2\"},{\"id\": \"5\", \"geo_ref_id\": \"1074\", \"rec_type_conf_lvl_config_id\": \"3\"},{\"id\": \"6\", \"geo_ref_id\": \"892\", \"rec_type_conf_lvl_config_id\": \"1\"},{\"id\": \"7\", \"geo_ref_id\": \"984\", \"rec_type_conf_lvl_config_id\": \"0\"},{\"id\": \"8\", \"geo_ref_id\": \"DFLT\", \"rec_type_conf_lvl_config_id\": \"1\"},{\"id\": \"9\", \"geo_ref_id\": \"984\", \"rec_type_conf_lvl_config_id\": \"2\"},{\"id\": \"10\", \"geo_ref_id\": \"1021\", \"rec_type_conf_lvl_config_id\": \"4\"},{\"id\": \"11\", \"geo_ref_id\": \"880\", \"rec_type_conf_lvl_config_id\": \"4\"},{\"id\": \"12\", \"geo_ref_id\": \"880\", \"rec_type_conf_lvl_config_id\": \"0\"},{\"id\": \"13\", \"geo_ref_id\": \"880\", \"rec_type_conf_lvl_config_id\": \"3\"},{\"id\": \"14\", \"geo_ref_id\": \"1021\", \"rec_type_conf_lvl_config_id\": \"0\"},{\"id\": \"15\", \"geo_ref_id\": \"984\", \"rec_type_conf_lvl_config_id\": \"5\"},{\"id\": \"16\", \"geo_ref_id\": \"1074\", \"rec_type_conf_lvl_config_id\": \"1\"},{\"id\": \"17\", \"geo_ref_id\": \"952\", \"rec_type_conf_lvl_config_id\": \"4\"},{\"id\": \"18\", \"geo_ref_id\": \"1073\", \"rec_type_conf_lvl_config_id\": \"1\"},{\"id\": \"19\", \"geo_ref_id\": \"892\", \"rec_type_conf_lvl_config_id\": \"3\"},{\"id\": \"20\", \"geo_ref_id\": \"952\", \"rec_type_conf_lvl_config_id\": \"1\"},{\"id\": \"21\", \"geo_ref_id\": \"1021\", \"rec_type_conf_lvl_config_id\": \"3\"},{\"id\": \"22\", \"geo_ref_id\": \"880\", \"rec_type_conf_lvl_config_id\": \"1\"},{\"id\": \"23\", \"geo_ref_id\": \"1074\", \"rec_type_conf_lvl_config_id\": \"0\"},{\"id\": \"24\", \"geo_ref_id\": \"880\", \"rec_type_conf_lvl_config_id\": \"2\"},{\"id\": \"25\", \"geo_ref_id\": \"1021\", \"rec_type_conf_lvl_config_id\": \"5\"},{\"id\": \"26\", \"geo_ref_id\": \"DFLT\", \"rec_type_conf_lvl_config_id\": \"2\"},{\"id\": \"27\", \"geo_ref_id\": \"1074\", \"rec_type_conf_lvl_config_id\": \"2\"},{\"id\": \"28\", \"geo_ref_id\": \"1073\", \"rec_type_conf_lvl_config_id\": \"2\"},{\"id\": \"29\", \"geo_ref_id\": \"1074\", \"rec_type_conf_lvl_config_id\": \"5\"},{\"id\": \"30\", \"geo_ref_id\": \"952\", \"rec_type_conf_lvl_config_id\": \"0\"},{\"id\": \"31\", \"geo_ref_id\": \"1021\", \"rec_type_conf_lvl_config_id\": \"2\"},{\"id\": \"32\", \"geo_ref_id\": \"DFLT\", \"rec_type_conf_lvl_config_id\": \"3\"},{\"id\": \"33\", \"geo_ref_id\": \"880\", \"rec_type_conf_lvl_config_id\": \"5\"},{\"id\": \"34\", \"geo_ref_id\": \"892\", \"rec_type_conf_lvl_config_id\": \"0\"},{\"id\": \"35\", \"geo_ref_id\": \"952\", \"rec_type_conf_lvl_config_id\": \"5\"},{\"id\": \"36\", \"geo_ref_id\": \"1074\", \"rec_type_conf_lvl_config_id\": \"4\"},{\"id\": \"37\", \"geo_ref_id\": \"DFLT\", \"rec_type_conf_lvl_config_id\": \"4\"},{\"id\": \"38\", \"geo_ref_id\": \"DFLT\", \"rec_type_conf_lvl_config_id\": \"0\"},{\"id\": \"39\", \"geo_ref_id\": \"1073\", \"rec_type_conf_lvl_config_id\": \"4\"},{\"id\": \"40\", \"geo_ref_id\": \"892\", \"rec_type_conf_lvl_config_id\": \"2\"},{\"id\": \"41\", \"geo_ref_id\": \"984\", \"rec_type_conf_lvl_config_id\": \"4\"},{\"id\": \"42\", \"geo_ref_id\": \"892\", \"rec_type_conf_lvl_config_id\": \"4\"},{\"id\": \"43\", \"geo_ref_id\": \"1073\", \"rec_type_conf_lvl_config_id\": \"0\"},{\"id\": \"44\", \"geo_ref_id\": \"1073\", \"rec_type_conf_lvl_config_id\": \"3\"},{\"id\": \"45\", \"geo_ref_id\": \"DFLT\", \"rec_type_conf_lvl_config_id\": \"5\"},{\"id\": \"46\", \"geo_ref_id\": \"952\", \"rec_type_conf_lvl_config_id\": \"3\"},{\"id\": \"47\", \"geo_ref_id\": \"984\", \"rec_type_conf_lvl_config_id\": \"1\"}]}";

    private static final String tabWithMoreThanOneJoin = "{\"two_joins_table\": [\n" +
            "                         {\"list_table\": \"0\", \"entry_table\": \"2\", \"name\": \"foo\"},\n" +
            "                         {\"list_table\": \"1\", \"entry_table\": \"3\", \"name\": \"bar\"}]}";

    private static final String xml_1_str = CommonUtils.convertXmlToJson(TestXMLData.scn_1);
    private static final Context context_1 = ContextFactory.INSTANCE.obtainContext(xml_1_str);

    private static final String xml_2_str = CommonUtils.convertXmlToJson(TestXMLData.scn_2);
    private static final Context context_2 = ContextFactory.INSTANCE.obtainContext(xml_2_str);

    private static final String xml_3_str = CommonUtils.convertXmlToJson(TestXMLData.scn_3);
    private static final Context context_3 = ContextFactory.INSTANCE.obtainContext(xml_3_str);

    /*SQL QUERIES*/
    //SQLDBOprRule Test Method Variables
    private static final String dataFetchSqlQuery = "SELECT UVRFD_ORG_REC_ID,NG_INP_MSG_ID,INFO_SRC_CD,LANG_CD,STDN_APPL_CD,DUNS_NBR_REGN_STAT_CD,BUS_NME,STR_NBR,STR_NBR_EXTN,STR_NBR_TO,STR_NBR_TO_EXTN,STR_DIRN_PFX_CD,STR_DSGN_NME,STR_DIRN_SFX_CD,STR_TYP_TXT,SCDY_STR_NME,BLDG_NME,LOCN_PHRS,ESTE_NME,STR_NME,PRIM_ADR_POST_TOWN_NME,MLG_ADR_POST_TOWN_NME,PRIM_ADR_TERR_ABRV,PRIM_ADR_TERR_ID,MLG_POST_RTE_TYP_CD,MLG_ADR_TERR_ABRV,MLG_ADR_TERR_ID,PRIM_ADR_POST_CD,PRIM_ADR_POST_CD_EXTN_CD,MLG_ADR_POST_CD,MLG_ADR_POST_CD_EXTN_CD,MLG_POST_BOX_NBR,MLG_POST_BOX_TYP_CD,MLG_POST_RTE,MLG_POST_RTE_BOX_NBR,TLCM_DOM_AREA_CD_NBR,PHON_NBR,SNR_PRIN_NME,STD_INDS_CODE_1,INDS_CODE_TYP_CD_1,STD_INDS_CODE_2,INDS_CODE_TYP_CD_2,STD_INDS_CODE_3,INDS_CODE_TYP_CD_3,STD_INDS_CODE_4,INDS_CODE_TYP_CD_4,TRDG_NME_1,TRDG_NME_2,TRDG_NME_3,TRDG_NME_4,TRDG_NME_5,LAL_INDC,MTCH_CNFL_INDC,GEO_REF_ID,SRVR_RISK_INDC,MTCH_REF_INDC,CLSTR_REC_PRCS_STAT_CD,ROW_CRET_DT,LST_UPD_DT,UPD_USR_ID,STRT_YR,TRAN_NG_STAT_IND,IE_REC_ID,PRIM_ADR_CNTY_NME,REGN_NBR,REGN_NBR_CD,SLS_AMT,EMPL_HRE_QTY,BUS_URL,CTAC_EML_ADR,UPRN,ACTV_TEXT,SRC_REC_KEY,TRD_REF_NBR,TRD_ACCT_NBR,SNR_PRIN_SHRTN_JB_TTL_NME,STRT_YR_RELB_CD,BUS_NME_LANG_CD,TRD_STYL_NME_LANG_CD,MLG_ADR_LANG_CD,SUBJ_ID,SRC_PFL_GRP_ID,SRC_PFL_GRP_INSTN_ID,SRC_GRP_BTCH_ID,SRC_REC_ID,PRIM_ADR_LANG_CD,ORG_INDS_1_CODE_RELB_CD,ADR_LANG_CD,ORG_INDS_2_CODE_RELB_CD,ORG_INDS_3_CODE_RELB_CD,ORG_INDS_4_CODE_RELB_CD,ORG_INDS_CODE_RELB_CD,DRVD_INDS_CODE,DRVD_INDS_CODE_TYP_CD,ASGD_INDS_CODE_RELB_CD,TLCM_2_DOM_AREA_CD_NBR,PHON_2_NBR,TLCM_3_DOM_AREA_CD_NBR,PHON_3_NBR,TLCM_4_DOM_AREA_CD_NBR,PHON_4_NBR,TLCM_5_DOM_AREA_CD_NBR,PHON_5_NBR,STD_INDS_CODE_5,INDS_CODE_TYP_CD_5,ORG_INDS_5_CODE_RELB_CD,STD_INDS_CODE_6,INDS_CODE_TYP_CD_6,ORG_INDS_6_CODE_RELB_CD,ACTV_2_DESC,ACTV_3_DESC,ACTV_4_DESC,ACTV_5_DESC,REGN_2_NBR,REGN_2_NBR_TYP_CD,REGN_3_NBR,REGN_3_NBR_TYP_CD,REGN_4_NBR,REGN_4_NBR_TYP_CD,REGN_5_NBR,REGN_5_NBR_TYP_CD,REC_STAT_CD,DUNS_NBR " +
            "FROM UVRFD_ORG WHERE DUNS_NBR = ?";
    private static final String teDupSqlQuery = "SELECT COUNT(1) as found " +
            "FROM SUBJ_EXTR " +
            "WHERE DUNS_NBR = ?";
    private static final String dupClstrMbrSqlQuery = "SELECT COUNT(1) " +
            "AS found " +
            "FROM (SELECT UVRFD_ORG_REC_ID " +
            "FROM CAND_CLSTR " +
            "WHERE  UVRFD_ORG_REC_ID IN ( " +
            "SELECT UVRFD_ORG_REC_ID " +
            "FROM UVRFD_ORG " +
            "WHERE DUNS_NBR = ? " +
            "AND DUNS_NBR_REGN_STAT_CD = 15200) "+
            "AND CLSTR_REC_PRCS_STAT_CD = 60 " +
            "GROUP BY UVRFD_ORG_REC_ID " +
            "HAVING COUNT(*) > 1 " +
            "ORDER BY UVRFD_ORG_REC_ID)";

    private static final RulesEngine testRulesEngine = new AbstractRulesEngine("com/exsoinn/ie/rule/", true) {
        @Override
        public void execute() {

        }
    };


    /**
     * These have to execute before any rules can be executed
     */
    static {
        boolean error = true;
        Exception thrown = null;
        try {
            /*
             * Add expected configurations to the rules engine
             */
            final Map<String, String> configMap = new HashMap<>();
            configMap.put(RuleConstants.CONFIG_PROP_THREAD_POOL_SIZE, "1");
            ((AbstractRulesEngine) testRulesEngine).addConfigurationProperties(configMap);

            /*
             * Load the JavaScript files required by the various tests
             */
            importData();
            error = false;
        } catch (FileNotFoundException e) {
            thrown = e;
        } catch (ScriptException e) {
            thrown = e;
        } catch (RuleException e) {
            thrown = e;
        }

        /*
         * Doesn't make sense to continue with tests if there were errors found while importing any of the files
         * above. Remember that among others, above "AbstractRule.initializeDataSourceJoinInformationForQueryableRuleConfigComponents()"
         * strictly enforces adherence to rule API config system, which will give error and rest of JS scripts will not load.
         */
        if (error) {
            CommonUtils.errorToFileAndStdErr(
                    "Aborting tests because of errors printed below, JVM will now shutdown.", _LOGGER);
            CommonUtils.errorToFileAndStdErr(CommonUtils.extractStackTrace(thrown).toString(), _LOGGER);
            System.exit(1);
        }
    }


    /**
     * Helper method to load all the rule test dependencies.
     *
     * @throws FileNotFoundException
     * @throws ScriptException
     * @throws RuleException
     */
    private static void importData() throws FileNotFoundException, ScriptException, RuleException {
        testRulesEngine.importJavaScriptFileIntoEngine("testCommon.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testJoinAutoDetection.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testQueryableConfigRule.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testMatrixDecision.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testRuleBasedMatrixDecision.js");
        //testRulesEngine.importJavaScriptFileIntoEngine("testIeBusinessNameValidationUS.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testLookupDataRule.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testContentValidationRule.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testContentValidationUsingLookupList.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testQueryableConfigRule.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testLoadCommonData.js");
        /*testRulesEngine.importJavaScriptFileIntoEngine("testDnbIndsCdCrsWlkUniverse.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDnbIndsCdCrsWlkListConfig.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDnbIndsCdCrsWlkEntryMapper.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDnbIndsCdDescUniverse.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDnbIndsCdDescListConfig.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDnbIndsCdDescEntryMapper.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDnbIndsCdDelLstFltrUniverse.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDnbIndsCdDelLstFltrListConfig.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDnbIndsCdDelLstFltrEntryMapper.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDnbIndsCdRuleSet.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDunsNbrPoolUniverse.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDunsNbrPoolListConfig.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDunsNbrPoolEntryMapper.js");
        testRulesEngine.importJavaScriptFileIntoEngine("testDunsNbrPoolRuleSet.js");*/
        testRulesEngine.importJavaScriptFileIntoEngine("testJavaScriptFunctionRule.js");
        AbstractRule.initializeDataSourceJoinInformationForQueryableRuleConfigComponents();
        AbstractRule.createElementFinderRule("testFindGeoRefId", "VER_ORG.GEO_REF_ID||NULL||NULL");
        AbstractRule.createElementFinderRule("testFindInfoSrcCd", "VER_ORG.INFO_SRC_CD||NULL||NULL");
        AbstractRule.createElementFinderRule("testFinderDunsFromMatchRsltAry", "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||NULL||DUNS_NBR");
        AbstractRule.elementFinderRuleBuilder("testFinderCandSetAry", "left_operand_table||id=90||left_operand")
                .selectionCriteriaSource("commonLeftOperand").convertResultToDelimitedString(false).build();
    }


    @Test
    public void passingAndFailingRegExRule() throws ScriptException, RuleException {
        RuleExecutionResult rr =
                regExRule("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.POST_CODE", null);
        assertTrue(rr.evaluateResult());

        rr = regExRule("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.TLCM_INFO.TLCM_ENTR.TEL_NBR", null);
        assertTrue(!rr.evaluateResult());

        // Try same tests but this time we explicitly pass the String to act on
        rr = regExRule(null, "12345");
        assertTrue(rr.evaluateResult());

        rr = regExRule(null, "123456");
        assertTrue(!rr.evaluateResult());


    }

    private RuleExecutionResult regExRule(String pSearchPath, String pTargetStr) throws RuleException {
        if (CommonUtils.stringIsBlank(pTargetStr)) {
            SearchPath sp = SearchPath.valueOf(pSearchPath);
            Rule r = com.exsoinn.ie.rule.AbstractRule.createRegExRule("^\\d{5}$");
            return r.apply(context, sp, null, null, null);
        } else {
            Rule r = com.exsoinn.ie.rule.AbstractRule.createRegExRuleWithFixedTargetString("^\\d{5}$", pTargetStr);
            return r.apply(context, null, null, null, null);
        }
    }

    @Test
    public void regExRuleJavaScript() throws ScriptException {
        testRulesEngine.executeFunctionByName("testRegExRule.js", "executeRule", jsonStr);
    }


    private void regExRuleJavaScriptUsingContext(Context pContext) throws ScriptException {
        testRulesEngine.executeFunctionByName("testRegExRuleUsingContext.js", "executeRule", pContext);
    }

    @Test
    public void equalsRelOpRule() throws RuleException {
        RuleExecutionResult rr = relOpRule("==", IGNORE_INT, 13182);
        assertTrue((Boolean) rr.evaluateResult());

        rr = relOpRule("==", 19555, 19555);
        assertTrue((Boolean) rr.evaluateResult());
    }


    @Test
    public void notEqualsRelOpRule() throws RuleException {
        RuleExecutionResult rr = relOpRule("!=", IGNORE_INT, 19999);
        assertTrue((Boolean) rr.evaluateResult());

        rr = relOpRule("!=", 19999, 2000);
        assertTrue((Boolean) rr.evaluateResult());
    }

    @Test
    public void greaterThanRelOpRule() throws RuleException {
        RuleExecutionResult rr = relOpRule(">", IGNORE_INT, 1);
        assertTrue((Boolean) rr.evaluateResult());

        rr = relOpRule(">", 2, 1);
        assertTrue((Boolean) rr.evaluateResult());
    }

    @Test
    public void lessThanRelOpRule() throws RuleException {
        RuleExecutionResult rr = relOpRule("<", IGNORE_INT, 19556);
        assertTrue((Boolean) rr.evaluateResult());


        rr = relOpRule("<", 19555, 19556);
        assertTrue((Boolean) rr.evaluateResult());
    }

    @Test
    public void greaterThanOrEqualRelOpRule() throws RuleException {
        RuleExecutionResult rr = relOpRule(">=", IGNORE_INT, 1);
        assertTrue((Boolean) rr.evaluateResult());

        rr = relOpRule(">=", 100, 100);
        assertTrue((Boolean) rr.evaluateResult());
    }

    @Test
    public void lessThanOrEqualRelOpRule() throws RuleException {
        RuleExecutionResult rr = relOpRule("<=", IGNORE_INT, 19556);
        assertTrue((Boolean) rr.evaluateResult());

        rr = relOpRule("<=", 19556, 19555);
        assertTrue(!(Boolean) rr.evaluateResult());
    }


    private RuleExecutionResult relOpRule(String pOp, int pLeftOperand, int pRightHandOperand)
            throws RuleException {
        // Sets up search path of the node
        SearchPath sp = SearchPath.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR");
        Filter f = Filter.valueOf("STD_STRG_VW.STDN_APPL_CD=13135");
        TargetElements t = TargetElements.valueOf("DNB_DATA_PRVD_CD");


        // Set the value we want to compare to (the right hand side operand)
        Map<String, String> extraParams = new java.util.HashMap(1);
        extraParams.put(RuleConstants.ARG_RIGHT_OPERAND, Integer.toString(pRightHandOperand));

        if (pLeftOperand == IGNORE_INT) {
            Rule r = AbstractRule.createRelOpRule(pOp);
            return r.apply(context, sp, f, t, extraParams);
        } else {
            Rule r = AbstractRule.createRelOpRuleWithFixedOperands(pOp, pLeftOperand, pRightHandOperand);
            return r.apply((Context) null, null, null, null, null);
        }
    }

    @Test
    public void relOpRuleJavaScript() throws ScriptException {
        testRulesEngine.executeFunctionByName("testRelOpRule.js", "executeRule", jsonStr);
    }


    private void relOpRuleJavaScriptUsingContext(Context pContext) throws ScriptException {
        testRulesEngine.executeFunctionByName("testRelOpRuleUsingContext.js", "executeRule", pContext);
    }


    /**
     * Test cases for {@link StringHandlerRule} component
     * @throws RuleException
     */
    @Test
    public void testStringHandlerRule() throws RuleException {
        /**
         * Test passing the left op as an input arg
         */
        Map<String, String> params = new HashMap<>();
        params.put(RuleConstants.ARG_LEFT_OPERAND, "abcdefgh");
        params.put(RuleConstants.ARG_RIGHT_OPERAND, "ABCDEFGH");
        Rule r = AbstractRule.createStringHandlerRule("EQUALS", "UPPER_CASE");
        RuleExecutionResult res = r.apply((Context) null, null, null, null, params);
        assertTrue(res.evaluateResult());
        assertEquals("ABCDEFGH", res.evaluateResultAsString());

        /**
         * Test obtaining the left op from a passed in {@link Context} object
         */
        String testInputData = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"life time wireless\"}        \n" +
                "}";
        params.remove(RuleConstants.ARG_LEFT_OPERAND);
        params.put(RuleConstants.ARG_RIGHT_OPERAND, "LIFE TIME WIRELESS");
        Context c = ContextFactory.INSTANCE.obtainContext(testInputData);
        res = r.apply(c, SearchPath.valueOf("VER_ORG.BIZ_NAME"), null, null, params);
        assertTrue(res.evaluateResult());
        assertEquals("LIFE TIME WIRELESS", res.evaluateResultAsString());
    }


    /**
     * Test cases for {@link RegExRule} component.
     *
     * @throws RuleException
     */
    @Test
    public void testRegExRule() throws RuleException {
        /**
         * Test passing the left op as an input arg
         */
        Map<String, String> params = new HashMap<>();
        params.put(RuleConstants.ARG_REGEX_PATTERN, "^[a-zA-Z]+$");
        params.put(RuleConstants.ARG_REGEX_TARGET_VAL, "abcd1234");
        Rule r = AbstractRule.createGenericRegExRule();
        RuleExecutionResult res = r.apply((Context) null, null, null, null, params);
        assertTrue(!res.evaluateResult());
        params.put(RuleConstants.ARG_REGEX_TARGET_VAL, "abcd");
        res = r.apply((Context) null, null, null, null, params);
        assertTrue(res.evaluateResult());

        /**
         * Test obtaining the left op from a passed in {@link Context} object
         */
        String testInputData = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"life time wireless\"}        \n" +
                "}";
        params.put(RuleConstants.ARG_REGEX_PATTERN, "^life\\s");
        params.remove(RuleConstants.ARG_REGEX_TARGET_VAL);
        Context c = ContextFactory.INSTANCE.obtainContext(testInputData);
        res = r.apply(c, SearchPath.valueOf("VER_ORG.BIZ_NAME"), null, null, params);
        assertTrue(res.evaluateResult());
    }

    private void checkAgainstCollectionRuleJavaScript() throws ScriptException {
        testRulesEngine.executeFunctionByName("testCheckAgainstCollectionRule.js", "executeRule", jsonStr);
    }

    @Test
    public void passingAndFailingCheckAgainstCollectionRule() throws ScriptException, RuleException {
        List<String> l = Arrays.stream("8,9,10".split(",")).collect(Collectors.toList());
        RuleExecutionResult rr = checkAgainstCollectionRule(l, null, null);
        assertTrue(rr.evaluateResult());

        l = Arrays.stream("7,8,9".split(",")).collect(Collectors.toList());
        rr = checkAgainstCollectionRule(l, null, null);
        assertTrue(!rr.evaluateResult());


        // This time we tell it the value to check
        rr = checkAgainstCollectionRule(l, null, "9");
        assertTrue((Boolean) rr.evaluateResult());

        l = Arrays.stream("7,8,9".split(",")).collect(Collectors.toList());
        rr = checkAgainstCollectionRule(l, null, "10");
        assertTrue(!(Boolean) rr.evaluateResult());
    }


    @Test
    public void passingAndFailingCheckAgainstCollectionRuleUsingNotInOperation() throws ScriptException, RuleException {
        List<String> l = Arrays.stream("8,9,10".split(",")).collect(Collectors.toList());
        RuleExecutionResult rr = checkAgainstCollectionRule(l, "NOT_IN", null);
        assertTrue(!(Boolean) rr.evaluateResult());

        l = Arrays.stream("7,8,9".split(",")).collect(Collectors.toList());
        rr = checkAgainstCollectionRule(l, "NOT_IN", null);
        assertTrue((Boolean) rr.evaluateResult());

        // Try same but pass in val to check explicitly
        rr = checkAgainstCollectionRule(l, "NOT_IN", "9");
        assertTrue(!(Boolean) rr.evaluateResult());

        rr = checkAgainstCollectionRule(l, "NOT_IN", "10");
        assertTrue((Boolean) rr.evaluateResult());
    }

    public RuleExecutionResult checkAgainstCollectionRule(List<String> pList, String pOp, String pValToCheck)
            throws ScriptException, RuleException {
        /*
         * Set up the search criteria to identify top MSR candidate, and return the confidence
         * code value
         */
        SearchPath sp = SearchPath.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF");

        // Defines filter that says to get top MSR candidate (rec code = 15201, and cand rank = 1)
        Filter f = Filter.valueOf("CAND_RNK=1;REGN_STAT_CD=15201");
        TargetElements t = TargetElements.valueOf("CFDC_LVL_VAL");

        Rule r;
        if (CommonUtils.stringIsBlank(pOp) && CommonUtils.stringIsBlank(pValToCheck)) {
            r = AbstractRule.createCheckAgainstCollectionRule(pList);
        } else {
            r = AbstractRule.createCheckAgainstCollectionRuleForOperationWithFixedValueToCheck(pList, pOp, pValToCheck);
        }
        return r.apply(context, sp, f, t, null);
    }


    private void checkAgainstCollectionRuleJavaScriptUsingContext(Context pContext) throws ScriptException {
        testRulesEngine.executeFunctionByName("testCheckAgainstCollectionRuleUsingContext.js", "executeRule", pContext);
    }


    @Test
    public void noDuplicateObjectsCreatedInMultiThreadedEnvironment() {

    }

    @Test
    public void measureMultiThreadExecutionTime()
            throws InterruptedException {
        int concurrency = 10;
        final CountDownLatch ready = new CountDownLatch(concurrency);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(concurrency);
        int numOfOps = 3;

        Executor executor = Executors.newCachedThreadPool();
        final AtomicInteger cnt = new AtomicInteger(0);
        for (int i = 0; i < concurrency; i++) {
            executor.execute(() -> {
                try {
                    /*
                     * Acts as gate that opens only once all threads are ready. Each thread will report itself
                     * as "ready" by decreasing the "ready" CountDownLatch, which is initialized to the total
                     * number of threads expected. Then after reporting itself as "ready", each thread
                     * will await on the "start" CountDownLatch, which acts as the gate that opens at the same time,
                     * so that al runners get a fair start (no one cheats by getting a head start). See EJ, Bloch18,
                     * page 327.
                     */
                    ready.countDown();
                    start.await();
                    /*
                     * Provide 3 different kinds of tests to run, equally divided among
                     * "concurrency" threads
                     */
                    if (cnt.intValue() % numOfOps == 1) {
                        checkAgainstCollectionRuleJavaScript();
                    } else if (cnt.intValue() % numOfOps == 2) {
                        regExRuleJavaScript();
                    } else {
                        relOpRuleJavaScript();
                    }
                    cnt.incrementAndGet();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ScriptException e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });
        }

        // This will block until all threads are ready.
        ready.await();
        long startNs = System.nanoTime();
        start.countDown();
        done.await();
        System.out.println("Run time in nano seconds is " + (System.nanoTime() - startNs));
    }


    /**
     * This test attempts to simulate how long it will take to process X number of records, by using a fixed
     * number Y of threads inside a thread pool. It will execute the same test <code>numOfExecs</code> number of times,
     * calculating average execution time at the end.
     *
     * @throws InterruptedException
     */
    @Test
    public void measureTimeToHandleRecordsUsingFixedNumberOfThreads() throws InterruptedException {
        int numOfExecs = 10;
        boolean useContext = true;
        List<Long> times = new ArrayList<>(numOfExecs);
        for (int i = 0; i <= numOfExecs; i++) {
            times.add(measureTimeToHandleRecordsUsingFixedNumberOfThreads(useContext, 10));
        }

        String ctxText = useContext ? " using context" : "";
        double totalNs = times.stream().mapToLong(d -> d).average().getAsDouble();
        double totalSecs = totalNs / 1000000000D;
        System.out.println("Total time (ns)" + ctxText + " is " + totalNs);
        System.out.println("Total time (secs)" + ctxText + " is " + totalSecs);
    }


    private long measureTimeToHandleRecordsUsingFixedNumberOfThreads(boolean pUseContext, int pNumOfRecs)
            throws InterruptedException {
        int numOfRecs = pNumOfRecs;
        int maxNumberOfThreads = 100;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(numOfRecs);
        int numOfOps = 3;

        Executor executor = Executors.newFixedThreadPool(maxNumberOfThreads);
        final AtomicInteger cnt = new AtomicInteger(0);

        /*
         * Allocate all threads in our fixed size thread pool. The ready to run tests
         * will wait via the count down latch. Then further below we count down 1 on
         * start latch, which signals the threads to start, allocating threads as they become available
         * to handle the remaining records, yet never exceeding maxNumberOfThreads.
         */
        for (int i = 0; i < numOfRecs; i++) {
            executor.execute(() -> {
                try {
                    start.await();
                    /*
                     * Provide 3 different kinds of tests to run, equally divided among
                     * "concurrency" threads
                     */
                    Context c = ContextFactory.INSTANCE.obtainContext(jsonStr);
                    if (cnt.intValue() % numOfOps == 1) {
                        if (pUseContext) {
                            checkAgainstCollectionRuleJavaScriptUsingContext(c);
                        } else {
                            checkAgainstCollectionRuleJavaScript();
                        }
                    } else if (cnt.intValue() % numOfOps == 2) {
                        if (pUseContext) {
                            regExRuleJavaScriptUsingContext(c);
                        } else {
                            regExRuleJavaScript();
                        }
                    } else {
                        if (pUseContext) {
                            relOpRuleJavaScriptUsingContext(c);
                        } else {
                            relOpRuleJavaScript();
                        }
                    }
                    cnt.incrementAndGet();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ScriptException e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });
        }

        long startNs = System.nanoTime();
        start.countDown();
        done.await();
        long totalNs = System.nanoTime() - startNs;
        System.out.println("Processed " + numOfRecs + " in " + (System.nanoTime() - startNs) + " nano seconds.");
        return totalNs;
    }

    @Test
    public void storeAndLookupRuleByName() throws RuleException {
        final String ruleName = "rule1";
        Rule r = AbstractRule.createQueryableDataRule(jsonStr2, ruleName);
        Rule found = AbstractRule.lookupRuleByName(r.name());
        assertTrue(null != found && found.name().equals(ruleName));
    }

    @Test(expected = DuplicateCacheEntryException.class)
    public void duplicateRuleNameNotAllowed() throws RuleException {
        final String ruleName1 = "rule1";
        AbstractRule.createQueryableDataRule(jsonStr2, ruleName1);
        AbstractRule.createQueryableDataRule(jsonStr2, ruleName1);
    }


    /**
     * Some rule types require a name to be giving at the time the rule type is requested. This
     * test case checks for that.
     *
     * @throws RuleException
     */
    @Test(expected = RuleException.class)
    public void throwsExceptionWhenRequiredRuleNameIsMissing() throws RuleException {
        final String ruleName1 = "";
        AbstractRule.createQueryableDataRule(jsonStr2, ruleName1);
    }


    /**
     * Test QueryableDataRule filter values that are dynamically populated using the underlying input record data, by using
     * placeholders denoted by angle brackets.
     *
     * @throws RuleException
     */
    @Test
    public void searchUsingDynamicallyPopulatedFilterClause() throws RuleException {
        final String ruleName = "queryableDataRule";
        AbstractRule.createQueryableDataRule(jsonStr3, ruleName);
        Rule r = AbstractRule.lookupRuleByName(ruleName);

        Map<String, String> params = new java.util.HashMap(3);
        params.put(RuleConstants.QUERY_SEARCH_PATH, "geo_to_conf_lvl_config_table");
        params.put(RuleConstants.QUERY_RETURN_ELEMENTS, "rec_type_conf_lvl_config_id");
        params.put(RuleConstants.QUERY_FILTER, "geo_ref_id=<GEO_REF_ID>");

        // Get the geo ref ID of this record to plug into query above
        SearchPath sp = SearchPath.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW");
        TargetElements ta = com.exsoinn.util.epf.TargetElements.valueOf("GEO_REF_ID");

        SearchResultRuleExecutionResult res = (SearchResultRuleExecutionResult) r.apply(context, sp, null, ta, params);
        List<Context> l = res.getSearchResult().get("geo_to_conf_lvl_config_table").asArray();
        assertTrue(l.size() == 6);
        assertTrue("5".equals(l.get(0).memberValue("rec_type_conf_lvl_config_id").stringRepresentation()));

    }

    /**
     * Note: The rule components referenced below were created by loading "testQueryableConfigRule.js"
     * in static block towards top of this class.
     *
     * @throws RuleException
     */
    @Test
    public void queryableRuleConfig() throws RuleException, InterruptedException {
        String[] expectedRes1 = {"true", "false", "cc true, mgp false"};
        queryableRuleConfigHelper(context, expectedRes1);


        /*
         * Test default configured rule set (I.e. context3 has a dummy, non-existent geo ref ID which will not be matched
         * against any in the configured rule set, and it will pickup the DEFAULT configuration)
         */
        String[] expectedRes2 = {"false", "true", "cc false, mgp true"};
        queryableRuleConfigHelper(context3, expectedRes2);


        /*
         * Test use-case where success output field value is overridden
         */
        RuleExecutionResult res = AbstractRule.executeConfigurableRule("ccEvalRuleOutFldOverride", context);
        assertEquals("1", res.evaluateResultAsString());


        /*
         * Test that filter created for the configurable rule set grabs the
         * correct rows.
         */
        QueryableRuleConfigurationData r =
                (QueryableRuleConfigurationData) AbstractRule.getRuleByNameCache().get("ruleSetFilterTest");
        Filter f = r.buildFilterToQueryRuleConfigData(context);
        Context ruleCtx = r.assembleRuleConfigurationData(f, null);
        List<Context> rows = ruleCtx.memberValue(ruleCtx.startSearchPath().toString()).asArray();
        assertTrue(rows.size() == 1);
        assertTrue(rows.get(0).memberValue("mgp_eval_rule").stringRepresentation().equals("true"));
    }


    /**
     * Tests a left_operand which gets its value from
     * the output of another rule.
     */
    @Test
    public void rulePrefixInRuleSetLeftOperand() throws RuleException {
        String testInputData = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"LIFE TIME WIRELESS\"}        \n" +
                "}";
        String testRuleSet = "{\"test_rule_set\": \n[" +
                "  {\"left_operand\":\"RULE:testFindGeoRefId\",\n" +
                "   \"operator\": \"==\",\n" +
                "   \"right_operand\":\"1073\",\n" +
                "   \"success_output\":\"passed\",\n" +
                "   \"failure_output\": \"failed\"}\n" +
                "]}";
        final String ruleName = "testRulePrefixLeftOp";
        Context c = ContextFactory.INSTANCE.obtainContext(testInputData);
        AbstractRule.ruleSetBuilder(testRuleSet, ruleName, "allMustPass").build();
        RuleExecutionResult res = AbstractRule.executeConfigurableRule(ruleName, c);
        assertTrue(res.evaluateResult());
    }

    /**
     * Tests a rule set that relies out output of another rule to
     * fill in the filter values for the rule set filter.
     */
    @Test
    public void rulePrefixInRuleSetFilter() throws RuleException {
        String testInputData = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"892\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"LIFE TIME WIRELESS\"}        \n" +
                "}";
        String testRuleSet = "{  \n" +
                "   \"test_rule_set\":[  \n" +
                "      {  \n" +
                "         \"geo_ref_id\":\"892\",\n" +
                "         \"left_operand\":\"NO-EVAL:892\",\n" +
                "         \"operator\":\"==\",\n" +
                "         \"right_operand\":\"1073\",\n" +
                "         \"success_output\":\"passed\",\n" +
                "         \"failure_output\": \"failed\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"geo_ref_id\":\"1073\",\n" +
                "         \"left_operand\":\"NO-EVAL:1073\",\n" +
                "         \"operator\":\"==\",\n" +
                "         \"right_operand\":\"1073\",\n" +
                "         \"success_output\":\"passed\",\n" +
                "         \"failure_output\": \"failed\"\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        final String ruleName = "testRulePrefixInFilter";
        Map<String, String> ruleFilter = new HashMap<>(1);
        ruleFilter.put("geo_ref_id", "RULE:testFindGeoRefId");

        Context c = ContextFactory.INSTANCE.obtainContext(testInputData);
        AbstractRule.ruleSetBuilder(testRuleSet, ruleName, "allMustPass").ruleSetFilter(ruleFilter).build();
        RuleExecutionResult res = AbstractRule.executeConfigurableRule(ruleName, c);
        assertTrue(!res.evaluateResult());
    }


    /**
     * Tests a rule set that relies out output of another rule to
     * dynamically get the success/failure outputs
     */
    @Test
    public void rulePrefixInRuleSetOutputFields() throws RuleException {
        String testInputData = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"892\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"LIFE TIME WIRELESS\"}        \n" +
                "}";
        String testRuleSet1 = "{  \n" +
                "   \"test_rule_set\":[  \n" +
                "      {  \n" +
                "         \"geo_ref_id\":\".*\",\n" +
                "         \"left_operand\":\"NO-EVAL:1\",\n" +
                "         \"operator\":\"==\",\n" +
                "         \"right_operand\":\"1\",\n" +
                "         \"success_output\":\"RULE:testFindGeoRefId\",\n" +
                "         \"failure_output\": \"RULE:testFindInfoSrcCd\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"geo_ref_id\":\".*\",\n" +
                "         \"left_operand\":\"NO-EVAL:1\",\n" +
                "         \"operator\":\"==\",\n" +
                "         \"right_operand\":\"2\",\n" +
                "         \"success_output\":\"RULE:testFindGeoRefId\",\n" +
                "         \"failure_output\": \"RULE:testFindInfoSrcCd\"\n" +
                "      }\n" +
                "   ]\n" +
                "}";

        final String ruleName1 = "testRulePrefixInSuccessFld";
        Context c = ContextFactory.INSTANCE.obtainContext(testInputData);
        AbstractRule.ruleSetBuilder(testRuleSet1, ruleName1, "anyCanPass").build();
        RuleExecutionResult res = AbstractRule.executeConfigurableRule(ruleName1, c);
        assertTrue(res.evaluateResult());
        assertEquals(res.getOutput(), "892");


        final String ruleName2 = "testRulePrefixInFailureFld";
        AbstractRule.ruleSetBuilder(testRuleSet1, ruleName2, "allMustPass").build();
        res = AbstractRule.executeConfigurableRule(ruleName2, c);
        assertTrue(!res.evaluateResult());
        assertEquals(res.getOutput(), "12345");
    }


    private void queryableRuleConfigHelper(Context pCtx, String[] pExpectedResAry) throws RuleException {
        String ruleNameCcEval = "ccEvalRule";
        String ruleNameMgpEval = "mgpEvalRule";

        RuleExecutionResult res = AbstractRule.executeConfigurableRule(ruleNameCcEval, pCtx);
        System.out.println("JMQ: CC Eval Rule: " + res.toString());
        assertTrue(pExpectedResAry[0].equals(res.evaluateResultAsString()));

        /*
         * When the rule set eval option is "ANY can pass", the result will contain each individual rule op's results,
         * even the ones that didn't pass. But regardless, *at least one of them* must be true. The below checks among
         * the return rule op results to find the success one, if any were expected, or if none were expected.
         */
        List<RuleExecutionResult> ruleOpResList = ((ExecutorRuleExecutionResult) res).getExecutedRuleResults();
        if ("true".equals(pExpectedResAry[0])) {
            assertTrue(ruleOpResList.stream().filter(e -> e.evaluateResult()).findFirst().isPresent());
        } else {
            assertTrue(!ruleOpResList.stream().filter(e -> e.evaluateResult()).findFirst().isPresent());
        }

        res = AbstractRule.executeConfigurableRule(ruleNameMgpEval, pCtx);
        System.out.println("JMQ: MGP Eval Rule: " + res.toString());
        assertTrue(pExpectedResAry[1].equals(res.evaluateResultAsString()));

        /*
         * Now test a matrix that uses rules created above to compute a decision
         */
        String matrixRuleName = "ccMgpEvalMatrix";
        res = AbstractRule.evaluateRuleBasedMatrix(matrixRuleName, pCtx);
        System.out.println("JMQ: MGP/CC Eval Matrix Rule: " + res.toString());
        assertTrue(pExpectedResAry[2].equals(res.evaluateResultAsString()));
    }


    /**
     * The data used for this test is defined in "testLookupDataRule.js"
     *
     * @throws RuleException
     * @throws FileNotFoundException
     * @throws ScriptException
     */
    @Test
    public void lookupData() throws RuleException, FileNotFoundException, ScriptException {
        String rec1 = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"LIFE TIME WIRELESS\"}        \n" +
                "}";

        String rec2 = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"TIME\"}        \n" +
                "}";

        String rec3 = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"LIFE TIME WIRELESS INC\"}        \n" +
                "}";

        String rec4 = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"6\"}        \n" +
                "}";

        String rec5 = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"WORD1 WORD2\"}        \n" +
                "}";

        String lookupRuleName = "lookupEvalRule";
        RuleExecutionResult res = AbstractRule.executeConfigurableRule(lookupRuleName, context);
        assertTrue(res.evaluateResult());

        /*
         * Test exact match that passes
         */
        lookupRuleName = "luExactMatch";
        res = AbstractRule.executeConfigurableRule(lookupRuleName, ContextFactory.INSTANCE.obtainContext(rec1));
        assertTrue(res.evaluateResult());

        /*
         * Test exact match that fails
         */
        res = AbstractRule.executeConfigurableRule(lookupRuleName, ContextFactory.INSTANCE.obtainContext(rec2));
        assertTrue(!res.evaluateResult());

        /*
         * Test partial match that passes
         */
        lookupRuleName = "luPartialMatch";
        res = AbstractRule.executeConfigurableRule(lookupRuleName, ContextFactory.INSTANCE.obtainContext(rec2));
        assertTrue(res.evaluateResult());
        assertEquals("LIFE__SPACE__TIME__SPACE__WIRELESS", res.evaluateResultAsContext().memberValue(RuleConstants.LOOKUP_OUTPUT_VALUE).stringRepresentation());

        /*
         * Test partial match that fails
         */
        res = AbstractRule.executeConfigurableRule(lookupRuleName, ContextFactory.INSTANCE.obtainContext(rec3));
        assertTrue(!res.evaluateResult());

        /**
         * Test when an alternate output field has been configured. The below when executed
         * will do a lookup against a list name which has configured output field name "id", which is 26. See
         * "testLookupDataRule.js" for details, search for "luAltOutputFld".
         */
        lookupRuleName = "luAltOutputFld";
        res = AbstractRule.executeConfigurableRule(lookupRuleName, ContextFactory.INSTANCE.obtainContext(rec2));
        assertEquals("26",
                res.evaluateResultAsContext().memberValue(RuleConstants.LOOKUP_OUTPUT_VALUE).stringRepresentation());
        assertTrue(res.evaluateResult());

        /**
         * Test when an alternate search field has been configured. The below when executed
         * will do a lookup against a list name which has configured output field name "other_field", which is 6. See
         * "testLookupDataRule.js" for details, search for "luAltOutputFld".
         */
        lookupRuleName = "luAltSearchFld";
        res = AbstractRule.executeConfigurableRule(lookupRuleName, ContextFactory.INSTANCE.obtainContext(rec4));
        assertEquals("6",
                EscapeUtil.unescapeSpecialCharacters(res.evaluateResultAsContext()
                        .memberValue(RuleConstants.LOOKUP_OUTPUT_VALUE).stringRepresentation()));
        assertTrue(res.evaluateResult());

        /**
         * Test combination of searchField and matchOption combined. Again see "testLookupDataRule.js"
         * which is the file that defines rule with name "luAltSearchFld"
         */
        res = AbstractRule.executeConfigurableRule(lookupRuleName, ContextFactory.obtainContext(rec5));
        assertEquals("WORD1",
                EscapeUtil.unescapeSpecialCharacters(res.evaluateResultAsContext()
                        .memberValue(RuleConstants.LOOKUP_OUTPUT_VALUE).stringRepresentation()));
        assertTrue(res.evaluateResult());
        assertEquals("passed", res.getOutput());


        /**
         * For lookuop rules, test that the output given is the one of the target rule executed (from the lookup result object),
         * and not the one from configured success/failure output fields. See "testLookupDataRule.js" and search for
         * "luAltSearchFldUsingTargetRuleOutput" to see where/how this is configured, which is namely by passing option
         * "useTargetRuleOutput==true". This directly also tests a config option of {@link QueryableRuleConfigurationData}
         */
        lookupRuleName = "luAltSearchFldUsingTargetRuleOutput";
        res = AbstractRule.executeConfigurableRule(lookupRuleName, ContextFactory.INSTANCE.obtainContext(rec5));
        assertEquals("WORD1",
                EscapeUtil.unescapeSpecialCharacters(res.evaluateResultAsContext()
                        .memberValue(RuleConstants.LOOKUP_OUTPUT_VALUE).stringRepresentation()));
        assertTrue(res.evaluateResult());
        assertEquals("WORD1", res.getOutput());

    }

    /**
     * The resources for this test can be found in file "testContentValidationRule.js"
     * @throws RuleException
     * @throws FileNotFoundException
     * @throws ScriptException
     */
    @Test
    public void testContentValidation() throws RuleException, FileNotFoundException, ScriptException {
        _LOGGER.info("Test Content Validation...");

        RuleExecutionResult res = AbstractRule.executeConfigurableRule("cntnVldnRules", context);
        assertEquals("9000", res.evaluateResultAsString());

    }


  /**
   * Ignore until I can set up a DB to connect to.
   * @throws RuleException
   * @throws DataSourceException
   * @throws InvocationTargetException
   * @throws NoSuchMethodException
   * @throws IllegalAccessException
   * @throws IOException
   */
    @Test
    @Ignore
    public void testDatabaseOperationRule() throws RuleException, DataSourceException,
            InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        final String DUNS_NUM_SC = "DUNS_NBR||NULL||NULL";
        // First register the database source that will handle our queries
        AbstractRule.registerDatabaseSource("ds_iedwmowner_test", "config/database/test_configuration_iedwmowner.properties");

        AbstractRule.databaseOperationBuilder("testFetchData", "ds_iedwmowner_test", dataFetchSqlQuery)
                .parameterSelectionCriteria(DUNS_NUM_SC)
                .build();
        AbstractRule.iteratorRuleBuilder("dbResultIterator", "RULE:testFinderDunsFromMatchRsltAry", "testFetchData", "CAND_SET").build();
        RuleExecutionResult dataFetchIteratorRuleResult = AbstractRule.executeIterableRule("dbResultIterator", itrRlSqlDunsFrmMtchCtx, null);


        Map<String, String> augmentFields = new HashMap<>();
        Map<String, String> ruleFldChooser = new HashMap<>();
        ruleFldChooser.put("topElementsChecker", "FOUND");
        AbstractRule.databaseOperationBuilder("topElementsChecker", "ds_iedwmowner_test", teDupSqlQuery)
                .parameterSelectionCriteria(DUNS_NUM_SC)
                .build();
        augmentFields.put("existsInTopElements", "RULE:topElementsChecker");
        AbstractRule.contextAugmenterBuilder("topElementsFlagAugmenter", augmentFields)
                .ruleOutputChosenField(ruleFldChooser)
                .build();
        AbstractRule.iteratorRuleBuilder("topElementsCheckerIterator", "RULE:testFinderCandSetAry",
                "topElementsFlagAugmenter", "CAND_SET").build();
        RuleExecutionResult teCheckerIteratorRes = AbstractRule.executeIterableRule("topElementsCheckerIterator",
                dataFetchIteratorRuleResult.evaluateResultAsContext(), null);
        Context teCheckOutCtx = teCheckerIteratorRes.evaluateResultAsContext();
        assertTrue(teCheckOutCtx.containsElement("CAND_SET"));
        assertTrue(teCheckOutCtx.memberValue("CAND_SET").asArray().size() == 7);



        AbstractRule.databaseOperationBuilder("dupClusterMemberChecker", "ds_iedwmowner_test", dupClstrMbrSqlQuery)
                .parameterSelectionCriteria(DUNS_NUM_SC)
                .build();
        Map<String,String> augmentFields2 = new HashMap<>();
        augmentFields2.put("duplicateClusterMember","RULE:dupClusterMemberChecker");
        Map<String, String> ruleFldChooser2 = new HashMap<>();
        ruleFldChooser2.put("dupClusterMemberChecker", "FOUND");
        AbstractRule.contextAugmenterBuilder("dupClusterMemberFlagAugmenter", augmentFields2)
                .ruleOutputChosenField(ruleFldChooser2)
                .build();
        AbstractRule.iteratorRuleBuilder("dupClusterMemberCheckerIterator",
                "RULE:testFinderCandSetAry", "dupClusterMemberFlagAugmenter", "CAND_SET").build();
        RuleExecutionResult dupIteratorRes = AbstractRule.executeIterableRule("dupClusterMemberCheckerIterator",
                teCheckerIteratorRes.evaluateResultAsContext(), null);
        Context dupCheckOutCtx = dupIteratorRes.evaluateResultAsContext();
        assertTrue(dupCheckOutCtx.containsElement("CAND_SET"));
        assertTrue(dupCheckOutCtx.memberValue("CAND_SET").asArray().size() == 7);
    }

    /**
     *
     * @throws RuleException
     * @throws FileNotFoundException
     * @throws ScriptException
     */
    @Test
    @Ignore
    public void testIndustryCodeValidation() throws RuleException, FileNotFoundException, ScriptException {
        _LOGGER.info("Test Industry Code Lookup Validation");

        RuleExecutionResult res = AbstractRule.executeConfigurableRule("indsCdDesc1ValCheck8Digit", context_1);
        assertTrue(res.evaluateResult());

        TextOutputRuleExecutionResult res2 = (TextOutputRuleExecutionResult) AbstractRule.executeConfigurableRule("dnbIndsCd1CrsWlk", context_2);
        assertEquals("59630000", res2.evaluateResultAsContext().memberValue(RuleConstants.LOOKUP_OUTPUT_VALUE).stringRepresentation());

        RuleExecutionResult res3 = AbstractRule.executeConfigurableRule("indsCd1DelLstFltrVal8Digits", context_1);
        assertTrue(res3.evaluateResult());
        assertEquals("59630000",res2.evaluateResultAsContext().memberValue(RuleConstants.LOOKUP_OUTPUT_VALUE).stringRepresentation());

        res3 = AbstractRule.executeConfigurableRule("indsCd1DelLstFltrVal8Digits", context_1);
        assertTrue(res3.evaluateResult());
    }


    /**
     * TODO: This does not belong in common area, move to non-app specific module ASAP!!!
     * @throws RuleException
     * @throws ScriptException
     */
    @Test
    @Ignore
    public void dunsNumberPoolValidation() throws RuleException, ScriptException {
        _LOGGER.info("DUNS Number Pool Validation");

        TextOutputRuleExecutionResult res1 = (TextOutputRuleExecutionResult) AbstractRule.executeConfigurableRule("dunsNbrPoolUsRlCnfg", context_1);
        assertEquals("234534567", res1.evaluateResultAsContext().memberValue(RuleConstants.LOOKUP_OUTPUT_VALUE).stringRepresentation());

        TextOutputRuleExecutionResult res3 = (TextOutputRuleExecutionResult) AbstractRule.executeConfigurableRule("dunsNbrPoolCaRlCnfg", context_3);
        assertEquals("564534231", res3.evaluateResultAsContext().memberValue(RuleConstants.LOOKUP_OUTPUT_VALUE).stringRepresentation());
        assertEquals("234534567",res1.evaluateResultAsContext().memberValue(RuleConstants.LOOKUP_OUTPUT_VALUE).stringRepresentation());

        res3 = (TextOutputRuleExecutionResult) AbstractRule.executeConfigurableRule("dunsNbrPoolCaRlCnfg", context_3);
        assertEquals("564534231",res3.evaluateResultAsContext().memberValue(RuleConstants.LOOKUP_OUTPUT_VALUE).stringRepresentation());
    }

    @Test
    public void businessNameValidationUS() throws RuleException, ScriptException {
        //testRulesEngine.executeFunctionByName("testIeBusinessNameValidationUS.js", "businessNameValidation", context);
    }

    // TODO: Add test cases for: config data rule that overrides the output value


    /**
     * The data used by this test is loaded from "testJoinAutoDetection.js"
     * @throws FileNotFoundException
     * @throws ScriptException
     * @throws RuleException
     */
    @Test
    public void testAutoDetectionJoinInformation()
            throws FileNotFoundException, ScriptException, RuleException, InterruptedException {
        // Below commented out because then it messes up other tests
        // TODO: Figure out a way to incorporate this test later
        //AbstractRule.createQueryableDataRule(tabWithMoreThanOneJoin, "twoJoinsTable");

        // Verify the joins were auto-detected correctly
        QueryableRuleConfigurationData mainTable = (QueryableRuleConfigurationData) AbstractRule.getRuleByNameCache().get("mainTable");
        QueryableDataRule middleTable = (QueryableDataRule) AbstractRule.getRuleByNameCache().get("middleTable");
        QueryableDataRule lastTable = (QueryableDataRule) AbstractRule.getRuleByNameCache().get("lastTable");
        QueryableDataRule mapperTable = (QueryableDataRule) AbstractRule.getRuleByNameCache().get("mapperTable");
        QueryableDataRule listTable = (QueryableDataRule) AbstractRule.getRuleByNameCache().get("listTable");
        QueryableDataRule entryTable = (QueryableDataRule) AbstractRule.getRuleByNameCache().get("entryTable");
        QueryableRuleConfigurationData listRuleTable =
                (QueryableRuleConfigurationData) AbstractRule.getRuleByNameCache().get("listRuleTable");

        verifyJoinHelper(mainTable, 1, middleTable, "middle_table", QueryableDataRule.ID_FLD_NAME);
        verifyJoinHelper(middleTable, 1, lastTable, "last_table", QueryableDataRule.ID_FLD_NAME);
        verifyJoinHelper(lastTable, 0, null, null, null);


        /**
         * Mapper join checks.
         * - Check that mapper data source was correctly identified
         * - Ensure that joins were automatically created to the mapper table from the tables that the mapper refers to
         */
        assertTrue(mapperTable.dataSourceIsMapper());
        verifyJoinHelper(listTable, 1, mapperTable, QueryableDataRule.ID_FLD_NAME, "list_table");
        verifyJoinHelper(entryTable, 1, mapperTable, QueryableDataRule.ID_FLD_NAME, "entry_table");
        verifyJoinHelper(mapperTable, 2, listTable, "list_table", QueryableDataRule.ID_FLD_NAME);
        verifyJoinHelper(mapperTable, 2, entryTable, "entry_table", QueryableDataRule.ID_FLD_NAME);


        // Verify assemble is correct when it involves data mapper
        verifyJoinHelper(listRuleTable, 1, listTable, "list_table", QueryableDataRule.ID_FLD_NAME);
        Filter f = listRuleTable.buildFilterToQueryRuleConfigData(context);
        Context ruleConfigCtx = listRuleTable.assembleRuleConfigurationData(f, null);

        // Expecting only 6 rows in the assembled context
        List<Context> rows = ruleConfigCtx.memberValue(ruleConfigCtx.startSearchPath().toString()).asArray();
        assertTrue(rows.size() == 6);

        // Only these list entries expected
        final List<String> expectedListEnts = new ArrayList<>(6);
        expectedListEnts.add("Google");
        expectedListEnts.add("Facebook");
        expectedListEnts.add("John Deere");
        expectedListEnts.add("VZW");
        expectedListEnts.add("Oracle");
        expectedListEnts.add("Sun Microsystems");
        Optional<Context> failed = rows.stream().filter(c -> expectedListEnts.contains(c.memberValue("name"))).findAny();
        assertTrue(!failed.isPresent());
    }

    private boolean verifyJoinHelper(QueryableDataRule pDataSource,
                                     int pExpectedJoinNum,
                                     QueryableDataRule pExpectedTargetDataSource,
                                     String pExpectedSrcJoinFldName,
                                     String pExpectedTargetJoinFldName) {
        boolean sizeCheckPassed = pDataSource.getDataSourceJoinInformation().size() == pExpectedJoinNum;
        if (pDataSource.getDataSourceJoinInformation().size() == 0 && sizeCheckPassed) {
            return true;
        }
        DataSourceJoinInformation dsji = pDataSource.getDataSourceJoinInformation().get(0);

        String srcJoinFldName = dsji.getSourceJoinFieldName();
        String targetJoinFldName = dsji.getTargetJoinFieldName();
        return sizeCheckPassed
                && dsji.getTargetDataSource() == pExpectedTargetDataSource
                && pExpectedSrcJoinFldName.equals(srcJoinFldName)
                && pExpectedTargetJoinFldName.equals(targetJoinFldName);

    }


    /**
     * Note that this test will depend on rules/data sources loaded in "testMatrixDecision.js", which is also
     * used by other test methods of this class.
     * TODO: Add some asserts on the rules result object!!!
     * @throws FileNotFoundException
     * @throws ScriptException
     */
    @Test
    public void ruleFlow() throws FileNotFoundException, ScriptException, RuleException {
        testRulesEngine.importJavaScriptFileIntoEngine("testRuleFlow.js");
        RuleExecutionResult res = AbstractRule.executeRuleFlow("testRuleFlow", context);
        System.out.println(res.toString());

        res = AbstractRule.executeRuleFlow("testRuleFlow", context2);
        System.out.println(res.toString());
    }

    /**
     * The data for this test is loaded via file "testMatrixDecision.js"
     */
    @Test
    public void matrixDecision() throws RuleException {
        String decision = AbstractRule.matrixDecision(
                "testMatrixDecision",
                MatrixDecisionInput.valueOf("top_ssr_conf_lvl=H;top_msr_conf_lvl=L"),
                "decision");
        assertTrue("FB".equals(decision));
        decision = AbstractRule.matrixDecision(
                "testMatrixDecision",
                MatrixDecisionInput.valueOf("top_ssr_conf_lvl=L;top_msr_conf_lvl=M"),
                "decision");
        assertTrue("DM".equals(decision));

        /*
         * Test that default decision works when the passed in decision inputs do
         * not match any of the decision matrix rows
         */
        decision = AbstractRule.matrixDecision(
                "testMatrixDecision",
                MatrixDecisionInput.valueOf("top_ssr_conf_lvl=FOO;top_msr_conf_lvl=BAR"),
                "decision");
        assertTrue("DEFAULT_DECISION".equals(decision));

        /*
         * We give bad inputs on purpose so that it throws exception
         */
        try {
            AbstractRule.matrixDecision(
                    "testMatrixDecisionWithoutDefault",
                    MatrixDecisionInput.valueOf("top_ssr_conf_lvl=FOO;top_msr_conf_lvl=BAR"),
                    "decision");
        } catch (Exception e) {
            assertTrue(e.toString().indexOf("Matrix decision gave invalid output") >= 0);
        }
    }


    /**
     * Tests a matrix which inputs are the outputs of other rules. The rule configuration definitions for this test
     * can be found in file "rule-api\src\test\resources\com.exsoinn\ie\rule\testRuleBasedMatrixDecision.js
     */
    @Test
    public void ruleBasedMatrixDecision() throws RuleException {
        RuleExecutionResult res = AbstractRule.evaluateRuleBasedMatrix("dlDecisionMatrix", context);
        assertTrue("DM".equals(res.evaluateResultAsString()));
        System.out.println("JMQ: res is " + res);

        res = AbstractRule.evaluateRuleBasedMatrix("dlDecisionMatrix", context2);
        assertTrue("FB".equals(res.evaluateResultAsString()));
        System.out.println("JMQ: res is " + res);
    }


    @Test(expected = InvalidQueryableDataFormatException.class)
    public void validateOnlyOneTopLevelElementPresent() throws RuleException {
        String json = "{\"two_joins_table\": [\n" +
                "                         {\"list_table\": \"0\", \"entry_table\": \"2\", \"name\": \"foo\"},\n" +
                "                         {\"list_table\": \"1\", \"entry_table\": \"3\", \"name\": \"bar\"}]," +
                "       \"other_elem\": \"1\"" +
                "      }";
        Rule r = AbstractRule.createQueryableDataRule(json, "testDataSource1");
        ((QueryableDataRule) r).validateOnlyOneTopElement();
    }


    @Test(expected = TopLevelElementValueIsNotArrayException.class)
    public void validateTopLevelElementValueIsArray() throws RuleException {
        String json = "{\"other_elem\": \"1\"}";
        Rule r = AbstractRule.createQueryableDataRule(json, "testDataSource2");
        ((QueryableDataRule) r).validateOnlyOneTopElement();
    }


    @Test(expected = RowIsNotComplexException.class)
    public void validateRowIsComplexObject() throws RuleException {
        String json = "{  \n" +
                "   \"pattern_list\":[  \n" +
                "      {  \n" +
                "         \"id\":\"0\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZF\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"1\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZ8\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"2\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZ9\"\n" +
                "      },\n" +
                "      \"1\"\n" +
                "   ]\n" +
                "}";
        Rule r = AbstractRule.createQueryableDataRule(json, "testDataSource3");
        QueryableDataRule qdr = (QueryableDataRule) r;
        for (Context row : qdr.context().memberValue(qdr.context().startSearchPath().toString()).asArray()) {
            ((QueryableDataRule) r).validateRowIsComplexObject(row);
        }
    }

    @Test
    public void validateAllFieldsArePrimitiveOrArrayPassing() throws RuleException {
        String json = "{  \n" +
                "   \"pattern_list\":[  \n" +
                "      {  \n" +
                "         \"id\":\"0\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZF\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"1\",\n" +
                "         \"right_operand\": [1,2,3,4,5,6]\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"2\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZ9\"\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        validateFieldTypes(json);
    }


    @Test(expected = NotAllRowFieldValuesArePrimitiveOrArray.class)
    public void validateAllFieldsArePrimitiveOrArrayFailing() throws RuleException {
        String json = "{  \n" +
                "   \"pattern_list\":[  \n" +
                "      {  \n" +
                "         \"id\":\"0\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZF\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"1\",\n" +
                "         \"right_operand\": {\"BFFAAZZBFZ8\": \"1\"}\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"2\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZ9\"\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        validateFieldTypes(json);
    }

    private void validateFieldTypes(String pJson) throws RuleException {
        try {
            Rule r = AbstractRule.createQueryableDataRule(pJson, "testDataSource4");
            QueryableDataRule qdr = (QueryableDataRule) r;
            for (Context row : qdr.context().memberValue(qdr.context().startSearchPath().toString()).asArray()) {
                ((QueryableDataRule) r).validateAllRowFieldValuesArePrimitiveOrArray(row);
            }
        } finally {
            /*
             * We're already done testing. Remove this badly formatted rule context, otherwise any rule that invokes
             * the validate method(s) will get this exception
             */
            AbstractRule.getRuleByNameCache().remove("testDataSource4");
        }
    }


    @Test
    public void validateAllRowFieldsAreTheSamePassing() throws RuleException {
        String json = "{  \n" +
                "   \"pattern_list\":[  \n" +
                "      {  \n" +
                "         \"id\":\"0\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZF\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"1\",\n" +
                "         \"right_operand\": \"BFFAAZZBF22\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"2\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZ9\"\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        validateAllRowFieldsAreTheSame(json);
    }

    @Test(expected = NotAllRowsHaveSameFieldsException.class)
    public void validateAllRowFieldsAreTheSameFailing() throws RuleException {
        String json = "{  \n" +
                "   \"pattern_list\":[  \n" +
                "      {  \n" +
                "         \"id\":\"0\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZF\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"1\",\n" +
                "         \"right_operand\": \"BFFAAZZBF22\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"2\",\n" +
                "         \"foo\":\"BFFAAZZBFZ9\"\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        validateAllRowFieldsAreTheSame(json);
    }


    private void validateAllRowFieldsAreTheSame(String pJson) throws RuleException {
        try {
            Rule r = AbstractRule.createQueryableDataRule(pJson, "testDataSource");
            QueryableDataRule qdr = (QueryableDataRule) r;
            qdr.validateRows();
        } finally {
            /*
             * We're already done testing. Remove this badly formatted rule context, otherwise any rule that invokes
             * the validate method(s) will get this exception
             */
            AbstractRule.getRuleByNameCache().remove("testDataSource");
        }
    }



    @Test(expected = DuplicateRowIdException.class)
    public void validateDuplicateIdCheck() throws RuleException {
        String json = "{  \n" +
                "   \"pattern_list\":[  \n" +
                "      {  \n" +
                "         \"id\":\"0\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZF\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"1\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZ8\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"2\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZ9\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"1\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZ9\"\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        Rule r = AbstractRule.createQueryableDataRule(json, "testDataSource5");
        QueryableDataRule qdr = (QueryableDataRule) r;
        Map<String, Boolean> seenIds = new HashMap<>();
        int curRowCnt = 1;
        for (Context row : qdr.context().memberValue(qdr.context().startSearchPath().toString()).asArray()) {
            ((QueryableDataRule) r).validateIdFieldRequirements(row, curRowCnt, seenIds);
            curRowCnt++;
        }
    }


    @Test(expected = MissingIdFieldException.class)
    public void validateRowMissingIdFieldCheck() throws RuleException {
        String json = "{  \n" +
                "   \"pattern_list\":[  \n" +
                "      {  \n" +
                "         \"id\":\"0\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZF\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"1\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZ8\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"id\":\"2\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZ9\"\n" +
                "      },\n" +
                "      {  \n" +
                "         \"foo\":\"1\",\n" +
                "         \"right_operand\":\"BFFAAZZBFZ9\"\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        Rule r = AbstractRule.createQueryableDataRule(json, "testDataSource6");
        QueryableDataRule qdr = (QueryableDataRule) r;
        Map<String, Boolean> seenIds = new HashMap<>();
        int curRowCnt = 1;
        for (Context row : qdr.context().memberValue(qdr.context().startSearchPath().toString()).asArray()) {
            ((QueryableDataRule) r).validateIdFieldRequirements(row, curRowCnt, seenIds);
            curRowCnt++;
        }
    }


    /**
     * Checks that validation works where two dissimilar config data are using the same table
     * name.
     * @throws RuleException
     */
    @Test(expected = DuplicateDataSourceNameException.class)
    public void validateDuplicateSourceNameNotAllowed() throws RuleException {
        String json1 = "{\"dup_table_name\": [{\"field\": \"value\"}]}";
        String json2 = "{\"dup_table_name\": [{\"field1\": \"value1\"}, {\"field1\": \"value1\"}]}";
        String json3 = "{\"unique_table_name\": [{\"field\": \"value\"}]}";

        AbstractRule.createQueryableDataRule(json1, "dupName1");
        AbstractRule.createQueryableDataRule(json2, "dupName2");
        AbstractRule.createQueryableDataRule(json3, "uniqueName");

        AbstractRule.validateQueryableDataRuleComponents();
    }



    /**
     * The data used for this test case can be found in
     * file "rule-api\src\test\resources\com.exsoinn\ie\rule\testContentValidationUsingLookupList.js"
     */
    @Test
    public void contentValidationUsingLookup() throws RuleException {
        /**
         * Sample input records, contain the minimum required fields.
         */
        String rec1 = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"a bad word Company\"}        \n" +
                "}";
        String rec2 = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"54321\",\n" +
                "   \"BIZ_NAME\":\"a bad word Company\"}        \n" +
                "}";
        String rec3 = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"892\",\n" +
                "   \"INFO_SRC_CD\": \"44444\",\n" +
                "   \"BIZ_NAME\":\"a bad wordCompany\"}        \n" +
                "}";
        String rec4 = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"892\",\n" +
                "   \"INFO_SRC_CD\": \"55555\",\n" +
                "   \"BIZ_NAME\":\"a bad word Company\"}        \n" +
                "}";
        String rec5 = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"892\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"new bad word Company\"}        \n" +
                "}";
        String rec6 = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"892\",\n" +
                "   \"INFO_SRC_CD\": \"67890\",\n" +
                "   \"BIZ_NAME\":\"bad word phrase Company\"}        \n" +
                "}";
        String rec7a = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"99999\",\n" +
                "   \"BIZ_NAME\":\"naughty phrase Company\"}        \n" +
                "}";
        String rec7b = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"99999\",\n" +
                "   \"BIZ_NAME\":\"Company naughty phrase Inc\"}        \n" +
                "}";
        String rec8a = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"88888\",\n" +
                "   \"BIZ_NAME\":\"Company naughty phrase\"}        \n" +
                "}";
        String rec8b = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"88888\",\n" +
                "   \"BIZ_NAME\":\"Company naughty phrase Inc\"}        \n" +
                "}";
        String rec9a = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"984\",\n" +
                "   \"INFO_SRC_CD\": \"88888\",\n" +
                "   \"BIZ_NAME\":\"ACME Biz Delete Phrase I\"}        \n" +
                "}";
        String rec9b = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1067\",\n" +
                "   \"INFO_SRC_CD\": \"88888\",\n" +
                "   \"BIZ_NAME\":\"ACME Biz Delete Phrase I\"}        \n" +
                "}";
        String rec9c = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1067\",\n" +
                "   \"INFO_SRC_CD\": \"88888\",\n" +
                "   \"BIZ_NAME\":\"Biz Delete Phrase II ACME\"}        \n" +
                "}";
        String ruleNameDirty = "luContentVldRuleDirtyWord";
        String ruleNameDelete = "luContentVldRuleDeleteWord";

        /**
         * ---> Test 1: Info src code "12345" in US doing exact match on input "a bad word Company", will have no hits because
         *   list has term "a bad word", which is not exactly the same as "a bad word Company" and we're doing exact match
         *
         */
        lookupServiceTestHelper(false, ContextFactory.INSTANCE.obtainContext(rec1), ruleNameDirty, true);


        /**
         * ---> Test 2: Info src code "54321" in US doing partial (anywhere in string) match on input "a bad word Company", will have
         *   a hit because list has term "a bad word", which is a substring of "a bad word Company"
         */
        lookupServiceTestHelper(true, ContextFactory.INSTANCE.obtainContext(rec2), ruleNameDirty, true);



        /**
         * Test 3: Canada does partial *whole* word match on input "a bad wordCompany" (across all info sources), will *not* have
         *   a hit because list has term "a bad word", which while is a substring in the input business name,
         *   it is *not* a separate word in input business name "a bad wordCompany"
         *   (If it were a partial match, then there would have been hit)
         *   Can demo before and after here
         */
        lookupServiceTestHelper(false, ContextFactory.INSTANCE.obtainContext(rec3), ruleNameDirty, true);


        /**
         * Test 4: A different Canada source which provides business name "a bad word Company", will match
         *   because list has term "a bad word", which *is* a separate word in input business name "a bad word Company"
         */
        lookupServiceTestHelper(true, ContextFactory.INSTANCE.obtainContext(rec4), ruleNameDirty, true);


        /**
         * Test 5: Input has a business name not configured in dirty word list, it should pass validation, should fail
         *   after new bad word added
         */
        lookupServiceTestHelper(true, ContextFactory.INSTANCE.obtainContext(rec5), ruleNameDirty, true);

        /**
         * Test 6: Canada business name comes that contains a phrase configured in dirty word list, test
         *   that word boundary match works in this use case scenario
         */
        lookupServiceTestHelper(true, ContextFactory.INSTANCE.obtainContext(rec6), ruleNameDirty, true);


        /**
         * Test 7: Test startsWith pass and fail
         */
        // Pass
        lookupServiceTestHelper(true, ContextFactory.INSTANCE.obtainContext(rec7a), ruleNameDirty, true);


        // Fail
        lookupServiceTestHelper(false, ContextFactory.INSTANCE.obtainContext(rec7b), ruleNameDirty, true);


        /**
         * Test 8: Test endsWith pass and fail
         */
        // Pass
        lookupServiceTestHelper(true, ContextFactory.INSTANCE.obtainContext(rec8a), ruleNameDirty, true);

        // Fail
        lookupServiceTestHelper(false, ContextFactory.INSTANCE.obtainContext(rec8b), ruleNameDirty, true);


        /**
         * Test 9a: Test endsWith pass and fail on a "delete" list
         */
        // Pass - Mexico does endsWith match on this delete list, hence below will pass
        lookupServiceTestHelper(true, ContextFactory.INSTANCE.obtainContext(rec9a), ruleNameDelete, true);

        /*
         * Test 9b: Test startsWith fail on a "delete" list
         */
        lookupServiceTestHelper(false, ContextFactory.INSTANCE.obtainContext(rec9b), ruleNameDelete, true);

        /**
         * Non-Mexico markets configured to match list term at beginning of input, hence match found
         * Below should pass because it's a non-Mexico market and key word will match
         * at beginning of input biz name
         */
        lookupServiceTestHelper(true, ContextFactory.INSTANCE.obtainContext(rec9c), ruleNameDelete, true);
    }


    private void lookupServiceTestHelper(boolean pExpected, Context pCtx, String pRuleName, boolean pPrintInfo)
            throws RuleException {
        RuleExecutionResult res = AbstractRule.executeConfigurableRule(pRuleName, pCtx);
        if (pPrintInfo) {
            System.out.println(res.toString() + "Input Record:\n" + pCtx.stringRepresentation());
        }
        assertTrue(res.evaluateResult() == pExpected);

    }


    /**
     * Test Javascript function rule feature. This just provides basic sanity check.
     * The JavaScript definitions for test below can be found at
     * rule-api/src/test/resources/com/exsoinn/ie/rule/testJavaScriptFunctionRule.js.
     *
     * @throws RuleException
     */
    @Test
    public void testJavaScriptFunctionRule() throws RuleException {
        Rule r = AbstractRule.lookupRuleByName("testFunc1");
        RuleExecutionResult res = r.apply(context, null, null, null, null);
        assertEquals("Passed", res.evaluateResultAsString());
        res = r.apply(context2, null, null, null, null);
        assertEquals("Failed", res.evaluateResultAsString());
        System.out.println(res.toString());
    }

    @Test(expected = BadJavaScriptFunctionException.class)
    public void testNonJavaScriptFunctionRuleRejection() throws Throwable {
        try {
            testRulesEngine.importJavaScriptFileIntoEngine("testNonJavaScriptFunctionDisallowed.js");
        } catch (Throwable thrown) {
            if (thrown.getCause() instanceof BadJavaScriptFunctionException) {
                throw thrown.getCause();
            } else {
                /**
                 * Did not get what we were expecting, simply re-throw as-is, but this certainly
                 * means our test will fail.
                 */
                throw thrown;
            }
        }
    }


    /**
     * Checks that default left operand feature when SearchCriteria yields
     * no results is working fine.
     * The below assigns a default left operand value of 1073 for the bogus search path
     * which we know does not exist in input data. The "==" rel op rule will thus pass.
     */
    @Test
    public void defaultLeftOperand() throws RuleException {
        String testInputData = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"LIFE TIME WIRELESS\"}        \n" +
                "}";
        String testRuleSet = "{\"test_rule_set\": \n[" +
                "  {\"left_operand\":\"VER_ORG.BOGUS_FLD_NAME||NULL||NULL\",\n" +
                "   \"left_operand_default\":\"1073\",\n" +
                "   \"ignore_element_not_found_error\":\"true\",\n" +
                "   \"operator\": \"==\",\n" +
                "   \"right_operand\":\"1073\",\n" +
                "   \"success_output\":\"passed\",\n" +
                "   \"failure_output\": \"failed\"}\n" +
                "]}";
        final String ruleName = "testDefaultLeftOp";
        Context c = ContextFactory.INSTANCE.obtainContext(testInputData);
        AbstractRule.ruleSetBuilder(testRuleSet, ruleName, "allMustPass").build();
        RuleExecutionResult res = AbstractRule.executeConfigurableRule(ruleName, c);
        assertTrue(res.evaluateResult());
    }


    /**
     *
     * @throws RuleException
     */
    @Test(expected = IncompatibleSearchPathException.class)
    public void ignoreLeftOperandElementNotFoundError() throws Throwable {
        String testInputData = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"1073\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"LIFE TIME WIRELESS\"}        \n" +
                "}";
        String testRuleSet1 = "{\"test_rule_set\": \n[" +
                "  {\"left_operand\":\"VER_ORG.BOGUS_FLD_NAME||NULL||NULL\",\n" +
                "   \"left_operand_default\":\"10\",\n" +
                "   \"ignore_element_not_found_error\":\"true\",\n" +
                "   \"operator\": \"==\",\n" +
                "   \"right_operand\":\"10\",\n" +
                "   \"success_output\":\"passed\",\n" +
                "   \"failure_output\": \"failed\"}\n" +
                "]}";

        String testRuleSet2 = "{\"test_rule_set\": \n[" +
                "  {\"left_operand\":\"VER_ORG.BOGUS_FLD_NAME||NULL||NULL\",\n" +
                "   \"left_operand_default\":\"DEFAULT_LEFT_OP_VAL\",\n" +
                "   \"operator\": \"==\",\n" +
                "   \"right_operand\":\"1073\",\n" +
                "   \"success_output\":\"passed\",\n" +
                "   \"failure_output\": \"failed\"}\n" +
                "]}";
        final String ruleName1 = "testIgnoreNotFoundElem1";
        final String ruleName2 = "testIgnoreNotFoundElem2";
        Context c = ContextFactory.INSTANCE.obtainContext(testInputData);
        AbstractRule.ruleSetBuilder(testRuleSet1, ruleName1, "allMustPass").build();
        RuleExecutionResult res = AbstractRule.executeConfigurableRule(ruleName1, c);
        assertTrue(res.evaluateResult());

        try {
            c = ContextFactory.INSTANCE.obtainContext(testInputData);
            AbstractRule.ruleSetBuilder(testRuleSet2, ruleName2, "allMustPass").build();
            res = AbstractRule.executeConfigurableRule(ruleName2, c);
            assertTrue(res.evaluateResult());
        } catch (Exception e) {
            if (null != e.getCause() && e.getCause() instanceof IncompatibleSearchPathException) {
                throw e.getCause();
            }
        }
    }


    /**
     * Test of the {@link JoinOutputRule}.
     * @throws RuleException
     */
    @Test
    public void testRuleOutputJoiner() throws RuleException {
        String testInputData = "{  \n" +
                "   \"VER_ORG\":{  \n" +
                "      \"GEO_REF_ID\":\"1073\",\n" +
                "      \"INFO_SRC_CD\":\"12345\",\n" +
                "      \"BIZ_NAME\":\"LIFE TIME WIRELESS\",\n" +
                "      \"CANDIDATES\":[  \n" +
                "         {  \n" +
                "            \"INFO_SRC_CD\":1234,\n" +
                "            \"DUNS_NBR\":0\n" +
                "         },\n" +
                "         {  \n" +
                "            \"INFO_SRC_CD\":5678,\n" +
                "            \"DUNS_NBR\":1\n" +
                "         },\n" +
                "         {  \n" +
                "            \"INFO_SRC_CD\":9012,\n" +
                "            \"DUNS_NBR\":2\n" +
                "         }\n" +
                "      ]\n" +
                "   }\n" +
                "}";
        Context c = ContextFactory.INSTANCE.obtainContext(testInputData);
        AbstractRule.createElementFinderRule("testRule1", "VER_ORG.INFO_SRC_CD||NULL||NULL");
        AbstractRule.createElementFinderRule("testRule2", "VER_ORG.BIZ_NAME||NULL||NULL");
        List<String> ruleNames = new java.util.ArrayList(3);
        ruleNames.add("testRule1");
        ruleNames.add("testRule2");
        Rule r = AbstractRule.ruleOutputJoinerBuilder("testOutputJoiner1", ruleNames).delimiter('-').build();
        RuleExecutionResult res = r.apply(c, null, null, null, null);
        assertEquals("12345-LIFE TIME WIRELESS", res.evaluateResultAsString());

        /**
         * Concatenate output of two rules, where the 2nd rule gives a multi-value output. The final rule outputs
         * is one single flattened list.
         */
        AbstractRule.createElementFinderRule("testRule3", "VER_ORG.CANDIDATES||NULL||INFO_SRC_CD");
        ruleNames.add("testRule3");
        r = AbstractRule.ruleOutputJoinerBuilder("testOutputJoiner2", ruleNames).delimiter('-').build();
        res = r.apply(c, null, null, null, null);
        assertEquals("12345-LIFE TIME WIRELESS-1234-5678-9012", res.evaluateResultAsString());


        /**
         * Same as above, but the final rule outputs is not a flattened list. Instead, a rule that gives multi-value
         * output will cause generation of multiple output lists (separated by double-pipe), where each list concatenates output
         * of other rules with each individual value of the list(s) that gave multi-value outputs.
         */
        ruleNames.clear();
        ruleNames.add("testRule1");
        ruleNames.add("testRule3");
        r = AbstractRule.ruleOutputJoinerBuilder("testOutputJoiner3", ruleNames).delimiter('-')
                .flattenedMultiValueOutputs(false).build();
        res = r.apply(c, null, null, null, null);
        assertEquals("12345-1234||12345-5678||12345-9012", res.evaluateResultAsString());
        assertEquals("12345-1234||12345-5678||12345-9012", res.getOutput());
    }


    /**
     * To test that a rule set filter picks up default value when the specified {@link SelectionCriteria} is not
     * in the input document.
     */
    @Test
    public void testDefaultInRuleSetFilter() throws RuleException {
        String testInputData = "{\"VER_ORG\": \n" +
                "  {\"GEO_REF_ID\":\"892\",\n" +
                "   \"INFO_SRC_CD\": \"12345\",\n" +
                "   \"BIZ_NAME\":\"LIFE TIME WIRELESS\"}        \n" +
                "}";
        String testRuleSet = "{\"test_rule_set\": \n[" +
                "  {\"left_operand\":\"VER_ORG.BOGUS_FLD_NAME||NULL||NULL\",\n" +
                "   \"geo_ref_id\":\"892\"," +
                "   \"left_operand_default\":\"1073\",\n" +
                "   \"ignore_element_not_found_error\":\"true\",\n" +
                "   \"operator\": \"==\",\n" +
                "   \"right_operand\":\"1073\",\n" +
                "   \"success_output\":\"passed\",\n" +
                "   \"failure_output\": \"failed\"}\n" +
                "]}";
        Map<String, String> m = new HashMap<>();
        m.put("geo_ref_id", "VER_ORG||GEO_REF_ID=9999||NULL");
        m.put("geo_ref_id_default", "892");
        final String ruleName = "testDefaultRuleSetFilterVal";
        Context c = ContextFactory.INSTANCE.obtainContext(testInputData);
        AbstractRule.ruleSetBuilder(testRuleSet, ruleName, "allMustPass").ruleSetFilter(m).build();
        RuleExecutionResult res = AbstractRule.executeConfigurableRule(ruleName, c);
        assertTrue(res.evaluateResult());
    }


    @Test(expected = DataMapperJoinMixedWithAnotherViolationException.class)
    public void onlyOneJoinWhenDataMapperJoinExists() throws FileNotFoundException, ScriptException, RuleException {
        Map<String, Rule> tempMap = new HashMap<>();
        tempMap.putAll(AbstractRule.getRuleByNameCache());
        try {
            /**
             * Clear out other rule objects temporarily, will restore after this test is done. The reason for doing this
             * is that we're trying to catch an init time exception.
             */
            AbstractRule.getRuleByNameCache().clear();
            testRulesEngine.importJavaScriptFileIntoEngine("testOnlyOneJoinWhenDataMapperJoinExists.js");
            AbstractRule.initializeDataSourceJoinInformationForQueryableRuleConfigComponents();
        } finally {
            AbstractRule.getRuleByNameCache().putAll(tempMap);
        }

    }


    /**
     * Tests the component responsible for adding fields to a {@link Context} object
     */
    @Test
    public void testContextAugmenter() throws RuleException {
        Map<String, String> fldsToAdd = new HashMap<>();
        String fldName = "field1";
        fldsToAdd.put(fldName, "value1");
        Context ctx = ContextFactory.obtainContext("{}");
        Rule r = AbstractRule.contextAugmenterBuilder("testCtxAugmenter1", fldsToAdd)
                .build();
        RuleExecutionResult res = r.apply(ctx, null, null, null, null);
        assertTrue(res.evaluateResultAsContext().containsElement(fldName));

        /**
         * Test both using rule output to populate the value of a field, and using alter context for this
         * rule to operate on. The "simpleFinder" will operate on
         */
        Map<String, String> map = new HashMap<>();
        String altCtxKey = "altCtxKey";
        map.put(altCtxKey, "{\"key\" = \"val\"}");
        Rule simpleFinder = AbstractRule.createElementFinderRule("simpleFinder", "key||null||null");
        fldsToAdd.clear();
        fldsToAdd.put("foundKey", "RULE:simpleFinder");
        r = AbstractRule.contextAugmenterBuilder("testCtxAugmenter2", fldsToAdd)
                .alternateSourceContextMapKey(altCtxKey)
                .build();
        res = r.apply(ctx, null, null, null, map);
        assertTrue(res.evaluateResultAsContext().containsElement("foundKey"));
    }
}
