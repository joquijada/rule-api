package com.exsoinn.ie.rule.data;

import com.exsoinn.ie.util.IdentifiedByName;

/**
 * Created by QuijadaJ on 9/19/2017.
 */
public interface Source<Q, R> extends IdentifiedByName {

    R retrieveData(Q q) throws SourceException;

    R insertData(Q q) throws SourceException;

}
