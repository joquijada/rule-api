var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");
var ruleName = Java.type("com.exsoinn.ie.rule.RuleName");

var tab_range = {"rec_code_to_type_table": [{"code": "15200", "type": "SSR"},{"code": "15201", "type": "MSR"}]};
ruleFactory.createQueryableDataRule(JSON.stringify(tab_range), ruleName.COMMON_REC_TYPE_CODE_CONV_TABLE);



/*
 * Configures data selection criteria that's common application-wide
 */
var tab_left_operand = {
   "left_operand_table":[
      {
         "id":"0",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15201||MTCH_GRD_TEXT",
         "description":"Finds the top MSR match grade pattern",
         "ignore_element_not_found_error":false
      },
      {
         "id":"1",
         "left_operand": "NO-EVAL:1",
         "description":"The value of '1'.",
         "ignore_element_not_found_error":false
      },
      {
         "id":"2",
         "left_operand": "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[2]||NULL||DNB_DATA_PRVD_CD",
         "description":"Finds the info source code.",
         "ignore_element_not_found_error":false
      },
      {
         "id":"3",
         "left_operand": "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15200||CFDC_LVL_VAL",
         "description":"Finds the top SSR confidence code",
         "ignore_element_not_found_error":false
      },
      {
         "id":"4",
         "left_operand": "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||REGN_STAT_CD=15200;CFDC_LVL_VAL=RULE:topSsrCcRange||MTCH_GRD_TEXT",
         "description":"Finds MGP's of all top SSR candidates using dynamically calculated range for the IE record in transit",
         "ignore_element_not_found_error":false
      },
      {
         "id":"5",
         "left_operand": "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||REGN_STAT_CD=15201;CFDC_LVL_VAL=RULE:topMsrCcRange||MTCH_GRD_TEXT",
         "description":"Finds MGP's of all top MSR candidates using dynamically calculated range for the IE record in transit",
         "ignore_element_not_found_error":false
      },
      {
         "id":"6",
         "left_operand": "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||null||null",
         "description":"Finds the candidate ref node.",
         "ignore_element_not_found_error":false
      },

      {
         "id":"7",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR||NME_CD=110;NME_ENTR_VW.STDN_APPL_CD=13135||NME_ENTR_VW.NME_TEXT",
         "description":"Business name, normalized",
         "ignore_element_not_found_error":true
      },
      {
         "id":"8",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[0].NME_ENTR_VW.NME_TEXT||NULL||NULL",
         "description":"Business name, raw",
         "ignore_element_not_found_error":false
      },
      {
         "id":"9",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR||NME_CD=110;NME_ENTR_VW.STDN_APPL_CD=24099||NME_ENTR_VW.NME_TEXT",
         "description":"Business name, standardized",
         "ignore_element_not_found_error":true
      },
      {
         "id":"10",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.PRIN_MGMT.PRIN.PRIN_NME_ENTR.PRIN_NME_ENTR_VW.NME_TEXT||NULL||NULL",
         "description":"CEO Name",
         "ignore_element_not_found_error":false
      },
      {
         "id":"11",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.POST_CODE",
         "description":"Mail address postal code, normalized",
         "ignore_element_not_found_error":false
      },
      {
         "id":"12",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.POST_CODE||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"13",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.POST_CODE",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"14",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.POST_CODE_EXTN",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"15",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.POST_CODE_EXTN||NULL||NULL",
         "description":"",
         "ignore_element_not_found_error":false
      },
      {
         "id":"16",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.POST_CODE_EXTN",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"17",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.POST_TOWN",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"18",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.POST_TOWN||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"19",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.POST_TOWN",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"20",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.TERR",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"21",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.TERR||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"22",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.TERR",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"23",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.TERR",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"24",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.TERR||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"25",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.TERR",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"26",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.POST_RTE",
         "description":"",
         "ignore_element_not_found_error":true
      },
      {
         "id":"27",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.POST_RTE||NULL||NULL",
         "description":"",
         "ignore_element_not_found_error":true
      },
      {
         "id":"28",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.POST_RTE",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"29",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.POST_RTE_BOX_NBR",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"30",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.POST_RTE_BOX_NBR||NULL||NULL",
         "description":"","ignore_element_not_found_error":true
      },
      {
         "id":"31",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1116;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.POST_RTE_BOX_NBR",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"32",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.TLCM_INFO.TLCM_ENTR.TLCM_ENTR_VW||TEL_NBR_CD=1110||TEL_NBR",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"33",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.TLCM_INFO.TLCM_ENTR.TEL_NBR||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"34",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.TLCM_INFO.TLCM_ENTR.TLCM_ENTR_VW||TEL_NBR_CD=1110||TEL_NBR",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"35",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.TLCM_INFO.TLCM_ENTR.TLCM_ENTR_VW||TEL_NBR_CD=1110||TEL_NBR",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"36",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.TLCM_INFO.TLCM_ENTR.AREA_CD||NULL||NULL",
         "description":"",
         "ignore_element_not_found_error":true
      },
      {
         "id":"37",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.TLCM_INFO.TLCM_ENTR.TLCM_ENTR_VW||TEL_NBR_CD=1110||TEL_NBR",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"38",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.BLDG_NME",
         "description":"",
         "ignore_element_not_found_error":true
      },
      {
         "id":"39",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.BLDG_NME||NULL||NULL",
         "description":"","ignore_element_not_found_error":true
      },
      {
         "id":"40",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.BLDG_NME",
         "description":"",
         "ignore_element_not_found_error":true
      },
      {
         "id":"41",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.TRDG_EST_NME",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"42",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.TRDG_EST_NME||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"43",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.TRDG_EST_NME",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"44",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.LOCN_TEXT",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"45",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.LOCN_TEXT||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"46",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.LOCN_TEXT",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"47",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||STD_STRG_VW.STD_RGN_LINE.TEXT",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"48",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.ADR_LINE.TEXT||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"49",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||STD_STRG_VW.STD_RGN_LINE.TEXT",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"50",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.POST_CODE",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"51",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.POST_CODE||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"52",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.POST_CODE",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"53",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.POST_CODE_EXTN",
         "description":"","ignore_element_not_found_error":false
      },/*
      TODO: Revisit, this is supposed to be for primary address, but mailing address configures the same, ask Sajith.
      ,
      {
         "id":"54",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.POST_CODE_EXTN||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      }*/
      {
         "id":"55",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.POST_CODE_EXTN",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"56",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.POST_TOWN",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"57",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.POST_TOWN||NULL||NULL",
         "description":"","ignore_element_not_found_error":false,"ignore_element_not_found_error":false
      },
      {
         "id":"58",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.POST_TOWN",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"59",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.STR_NME",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"60",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.STR_NME||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"61",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.STR_NME",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"62",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.TERR",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"63",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.TERR||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"64",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.TERR",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"65",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.SCDY_STR_NME",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"66",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.SCDY_STR_NME||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"67",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.SCDY_STR_NME",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"68",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.STR_DSGN_NME",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"69",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.STR_DSGN_NME||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"70",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.STR_DSGN_NME",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"71",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.STR_NBR_EXTN",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"72",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.STR_NBR_EXTN||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"73",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.STR_NBR_EXTN",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"74",
         "id":"74",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.STR_NBR_TO_EXTN",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"75",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.STR_NBR_TO_EXTN||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"76",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.STR_NBR_TO_EXTN",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"77",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.STR_TYP_TEXT",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"78",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW.STR_TYP_TEXT||NULL||NULL",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"79",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||ADR_ENTR_VW.STR_TYP_TEXT",
         "description":"","ignore_element_not_found_error":false
      },
      {
         "id":"80",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.ACTV.INDS_CODE[0]||NULL||STD_INDS_CODE",
         "description":"Standard Industry Code 1",
         "ignore_element_not_found_error":true
      },
      {
         "id":"81",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR||NME_CD=109;NME_ENTR_VW.DSPL_SEQ_NBR=1||NME_ENTR_VW.NME_TEXT",
         "description":"Business trading name 1",
         "ignore_element_not_found_error":false
      },
      {
         "id":"82",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR||NME_CD=109;NME_ENTR_VW.DSPL_SEQ_NBR=2||NME_ENTR_VW.NME_TEXT",
         "description":"Business trading name 2",
         "ignore_element_not_found_error":false
      },
      {
         "id":"83",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR||NME_CD=109;NME_ENTR_VW.DSPL_SEQ_NBR=3||NME_ENTR_VW.NME_TEXT",
         "description":"Business trading name 3",
         "ignore_element_not_found_error":false
      },
      {
         "id":"84",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR||NME_CD=109;NME_ENTR_VW.DSPL_SEQ_NBR=4||NME_ENTR_VW.NME_TEXT",
         "description":"Business trading name 4",
         "ignore_element_not_found_error":false
      },
      {
         "id":"85",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR||NME_CD=109;NME_ENTR_VW.DSPL_SEQ_NBR=5||NME_ENTR_VW.NME_TEXT",
         "description":"Business trading name 5",
         "ignore_element_not_found_error":false
      },
      {
         "id":"86",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.ACTV.INDS_CODE[1]||NULL||STD_INDS_CODE",
         "description":"Standard Industry Code 2",
         "ignore_element_not_found_error":true
      },
      {
         "id":"87",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.ACTV.INDS_CODE[2]||NULL||STD_INDS_CODE",
         "description":"Standard Industry Code 3",
         "ignore_element_not_found_error":true
      },
      {
         "id":"88",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.ACTV.INDS_CODE[3]||NULL||STD_INDS_CODE",
         "description":"Standard Industry Code 4",
         "ignore_element_not_found_error":true
      },
      {
         "id":"89",
         "left_operand":"VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR[0].ADR_ENTR_VW||NULL||GEO_REF_ID",
         "description":"Geo Ref ID Value",
         "ignore_element_not_found_error":true
      },
      {
        "id":"90",
        "left_operand":"CAND_SET||NULL||NULL",
        "description":"CAND_SET node",
        "ignore_element_not_found_error":false
      }
   ]
};



/*
 * Configures data selection criteria that's common application-wide
 */
var tab_right_operand = {
   "right_operand_table":[
      {
         "id":"0",
         "right_operand":"8,9,10",
         "description":"Range 8 - 10"
      },
      {
         "id":"1",
         "right_operand":"5,6,7",
         "description":"Range 5 - 7"
      },
      {
         "id":"2",
         "right_operand":"0,1,2,3,4",
         "description":"Range 0 - 4"
      },
      {
         "id":"3",
         "right_operand":"7,8,9,10",
         "description":"Range 7 - 10"
      },
      {
         "id":"4",
         "right_operand":"5,6",
         "description":"Range 5 - 6"
      },
      {
         "id":"5",
         "right_operand":"DUMMY",
         "description":"Dummy right operand; some operations (E.g. string handler blank check) do not require right operand."
      },
      {
         "id":"6",
         "right_operand":"7",
         "description":"Value 7"
      }
   ]
};

ruleFactory.createQueryableDataRule(JSON.stringify(tab_left_operand), "commonLeftOperand");
ruleFactory.createQueryableDataRule(JSON.stringify(tab_right_operand), "commonRightOperand");