package com.exsoinn.ie.rule.definition;

import com.exsoinn.ie.rule.RuleConstants;
import com.exsoinn.util.epf.Context;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Created by QuijadaJ on 10/12/2017.
 */
abstract class AbstractValueElem extends AbstractRuleExpression<Comparable<?>, Context> {
    private static final String DATA_TYPE_PROP_NAME = "data-type";
    private static final String VAL_PROP_NAME = "value";
    private final DataType dataType;
    private final String value;

    AbstractValueElem(String pElementName, Context pRuleExpCtx, Set<String> pReqElems) {
        super(pElementName, pRuleExpCtx, new SetBuilder<>().add(VAL_PROP_NAME).addAll(pReqElems).build());
        DataType dt = DataType.STRING;
        if (pRuleExpCtx.containsElement(DATA_TYPE_PROP_NAME)) {
            dt = DataType.fromClassHandle(pRuleExpCtx.memberValue(DATA_TYPE_PROP_NAME).stringRepresentation());
        }
        dataType = dt;
        value = pRuleExpCtx.memberValue(VAL_PROP_NAME).stringRepresentation();
    }


    @Override
    public Comparable<?> evaluate(Context pInput) {
        Class<?> retClass;
        try {
            retClass = Class.forName(getDataType().className());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Problem in " + getElementName() + ", content is "
                    + getElementContent().stringRepresentation() + ": " + e);
        }
        return convertStringToObject(valueForComputation(pInput), retClass);
    }

    private Comparable<?> convertStringToObject(String pVal, Class<?> pRetClass) {
        StringBuilder sb = new StringBuilder();
        sb.append("Problem with '");
        sb.append(getElementName());
        sb.append("' element, content ");
        sb.append(getElementContent().stringRepresentation());
        sb.append(". ");
        Exception thrown = null;
        try {
            if (Date.class.isAssignableFrom(pRetClass)) {
                return RuleConstants.DATE_FORMATTER.parse(pVal);
            } else if (String.class.isAssignableFrom(pRetClass)) {
                return pVal;
            } else if (Integer.class.isAssignableFrom(pRetClass)) {
                return Integer.valueOf(pVal);
            }
        } catch (Exception e) {
            thrown = e;
        }

        if (null != thrown) {
            sb.append("Problem converting ").append(pVal)
                    .append(" to object of type ").append(pRetClass.getName()).append(thrown);
        } else {
            sb.append("Class name not handled: ").append(pRetClass.getName());
        }
        throw new IllegalArgumentException(sb.toString());
    }


    enum DataType {
        STRING("string", "java.lang.String"),
        DATE("date", "java.util.Date"),
        INTEGER("int", "java.lang.Integer");

        private final String classHandle;
        private final String className;


        String className() {
            return className;
        }

        DataType(String pClassHandle, String pClassName) {
            classHandle = pClassHandle;
            className = pClassName;
        }

        static DataType fromClassHandle(String pName) {
            for (DataType e :DataType.values()) {
                if (e.classHandle.equals(pName)) {
                    return e;
                }
            }

            throw new IllegalArgumentException("Unrecognized class name '" + pName + "'. Only these"
                    + " are the recognized class names: "
                    + Arrays.stream(ConstantElem.DataType.values()).map(ConstantElem.DataType::toString).collect(Collectors.joining(", ")));
        }
    }

    String valueForComputation(Context pCtx) {
        return getValue();
    }

    /*
     * Getters/Setters
     */

    DataType getDataType() {
        return dataType;
    }

    String getValue() {
        return value;
    }
}
