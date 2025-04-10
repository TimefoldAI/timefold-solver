package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record VariableSourceReference(Class<?> entityClass,
        String variableName,
        List<MemberAccessor> chainToVariable,
        boolean isTopLevel,
        boolean isDeclarative,
        VariableId targetVariableId,
        @Nullable VariableId downstreamDeclarativeVariable,
        BiConsumer<Object, Consumer<Object>> targetEntityFunctionStartingFromVariableEntity) {
    public boolean affectGraphEdges() {
        return downstreamDeclarativeVariable != null;
    }
}
