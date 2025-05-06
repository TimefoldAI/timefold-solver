package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class EmptyWorkingReferenceGraph<Solution_> implements WorkingReferenceGraph<Solution_> {

    @SuppressWarnings("rawtypes")
    public static final EmptyWorkingReferenceGraph INSTANCE = new EmptyWorkingReferenceGraph<>();

    @Override
    public @Nullable EntityVariablePair<Solution_> lookupOrNull(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        return null;
    }

    @Override
    public void addEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
        throw new IllegalStateException("Impossible state: cannot modify an empty graph.");
    }

    @Override
    public void removeEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
        throw new IllegalStateException("Impossible state: cannot modify an empty graph.");
    }

    @Override
    public void markChanged(@NonNull EntityVariablePair<Solution_> node) {
        throw new IllegalStateException("Impossible state: cannot modify an empty graph.");
    }

    @Override
    public void updateChanged() {
        // No need to do anything.
    }

    @Override
    public String toString() {
        return "{}";
    }

}
