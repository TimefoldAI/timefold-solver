package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public sealed interface VariableReferenceGraph<Solution_>
        permits AbstractVariableReferenceGraph, DefaultVariableReferenceGraph, EmptyVariableReferenceGraph,
        FixedVariableReferenceGraph, SingleDirectionalParentVariableReferenceGraph {

    @Nullable
    EntityVariablePair<Solution_> lookupOrNull(VariableMetaModel<?, ?, ?> variableId, Object entity);

    void addEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to);

    void removeEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to);

    void markChanged(@NonNull EntityVariablePair<Solution_> node);

    void updateChanged();

    void beforeVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity);

    void afterVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity);

    /**
     * If afterVariableChanged events should be queued instead of executed immediately.
     * Needed if variables are updated in afterVariableChanged instead of updateChanged
     * to guarantee the input variables are consistent with one another.
     * 
     * @return true iff the afterVariableChanged events should be queued.
     */
    boolean shouldQueueAfterEvents();

}
