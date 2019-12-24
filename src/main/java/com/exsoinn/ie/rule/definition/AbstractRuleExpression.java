package com.exsoinn.ie.rule.definition;

import com.exsoinn.ie.util.Builder;
import com.exsoinn.util.epf.Context;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parent class to all classes that represent an element of an expressed rule. Enforcement of valid rule expression
 * format is done in the code. When adding a new element, the following activities need to take place:<br/><br/>
 * 1) Child classes will want to implement {@link this#allowedSubElements()}, which tells
 * the code during parsing the list of child elements allowed under this element. If a child
 * element name is encountered which is not found in {@link this#allowedSubElements()}, then the code will throw
 * {@link IllegalArgumentException}<br/><br/>
 * 2) The child class can add any additional member variables needed for the element it implements,
 * and inside the constructor instantiate those with values obtained from the <code>Context</code> passed to
 * the constructor. The context object passed to the element's constructor corresponds to the element's content defined
 * in the rule expression. Bottom line is each element is responsible for reading from the Context any values needed
 * for that elements functionality.<br/><br/>
 * 3) The child class is responsible for implementing {@link this#evaluate(Object)}, which gets called during execution
 * of the rule expression. This method gets called for every element and child, and children of child and so on, until
 * the full rule expression gets evaluated. Child class will best know implementation details of this method, which
 * really depends on the nature of the functionality the child class offers with respect to rule expression. For examples,
 * see {@link OrElem}, {@link AndElem}, {@link NotElem}.
 *
 * Created by QuijadaJ on 10/10/2017.
 */
public abstract class AbstractRuleExpression<T, U> implements RuleExpression<T, U> {
    private final String elementName;
    private final Context elementContent;
    private final List<RuleExpression<T, U>> subElements = new ArrayList<>();
    private final Set<String> requiredMembers = new HashSet<>();
    final static String NULL_STR = "null";


    AbstractRuleExpression(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        elementName = pElementName;
        elementContent = pRuleExpCtx;
        if (null != pReqElems && !pReqElems.isEmpty()) {
            requiredMembers.addAll(pReqElems);
        }
        validateRequiredMembers(pElementName, pRuleExpCtx, requiredMembers);
    }

    enum Element {
        ROOT("rule-expression", RootElem.class.getName()),
        OR("or", OrElem.class.getName()),
        AND("and", AndElem.class.getName()),
        NOT("not", NotElem.class.getName()),
        NULL("null", Void.class.getName()),
        CONSTANT("constant", ConstantElem.class.getName()),
        GT_THAN("greater-than", GreaterThanElem.class.getName()),
        GT_THAN_OR_EQ("greater-than-or-equals", GreaterThanOrEqualsElem.class.getName()),
        LESS_THAN("less-than", LessThanElem.class.getName()),
        LESS_THAN_OR_EQ("less-than-or-equals", LessThanOrEqualsElem.class.getName()),
        EQUALS("equals", EqualsElem.class.getName()),
        SEARCH_PATH("search-path", SearchPathElem.class.getName()),
        AT_LEAST("at-least", AtLeastElem.class.getName()),
        TOTAL("total", TotalElem.class.getName()),
        MAX("max", MaxElem.class.getName()),
        DEFAULT("default", DefaultElem.class.getName()),
        SELECTOR("selector", SelectorElem.class.getName());


        final private String elementName;
        final private String className;

        public String className() {
            return className;
        }

        public String elementName() {
            return elementName;
        }


        Element(String pElemName, String pClassName) {
            elementName = pElemName;
            className = pClassName;
        }


        static Element fromName(String pName) {
            for (Element e : Element.values()) {
                if (e.elementName.equals(pName)) {
                    return e;
                }
            }

            throw new IllegalArgumentException("Unrecognized element name '" + pName + "'. Only these"
                    + " are the recognized element names: "
                    + Arrays.stream(Element.values()).map(Element::toString).collect(Collectors.joining(", ")));
        }
    }


    abstract Set<Element> allowedSubElements();

    private void validateRequiredMembers(String pElemName, Context pCtx, Set<String> pReqElems) {
        Set<String> missing = new HashSet<>();
        for (String s : pReqElems) {
            if (!pCtx.containsElement(s)) {
                missing.add(s);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(buildPropertyMissingMessage(pElemName, missing, pCtx));
        }
    }




    List<RuleExpression<T, U>> subElements() {
        return Collections.unmodifiableList(subElements);
    }


    /**
     * The entry point to begin parsing a fully formed rule expression, passed in as a
     * <code>Context</code> object. The first, outer most, single-element is expected to
     * be <code>rule-expression</code>, otherwise error is thrown.
     * All sub-elements and their child elements will get parsed iteratively until entire rule
     * expression has been converted to Java objects.
     * @param pRuleExpCtx
     * @return
     */
    public static RuleExpression parse(Context pRuleExpCtx) {
        String rootElemName = Element.ROOT.elementName();
        if (pRuleExpCtx.entrySet().size() != 1 || !pRuleExpCtx.containsElement(rootElemName)) {
            throw new IllegalArgumentException("Passed in rule expression did not contain single root element '"
                    + rootElemName + "'. The passed in rule expression was " + pRuleExpCtx.stringRepresentation());
        }

        Context elemCtn = pRuleExpCtx.memberValue(rootElemName);
        RuleExpression re = new RootElem(rootElemName, elemCtn);
        ((AbstractRuleExpression) re).addSubElements(elemCtn);
        return re;
    }


    /**
     * Adds child elements (if any).
     *
     * @param pRuleExpCtx - The contents context associated with this element, from which
     *                    child elements will be created.
     */
    private void addSubElements(Context pRuleExpCtx) {
        List<Context> ctxList = new ArrayList<>();
        /**
         * Regardless of whether this is a complex object or array, add it to a list,
         * which farther below for() loop will consume. The reason that "pRuleExpCtx" might come
         * as array or complex object is that, some elements add their sub-elements as a list, because
         * for example JSON and other formats required unique property names, and some element names will be
         * re-used as needed by the parent element.
         */
        if (pRuleExpCtx.isArray()) {
            ctxList.addAll(pRuleExpCtx.asArray());
        } else if (pRuleExpCtx.isRecursible()) {
            ctxList.add(pRuleExpCtx);
        } else {
            return;
        }

        /**
         * Iterate over the members of this elements contents, and for members which are array or
         * another complex element (I.e. Context.isRecursible == true), create child element
         * {@link RuleExpression} objects, and add them to this element. Primitives are not considered
         * to be sub-elements, but instead properties which the owning element should handle in the respective
         * child class.
         */
        for (Context c : ctxList) {
            for (Map.Entry<String, Context> ent : c.entrySet()) {
                String childElemName = ent.getKey();
                Context childElemCtn = ent.getValue();
                if (!childElemCtn.isRecursible() && !childElemCtn.isArray()) {
                    continue;
                }
                try {
                    /**
                     * Add the child element to this element
                     */
                    RuleExpression childRe = this.addSubElement(childElemName, childElemCtn);

                    /**
                     * Now build the child's children also.
                     */
                    ((AbstractRuleExpression) childRe).addSubElements(childElemCtn);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }


    /**
     * Adds a sub-element to this element.
     * @param pElemName - The child element name
     * @param pRuleExpCtx - The Context which contains the child element's contents
     * @return - The {@link RuleExpression} object built out of the child element
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    RuleExpression addSubElement(String pElemName, Context pRuleExpCtx)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchMethodException,
            InvocationTargetException {
        Element e = Element.fromName(pElemName);
        if (!allowedSubElements().contains(e)) {
            throw new IllegalArgumentException("Element '" + e.elementName() + "' is not a recognized child "
                    + "element of '" + this.elementName + "'. Only these child elements are allowed: "
                    + this.allowedSubElements().stream().map(Element::toString).collect(Collectors.joining(", ")));
        }
        Class c = Class.forName(e.className());
        Constructor constructor = c.getDeclaredConstructor(String.class, Context.class, Set.class);
        Object elemObj = constructor.newInstance(e.elementName(), pRuleExpCtx, null);
        RuleExpression re = (RuleExpression) elemObj;
        this.subElements.add(re);
        return re;
    }

    private String buildPropertyMissingMessage(String pElemName, Set<String> pPropNames, Context pParentCtx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Element '");
        sb.append(pElemName);
        sb.append("' ");
        sb.append(" is missing property(ies) ");
        sb.append(pPropNames.stream().collect(Collectors.joining(", ", "'", "'")));
        sb.append(". This is what I got: ");
        sb.append("\"");
        sb.append(pElemName);
        sb.append("\":");
        sb.append(pParentCtx.stringRepresentation());
        return sb.toString();
    }


    static class SetBuilder<V> implements Builder<Set<V>> {
        private final Set<V> set = new HashSet<>();

        SetBuilder add(V pStr) {
            set.add(pStr);
            return this;
        }

        SetBuilder addAll(Set<V> pSet) {
            if (null != pSet && !pSet.isEmpty()) {
                set.addAll(pSet);
            }
            return this;
        }

        @Override
        public Set<V> build() {
            return set;
        }
    }


    void validateResultIsBoolean(Object pResObj) {
        if (!(pResObj instanceof Boolean)) {
            throw new IllegalArgumentException("Element class " + this.getClass().getName()
                    + " can only deal with expressions that return boolean. The expression returned "
                    + pResObj.getClass().getName() + ". This element name is " + getElementName() + ", and its contents"
                    + " is " + getElementContent().stringRepresentation());
        }
    }


    /*
     * Getters/Setters
     */
    public String getElementName() {
        return elementName;
    }

    public Context getElementContent() {
        return elementContent;
    }
}
