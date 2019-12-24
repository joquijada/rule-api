package com.exsoinn.ie.rule;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by QuijadaJ on 9/29/2017.
 */
public class DuplicateListNameException extends RuleException {
    DuplicateListNameException(List<String> pListNames, QueryableLookupRule pRule) {
        super("Found one or more duplicate list name in component '" + pRule.name() + "'. Check he "
                + "configuration and try again. The offending list names are " + pListNames.stream().map(e -> e)
                .collect(Collectors.joining(", ")));
    }
}
