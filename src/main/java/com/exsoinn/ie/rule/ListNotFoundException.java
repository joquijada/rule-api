package com.exsoinn.ie.rule;

/**
 * Created by QuijadaJ on 9/29/2017.
 */
public class ListNotFoundException extends RuleException {
    ListNotFoundException(String pListName, QueryableLookupRule pRule) {
        super("List name '" + pListName + "' is not managed by component '" + pRule.name()
                + "'. Check configurations and try again. This component is owner of the following lists: "
                + pRule.listNames());
    }
}
