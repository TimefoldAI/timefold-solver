package ai.timefold.solver.core.preview.api.domain.variable.declarative;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface ShadowVariableSession {
    void setVariable(Object entity, String variableName, @Nullable Object value);

    void setPrevious(Object entity, @Nullable Object previousValue);

    void setNext(Object entity, @Nullable Object nextValue);

    void setInverse(Object entity, @Nullable Object inverseValue);

    void updateVariables();
}
