package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record VariableSourceReference(VariableMetaModel<?, ?, ?> variableMetaModel,
        List<MemberAccessor> chainToVariableEntity,
        boolean onRootEntity,
        boolean isTopLevel,
        boolean isBottomLevel,
        boolean isDeclarative,
        VariableMetaModel<?, ?, ?> targetVariableMetamodel,
        @Nullable VariableMetaModel<?, ?, ?> downstreamDeclarativeVariableMetamodel,
        Function<Object, @Nullable Object> targetEntityFunctionStartingFromVariableEntity) {

    public boolean affectGraphEdges() {
        return downstreamDeclarativeVariableMetamodel != null;
    }

    public @Nullable Object findTargetEntity(Object entity) {
        return targetEntityFunctionStartingFromVariableEntity.apply(entity);
    }

}
