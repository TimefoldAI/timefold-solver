package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record VariableUpdaterInfo(Class<?> entityClass,
        String variableName,
        DeclarativeShadowVariableDescriptor<?> variableDescriptor,
        @Nullable InvalidityMarkerVariableDescriptor<?> invalidityMarkerVariableDescriptor,
        MemberAccessor memberAccessor,
        Function<Object, Object> calculator) {
    VariableId getVariableId() {
        return new VariableId(entityClass, variableName);
    }
}
