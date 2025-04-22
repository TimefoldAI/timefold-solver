package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record VariableSourceReference(VariableMetaModel<?, ?, ?> variableMetaModel,
        List<MemberAccessor> chainToVariableEntity,
        boolean isTopLevel,
        boolean isBottomLevel,
        boolean isDeclarative,
        VariableMetaModel<?, ?, ?> targetVariableMetamodel,
        @Nullable VariableMetaModel<?, ?, ?> downstreamDeclarativeVariableMetamodel,
        BiConsumer<Object, Consumer<Object>> targetEntityFunctionStartingFromVariableEntity) {
    public boolean affectGraphEdges() {
        return downstreamDeclarativeVariableMetamodel != null;
    }
}
