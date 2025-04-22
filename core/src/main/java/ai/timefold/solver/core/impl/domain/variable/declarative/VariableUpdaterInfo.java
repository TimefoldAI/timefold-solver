package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record VariableUpdaterInfo(
        DeclarativeShadowVariableDescriptor<?> variableDescriptor,
        @Nullable ShadowVariableLoopedVariableDescriptor<?> shadowVariableLoopedDescriptor,
        MemberAccessor memberAccessor,
        Function<Object, Object> calculator) {
}
