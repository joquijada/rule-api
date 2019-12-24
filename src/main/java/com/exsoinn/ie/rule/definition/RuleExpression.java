package com.exsoinn.ie.rule.definition;


/**
 * TODO: Move this interface to common module??
 * Created by QuijadaJ on 10/10/2017.
 */
public interface RuleExpression<T, U> {
    T evaluate(U pInput);
}
