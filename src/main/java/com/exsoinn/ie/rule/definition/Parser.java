package com.exsoinn.ie.rule.definition;

/**
 * Created by QuijadaJ on 10/10/2017.
 */
public interface Parser<T, U> {

    T parse(U pExpression);
}
