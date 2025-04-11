package ai.timefold.solver.core.preview.api.domain.variable.declarative;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface ShadowVariableSession {
    void setVariable(Object entity, String variableName, @Nullable Object value);

    void updateVariables();
}
