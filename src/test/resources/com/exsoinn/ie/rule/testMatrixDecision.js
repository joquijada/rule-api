var ruleFactory = Java.type("com.exsoinn.ie.rule.AbstractRule");
var tab_decision_matrix_with_default = {"flow_decision_table": [
                           {"decision": "DM", "top_ssr_conf_lvl": "H", "top_msr_conf_lvl": "H"},
                           {"decision": "DM", "top_ssr_conf_lvl": "H", "top_msr_conf_lvl": "M"},
                           {"decision": "FB", "top_ssr_conf_lvl": "H", "top_msr_conf_lvl": "L"},
                           {"decision": "DM", "top_ssr_conf_lvl": "M", "top_msr_conf_lvl": "H"},
                           {"decision": "DM", "top_ssr_conf_lvl": "M", "top_msr_conf_lvl": "M"},
                           {"decision": "FB", "top_ssr_conf_lvl": "M", "top_msr_conf_lvl": "L"},
                           {"decision": "DM", "top_ssr_conf_lvl": "L", "top_msr_conf_lvl": "H"},
                           {"decision": "DM", "top_ssr_conf_lvl": "L", "top_msr_conf_lvl": "M"},
                           {"decision": "FB", "top_ssr_conf_lvl": "L", "top_msr_conf_lvl": "L"},
                           {"decision": "DEFAULT_DECISION", "top_ssr_conf_lvl": "DEFAULT", "top_msr_conf_lvl": "DEFAULT"}]};

var tab_decision_matrix_without_default = {"flow_decision_table_without_default": [
                           {"decision": "DM", "top_ssr_conf_lvl": "H", "top_msr_conf_lvl": "H"},
                           {"decision": "DM", "top_ssr_conf_lvl": "H", "top_msr_conf_lvl": "M"},
                           {"decision": "FB", "top_ssr_conf_lvl": "H", "top_msr_conf_lvl": "L"},
                           {"decision": "DM", "top_ssr_conf_lvl": "M", "top_msr_conf_lvl": "H"},
                           {"decision": "DM", "top_ssr_conf_lvl": "M", "top_msr_conf_lvl": "M"},
                           {"decision": "FB", "top_ssr_conf_lvl": "M", "top_msr_conf_lvl": "L"},
                           {"decision": "DM", "top_ssr_conf_lvl": "L", "top_msr_conf_lvl": "H"},
                           {"decision": "DM", "top_ssr_conf_lvl": "L", "top_msr_conf_lvl": "M"},
                           {"decision": "FB", "top_ssr_conf_lvl": "L", "top_msr_conf_lvl": "L"}]};

ruleFactory.createQueryableDataRule(JSON.stringify(tab_decision_matrix_with_default), "testMatrixDecision");
ruleFactory.createQueryableDataRule(JSON.stringify(tab_decision_matrix_without_default), "testMatrixDecisionWithoutDefault");
