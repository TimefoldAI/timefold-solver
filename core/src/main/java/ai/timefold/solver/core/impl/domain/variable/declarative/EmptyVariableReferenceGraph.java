package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.analysis.VariableLoop;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.Nullable;

final class EmptyVariableReferenceGraph implements VariableReferenceGraph {

    public static final EmptyVariableReferenceGraph INSTANCE = new EmptyVariableReferenceGraph();

    @Override
    public boolean updateChanged() {
        // No need to do anything.
        return true;
    }

    @Override
    public boolean hasPendingChanges() {
        return false;
    }

    @Override
    public @Nullable VariableChangeHook resolveHookFor(VariableMetaModel<?, ?, ?> variableReference) {
        return null;
    }

    @Override
    public List<VariableLoop> getVariableLoops() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "{}";
    }

}
