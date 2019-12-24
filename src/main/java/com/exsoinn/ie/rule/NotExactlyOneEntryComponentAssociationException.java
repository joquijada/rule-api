package com.exsoinn.ie.rule;

/**
 * Created by QuijadaJ on 9/19/2017.
 */
public class NotExactlyOneEntryComponentAssociationException extends RuleException {
    NotExactlyOneEntryComponentAssociationException(QueryableLookupRule pLookupComponent) {
        super("Did not find an entry data source associated with list data source " + pLookupComponent.dataSourceName()
                + ". The owning component is " + pLookupComponent.name() + ". All list data sources must join with one and only" +
                " one entry data source. Check configurations and try again.");
    }


}
