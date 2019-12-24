package com.exsoinn.ie.rule;


import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.ContextFactory;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by QuijadaJ on 10/16/2017.
 */
public class BRELTest {
    private final static String data = "{  \n" +
            "   \"outer-node\":{  \n" +
            "      \"child-node\":{  \n" +
            "         \"date\":\"10/17/2017\"\n" +
            "      }\n" +
            "   }\n" +
            "}";

    @Test
    public void greaterThanTest() throws RuleException {
        String rule1 = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"greater-than\":{  \n" +
                "         \"constant\":{  \n" +
                "            \"data-type\":\"date\",\n" +
                "            \"value\":\"5/31/1974\"\n" +
                "         },\n" +
                "         \"search-path\":{  \n" +
                "            \"data-type\":\"date\",\n" +
                "            \"value\":\"outer-node.child-node.date||null||null\"\n" +
                "         }\n" +
                "      }\n" +
                "   }\n" +
                "}";
        String rule2 = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"greater-than\":{  \n" +
                "         \"search-path\":{  \n" +
                "            \"data-type\":\"date\",\n" +
                "            \"value\":\"outer-node.child-node.date||null||null\"\n" +
                "         },\n" +
                "         \"constant\":{  \n" +
                "            \"data-type\":\"date\",\n" +
                "            \"value\":\"5/31/1974\"\n" +
                "         }\n" +
                "      }\n" +
                "   }\n" +
                "}";

        /**
         * This should evaluate to false (5/31/1974 is not greater then 10/17/2017)
         */
        ExpressedRule.Builder ruleBuilder = AbstractRule.expressedRuleBuilder("gtTest1", rule1);
        ExpressedRule r = ruleBuilder.build();
        Context ctx = ContextFactory.obtainContext(data);
        RuleExecutionResult res = r.apply(ctx, null, null, null, null);
        assertFalse(res.evaluateResult());

        /**
         * This should evaluate to true (10/17/2017 IS greater then 5/31/1974 )
         */
        ruleBuilder = AbstractRule.expressedRuleBuilder("gtTest2", rule2);
        r = ruleBuilder.build();
        ctx = ContextFactory.obtainContext(data);
        res = r.apply(ctx, null, null, null, null);
        assertTrue(res.evaluateResult());
    }

    @Test
    public void andTest() throws RuleException {
        String rule = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"and\":[  \n" +
                "         {  \n" +
                "            \"equals\":[  \n" +
                "               {  \n" +
                "                  \"constant\":{  \n" +
                "                     \"value\":\"hello\"\n" +
                "                  }\n" +
                "               },\n" +
                "               {  \n" +
                "                  \"constant\":{  \n" +
                "                     \"value\":\"hello\"\n" +
                "                  }\n" +
                "               }\n" +
                "            ]\n" +
                "         },\n" +
                "         {  \n" +
                "            \"equals\":[  \n" +
                "               {  \n" +
                "                  \"constant\":{  \n" +
                "                     \"value\":\"goodbye\"\n" +
                "                  }\n" +
                "               },\n" +
                "               {  \n" +
                "                  \"constant\":{  \n" +
                "                     \"value\":\"goodbye\"\n" +
                "                  }\n" +
                "               }\n" +
                "            ]\n" +
                "         }\n" +
                "      ]\n" +
                "   }\n" +
                "}";
        ExpressedRule.Builder ruleBuilder = AbstractRule.expressedRuleBuilder("andTest1", rule);
        ExpressedRule r = ruleBuilder.build();
        Context ctx = ContextFactory.obtainContext(data);
        RuleExecutionResult res = r.apply(ctx, null, null, null, null);
        assertTrue(res.evaluateResult());

    }

    @Test
    public void atLeastTest() throws RuleException {
        String rule = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"at-least\":{  \n" +
                "         \"number\":3,\n" +
                "         \"collection\":\"CAND_SET||null||null\",\n" +
                "         \"equals\":[  \n" +
                "            {  \n" +
                "               \"constant\":{  \n" +
                "                  \"value\":\"true\"\n" +
                "               }\n" +
                "            },\n" +
                "            {  \n" +
                "               \"search-path\":{  \n" +
                "                  \"value\":\"corroborationStatus||null||null\"\n" +
                "               }\n" +
                "            }\n" +
                "         ]\n" +
                "      }\n" +
                "   }\n" +
                "}";
        ExpressedRule.Builder ruleBuilder = AbstractRule.expressedRuleBuilder("atLeastTest", rule);
        ExpressedRule r = ruleBuilder.build();
        String fileData = loadFile("com/exsoinn/ie/rule/brel/testCollectionRecordLevel.json");
        Context ctx = ContextFactory.obtainContext(fileData);
        RuleExecutionResult res = r.apply(ctx, null, null, null, null);
        assertTrue(res.evaluateResult());
    }


    /**
     * Tests the {@link com.exsoinn.ie.rule.definition.TotalElem}.
     * @throws RuleException
     */
    @Test
    public void totalTest() throws RuleException {
        String rule = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"total\":{  \n" +
                "         \"collection\":\"CAND_SET||null||null\",\n" +
                "         \"equals\":[  \n" +
                "            {  \n" +
                "               \"constant\":{  \n" +
                "                  \"value\":\"1\"\n" +
                "               }\n" +
                "            },\n" +
                "            {  \n" +
                "               \"search-path\":{  \n" +
                "                  \"value\":\"elementConfiguration||name=ADDRESS_PHYS||permissible\"\n" +
                "               }\n" +
                "            }\n" +
                "         ]\n" +
                "      }\n" +
                "   }\n" +
                "}";

        /**
         * First test looks for something which will yield a 0 count.
         */
        ExpressedRule.Builder ruleBuilder = AbstractRule.expressedRuleBuilder("totalTest", rule);
        ExpressedRule r = ruleBuilder.build();
        String fileData = loadFile("com/exsoinn/ie/rule/brel/testCollectionRecordLevel.json");
        Context ctx = ContextFactory.obtainContext(fileData);
        RuleExecutionResult res = r.apply(ctx, null, null, null, null);
        assertTrue(res.evaluateResult());
        assertTrue(Integer.valueOf(0).intValue() == Integer.valueOf(res.getOutput()).intValue());


        /**
         * Second test looks for something which will yield a 3 count.
         */
        fileData = loadFile("com/exsoinn/ie/rule/brel/testCollectionChange.json");
        ctx = ContextFactory.obtainContext(fileData);
        res = r.apply(ctx, null, null, null, null);
        assertTrue(res.evaluateResult());
        assertTrue(Integer.valueOf(3).intValue() == Integer.valueOf(res.getOutput()).intValue());

        /**
         * Third test does equality evaluation and returns true or false if the "total"
         * operation count is same as the "constant"
         */
        rule = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"equals\":[  \n" +
                "         {  \n" +
                "            \"total\":{  \n" +
                "               \"collection\":\"CAND_SET||null||null\",\n" +
                "               \"equals\":[  \n" +
                "                  {  \n" +
                "                     \"constant\":{  \n" +
                "                        \"value\":\"1\"\n" +
                "                     }\n" +
                "                  },\n" +
                "                  {  \n" +
                "                     \"search-path\":{  \n" +
                "                        \"value\":\"elementConfiguration||name=ADDRESS_PHYS||permissible\"\n" +
                "                     }\n" +
                "                  }\n" +
                "               ]\n" +
                "            }\n" +
                "         },\n" +
                "         {  \n" +
                "            \"constant\":{  \n" +
                "               \"data-type\":\"int\",\n" +
                "               \"value\":\"3\"\n" +
                "            }\n" +
                "         }\n" +
                "      ]\n" +
                "   }\n" +
                "}";
        ExpressedRule.Builder ruleBuilder2 = AbstractRule.expressedRuleBuilder("totalTest3", rule);
        ExpressedRule r2 = ruleBuilder2.build();
        fileData = loadFile("com/exsoinn/ie/rule/brel/testCollectionChange.json");
        ctx = ContextFactory.obtainContext(fileData);
        res = r2.apply(ctx, null, null, null, null);
        assertTrue(res.evaluateResult());


        /**
         * Fourth test does equality evaluation and returns true or false if the "total"
         * operation count is same as the "constant"
         */
        rule = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"greater-than-or-equals\":[  \n" +
                "         {  \n" +
                "            \"total\":{  \n" +
                "               \"collection\":\"CAND_SET||null||null\",\n" +
                "               \"equals\":[  \n" +
                "                  {  \n" +
                "                     \"constant\":{  \n" +
                "                        \"value\":\"1\"\n" +
                "                     }\n" +
                "                  },\n" +
                "                  {  \n" +
                "                     \"search-path\":{  \n" +
                "                        \"value\":\"elementConfiguration||name=ADDRESS_PHYS||permissible\"\n" +
                "                     }\n" +
                "                  }\n" +
                "               ]\n" +
                "            }\n" +
                "         },\n" +
                "         {  \n" +
                "            \"constant\":{  \n" +
                "               \"data-type\":\"int\",\n" +
                "               \"value\":\"4\"\n" +
                "            }\n" +
                "         }\n" +
                "      ]\n" +
                "   }\n" +
                "}";
        ExpressedRule.Builder ruleBuilder3 = AbstractRule.expressedRuleBuilder("totalTest2", rule);
        ExpressedRule r3 = ruleBuilder3.build();
        fileData = loadFile("com/exsoinn/ie/rule/brel/testCollectionChange.json");
        ctx = ContextFactory.obtainContext(fileData);
        res = r3.apply(ctx, null, null, null, null);
        assertFalse(res.evaluateResult());
    }


    @Test
    public void maxTest() throws RuleException {
        String rule = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"selector\":{  \n" +
                "         \"element\":\"elementConfiguration||name=ADDRESS_PHYS||geoRefId,name,value,calculatedAgreementRatio,agreementRatio\",\n" +
                "         \"max\":{  \n" +
                "            \"collection\":\"CAND_SET||null||null\",\n" +
                "            \"element\":\"elementConfiguration||name=ADDRESS_PHYS||calculatedAgreementRatio\",\n" +
                "            \"greater-than-or-equals\":[  \n" +
                "               {  \n" +
                "                  \"search-path\":{  \n" +
                "                     \"value\":\"elementConfiguration||name=ADDRESS_PHYS||calculatedAgreementRatio\"\n" +
                "                  }\n" +
                "               },\n" +
                "               {  \n" +
                "                  \"default\":[  \n" +
                "                     {  \n" +
                "                        \"search-path\":{  \n" +
                "                           \"value\":\"elementConfiguration||name=ADDRESS_PHYS||agreementRatio\"\n" +
                "                        }\n" +
                "                     },\n" +
                "                     {  \n" +
                "                        \"search-path\":{  \n" +
                "                           \"value\":\"elementConfiguration||name=ADDRESS_PHYS||agreementRatio_default\"\n" +
                "                        }\n" +
                "                     }\n" +
                "                  ]\n" +
                "               }\n" +
                "            ]\n" +
                "         }\n" +
                "      }\n" +
                "   }\n" +
                "}";

        ExpressedRule.Builder ruleBuilder = AbstractRule.expressedRuleBuilder("maxTest", rule);
        ExpressedRule r = ruleBuilder.build();
        String fileData = loadFile("com/exsoinn/ie/rule/brel/testCollectionChange.json");
        Context ctx = ContextFactory.obtainContext(fileData);
        RuleExecutionResult res = r.apply(ctx, null, null, null, null);
        Context outCtx = res.evaluateResultAsContext().asArray().get(0);
        assertEquals("123 Main St", outCtx.memberValue("value").stringRepresentation());
        assertEquals("ADDRESS_PHYS", outCtx.memberValue("name").stringRepresentation());
        assertEquals("71", outCtx.memberValue("calculatedAgreementRatio").stringRepresentation());
        assertEquals("67", outCtx.memberValue("agreementRatio").stringRepresentation());
    }


    /**
     * Delete after sprint 17 demo
     */
    @Test
    public void demoTestOneUseCaseOne() throws RuleException {
        String rule = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"at-least\":{  \n" +
                "         \"number\":3,\n" +
                "         \"collection\":\"CAND_SET||null||null\",\n" +
                "         \"equals\":[  \n" +
                "            {  \n" +
                "               \"constant\":{  \n" +
                "                  \"value\":\"true\"\n" +
                "               }\n" +
                "            },\n" +
                "            {  \n" +
                "               \"search-path\":{  \n" +
                "                  \"value\":\"corroborationStatus||null||null\"\n" +
                "               }\n" +
                "            }\n" +
                "         ]\n" +
                "      }\n" +
                "   }\n" +
                "}";
        ExpressedRule.Builder ruleBuilder = AbstractRule.expressedRuleBuilder("demoTestOneUseCaseOne", rule);
        ExpressedRule r = ruleBuilder.build();
        String fileData = loadFile("com/exsoinn/ie/rule/brel/testCollectionRecordLevel.json");
        Context ctx = ContextFactory.obtainContext(fileData);
        RuleExecutionResult res = r.apply(ctx, null, null, null, null);
        assertTrue(res.evaluateResult());
        System.out.println("JMQ: res is " + res.toString());
    }


    @Test
    public void demoTestOneUseCaseTwo() throws RuleException {
        String rule = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"total\":{  \n" +
                "         \"collection\":\"CAND_SET||null||null\",\n" +
                "         \"equals\":[  \n" +
                "            {  \n" +
                "               \"constant\":{  \n" +
                "                  \"value\":\"true\"\n" +
                "               }\n" +
                "            },\n" +
                "            {  \n" +
                "               \"search-path\":{  \n" +
                "                  \"value\":\"corroborationStatus||null||null\"\n" +
                "               }\n" +
                "            }\n" +
                "         ]\n" +
                "      }\n" +
                "   }\n" +
                "}";
        ExpressedRule.Builder ruleBuilder = AbstractRule.expressedRuleBuilder("demoTestOneUseCaseTwo", rule);
        ExpressedRule r = ruleBuilder.build();
        String fileData = loadFile("com/exsoinn/ie/rule/brel/testCollectionRecordLevel.json");
        Context ctx = ContextFactory.obtainContext(fileData);
        RuleExecutionResult res = r.apply(ctx, null, null, null, null);
        //assertTrue(res.evaluateResult());
        System.out.println("JMQ: res is " + res.toString());
    }


    @Test
    public void demoTestTwoUseCaseOne() throws RuleException {
        String rule = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"total\":{  \n" +
                "         \"collection\":\"CAND_SET||null||null\",\n" +
                "         \"equals\":[  \n" +
                "            {  \n" +
                "               \"constant\":{  \n" +
                "                  \"value\":\"1\"\n" +
                "               }\n" +
                "            },\n" +
                "            {  \n" +
                "               \"search-path\":{  \n" +
                "                  \"value\":\"elementConfiguration||name=ADDRESS_PHYS||permissible\"\n" +
                "               }\n" +
                "            }\n" +
                "         ]\n" +
                "      }\n" +
                "   }\n" +
                "}";

        /**
         * First test looks for something which will yield a 0 count.
         */
        ExpressedRule.Builder ruleBuilder = AbstractRule.expressedRuleBuilder("demoTestTwoUseCaseOne", rule);
        ExpressedRule r = ruleBuilder.build();
        String fileData = loadFile("com/exsoinn/ie/rule/brel/testCollectionChange.json");
        Context ctx = ContextFactory.obtainContext(fileData);
        RuleExecutionResult res = r.apply(ctx, null, null, null, null);
        System.out.println("JMQ: res is " + res.toString());
        assertTrue(res.evaluateResult());
        assertTrue(Integer.valueOf(3).intValue() == Integer.valueOf(res.getOutput()).intValue());
    }


    /**
     * Tests the {@link com.exsoinn.ie.rule.definition.TotalElem}.
     * @throws RuleException
     */
    @Test
    public void demoTestTwoUseCaseTwo() throws RuleException {

        /**
         * Third test does equality evaluation and returns true or false if the "total"
         * operation count is same as the "constant"
         */
        String rule = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"equals\":[  \n" +
                "         {  \n" +
                "            \"total\":{  \n" +
                "               \"collection\":\"CAND_SET||null||null\",\n" +
                "               \"equals\":[  \n" +
                "                  {  \n" +
                "                     \"constant\":{  \n" +
                "                        \"value\":\"1\"\n" +
                "                     }\n" +
                "                  },\n" +
                "                  {  \n" +
                "                     \"search-path\":{  \n" +
                "                        \"value\":\"elementConfiguration||name=ADDRESS_PHYS||permissible\"\n" +
                "                     }\n" +
                "                  }\n" +
                "               ]\n" +
                "            }\n" +
                "         },\n" +
                "         {  \n" +
                "            \"constant\":{  \n" +
                "               \"data-type\":\"int\",\n" +
                "               \"value\":\"3\"\n" +
                "            }\n" +
                "         }\n" +
                "      ]\n" +
                "   }\n" +
                "}";
        ExpressedRule.Builder ruleBuilder2 = AbstractRule.expressedRuleBuilder("demoTestTwoUseCaseTwo", rule);
        ExpressedRule r2 = ruleBuilder2.build();
        String fileData = loadFile("com/exsoinn/ie/rule/brel/testCollectionChange.json");
        Context ctx = ContextFactory.obtainContext(fileData);
        RuleExecutionResult res = r2.apply(ctx, null, null, null, null);
        assertTrue(res.evaluateResult());
        System.out.println("JMQ: res is " + res.toString());
    }

    @Test
    public void demoTestTwoUseCaseThree() throws RuleException {
        /**
         * Fourth test does equality evaluation and returns true or false if the "total"
         * operation count is same as the "constant"
         */
        String rule = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"greater-than-or-equals\":[  \n" +
                "         {  \n" +
                "            \"total\":{  \n" +
                "               \"collection\":\"CAND_SET||null||null\",\n" +
                "               \"equals\":[  \n" +
                "                  {  \n" +
                "                     \"constant\":{  \n" +
                "                        \"value\":\"1\"\n" +
                "                     }\n" +
                "                  },\n" +
                "                  {  \n" +
                "                     \"search-path\":{  \n" +
                "                        \"value\":\"elementConfiguration||name=ADDRESS_PHYS||permissible\"\n" +
                "                     }\n" +
                "                  }\n" +
                "               ]\n" +
                "            }\n" +
                "         },\n" +
                "         {  \n" +
                "            \"constant\":{  \n" +
                "               \"data-type\":\"int\",\n" +
                "               \"value\":\"4\"\n" +
                "            }\n" +
                "         }\n" +
                "      ]\n" +
                "   }\n" +
                "}";
        ExpressedRule.Builder ruleBuilder3 = AbstractRule.expressedRuleBuilder("demoTestTwoUseCaseThree", rule);
        ExpressedRule r3 = ruleBuilder3.build();
        String fileData = loadFile("com/exsoinn/ie/rule/brel/testCollectionChange.json");
        Context ctx = ContextFactory.obtainContext(fileData);
        RuleExecutionResult res = r3.apply(ctx, null, null, null, null);
        assertFalse(res.evaluateResult());
        System.out.println("JMQ: res is " + res.toString());
    }


    @Test
    public void demoTestThree() throws RuleException {
        String rule = "{  \n" +
                "   \"rule-expression\":{  \n" +
                "      \"selector\":{  \n" +
                "         \"element\":\"elementConfiguration||name=ADDRESS_PHYS||geoRefId,name,value,calculatedAgreementRatio,agreementRatio\",\n" +
                "         \"max\":{  \n" +
                "            \"collection\":\"CAND_SET||null||null\",\n" +
                "            \"element\":\"elementConfiguration||name=ADDRESS_PHYS||calculatedAgreementRatio\",\n" +
                "            \"greater-than-or-equals\":[  \n" +
                "               {  \n" +
                "                  \"search-path\":{  \n" +
                "                     \"value\":\"elementConfiguration||name=ADDRESS_PHYS||calculatedAgreementRatio\"\n" +
                "                  }\n" +
                "               },\n" +
                "               {  \n" +
                "                  \"default\":[  \n" +
                "                     {  \n" +
                "                        \"search-path\":{  \n" +
                "                           \"value\":\"elementConfiguration||name=ADDRESS_PHYS||agreementRatio\"\n" +
                "                        }\n" +
                "                     },\n" +
                "                     {  \n" +
                "                        \"search-path\":{  \n" +
                "                           \"value\":\"elementConfiguration||name=ADDRESS_PHYS||agreementRatio_default\"\n" +
                "                        }\n" +
                "                     }\n" +
                "                  ]\n" +
                "               }\n" +
                "            ]\n" +
                "         }\n" +
                "      }\n" +
                "   }\n" +
                "}";

        ExpressedRule.Builder ruleBuilder = AbstractRule.expressedRuleBuilder("demoTestThree", rule);
        ExpressedRule r = ruleBuilder.build();
        String fileData = loadFile("com/exsoinn/ie/rule/brel/testCollectionChange.json");
        Context ctx = ContextFactory.obtainContext(fileData);
        RuleExecutionResult res = r.apply(ctx, null, null, null, null);
        System.out.println("JMQ: result is " + res.toString());
        System.out.println("JMQ: output is " + res.evaluateResultAsContext());
    }

    /*
     * Private methods
     */
    private String loadFile(String pFileName) {
        InputStream fileIs = getClass().getClassLoader()
                .getResourceAsStream(pFileName);
        BufferedReader fileBr = new BufferedReader(new InputStreamReader(fileIs));
        return fileBr.lines().collect(Collectors.joining());
    }
}
