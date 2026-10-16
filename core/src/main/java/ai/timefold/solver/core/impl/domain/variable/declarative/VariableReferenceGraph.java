package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;

import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.score.analysis.VariableLoop;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.Nullable;

public sealed interface VariableReferenceGraph
        permits AbstractVariableReferenceGraph, EmptyVariableReferenceGraph, ListElementCascadeVariableReferenceGraph,
        SingleDirectionalParentVariableReferenceGraph {

    /**
     * Update all declarative {@link ShadowVariable} that has
     * a source that was changed through a {@link VariableChangeHook}.
     * <p>
     * Called after all other shadow variables are
     * updated. Declarative {@link ShadowVariable}
     * are guaranteed to be the last variables to update.
     *
     * @return true if the update successful; false otherwise
     */
    boolean updateChanged();

    List<VariableLoop> getVariableLoops();

    /**
     * True if a node is marked as changed and {@link #updateChanged()} has not processed it yet.
     */
    boolean hasPendingChanges();

    /**
     * Resolves once what a change of the variable does in this graph;
     * cheap to call, returns the same instance.
     *
     * @return null if changes to that variable never affect this graph
     */
    @Nullable
    VariableChangeHook resolveHookFor(VariableMetaModel<?, ?, ?> variableReference);

    /**
     * What changes of one variable do in this graph, resolved once by {@link #resolveHookFor(VariableMetaModel)}.
     */
    interface VariableChangeHook {

        void beforeVariableChanged(Object entity);

        void afterVariableChanged(Object entity);

        default void beforeListVariableChanged(Object entity, List<Object> elementsBeforeChange, int fromIndex, int toIndex) {
            // Most graphs do not have edges that depend on a list variable's contents.
        }

        default void afterListVariableChanged(Object entity, List<Object> elementsAfterChange, int fromIndex, int toIndex) {
            // Most graphs do not have edges that depend on a list variable's contents.
        }

    }
}
