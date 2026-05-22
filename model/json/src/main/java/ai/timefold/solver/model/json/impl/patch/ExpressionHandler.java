package ai.timefold.solver.model.json.impl.patch;

import com.fasterxml.jackson.databind.JsonNode;

public class ExpressionHandler {

    private static final String EQ_OPERATOR = "=";

    private String fieldOrPath;

    private String operator;

    private String value;

    public ExpressionHandler(String fieldOrPath, String operator, String value) {
        this.fieldOrPath = fieldOrPath;
        this.operator = operator;
        this.value = value;
    }

    public static ExpressionHandler from(String expressionString) {
        if (expressionString.startsWith("[") && expressionString.endsWith("]")) {

            String expression = expressionString.substring(1, expressionString.length() - 1);

            if (expression.contains(EQ_OPERATOR)) {
                String[] expParts = expression.split(EQ_OPERATOR);
                String fieldName = expParts[0];
                String fieldValue = expParts[1];

                return new ExpressionHandler(fieldName, EQ_OPERATOR, fieldValue);
            } else {
                // only value is given as expression - match the item not a field
                return new ExpressionHandler("", EQ_OPERATOR, expression);
            }

        } else {
            throw new IllegalArgumentException(
                    String.format("Expression '%s' is not valid filter expression", expressionString));
        }
    }

    public boolean evaluate(JsonNode val) {

        switch (operator) {
            case EQ_OPERATOR: {
                if (!fieldOrPath.isBlank()) {
                    if (fieldOrPath.contains("/")) {
                        int lastPathIndexProp = fieldOrPath.toString().lastIndexOf('/');
                        String propertyName = fieldOrPath.toString().substring(lastPathIndexProp + 1);

                        return val.has(propertyName) && val.get(propertyName).textValue().equals(value);
                    } else {

                        return val.has(fieldOrPath) && val.get(fieldOrPath).textValue().equals(value);
                    }
                } else {
                    return val.textValue().equals(value);
                }
            }
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }

    }

    public String getFieldOrPath() {
        return fieldOrPath;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }
}
