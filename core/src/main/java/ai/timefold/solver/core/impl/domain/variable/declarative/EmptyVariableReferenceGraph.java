package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

final class EmptyVariableReferenceGraph implements VariableReferenceGraph {

    public static final EmptyVariableReferenceGraph INSTANCE = new EmptyVariableReferenceGraph();

    @Override
    public boolean updateChanged() {
        // No need to do anything.
        return true;
    }

    @Override
    public void beforeVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        // No need to do anything.
    }

    @Override
    public List<Object> getInconsistentEntities() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "{}";
    }

}
