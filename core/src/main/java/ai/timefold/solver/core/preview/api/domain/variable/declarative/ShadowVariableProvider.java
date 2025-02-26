package ai.timefold.solver.core.preview.api.domain.variable.declarative;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ShadowVariableProvider {
    void defineVariables(ShadowVariableFactory shadowVariableFactory);
}
