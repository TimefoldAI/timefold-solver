package ai.timefold.solver.core.preview.api.variable.provided;

public interface ShadowVariableSession {
    void setVariable(Object entity, String variableName, Object value);

    void setPrevious(Object entity, Object previousValue);

    void setNext(Object entity, Object nextValue);

    void setInverse(Object entity, Object inverseValue);

    void updateVariables();
}
