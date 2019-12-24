package com.exsoinn.ie.rule;

import java.util.List;

/**
 * This interface is meant to be implemented by classes that can contain a collection
 * of Rule objects.
 *
 * Created by QuijadaJ on 4/19/2017.
 */
public interface RuleContainer {
    public List<Rule> rules();
}
