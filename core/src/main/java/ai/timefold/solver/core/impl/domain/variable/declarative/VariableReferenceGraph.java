package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

public sealed interface VariableReferenceGraph
        permits AbstractVariableReferenceGraph, EmptyVariableReferenceGraph, SingleDirectionalParentVariableReferenceGraph {

    /**
     * Update all declarative {@link ai.timefold.solver.core.api.domain.variable.ShadowVariable} that has
     * a source that was changed in either {@link #beforeVariableChanged(VariableMetaModel, Object)} or
     * {@link #afterVariableChanged(VariableMetaModel, Object)}.
     * <p>
     * Called after all {@link ai.timefold.solver.core.impl.domain.variable.VariableListener} are
     * triggered. Declarative {@link ai.timefold.solver.core.api.domain.variable.ShadowVariable}
     * are guaranteed to be the last variables to update.
     */
    void updateChanged();

    /**
     * Called before the variable corresponding to the {@link VariableMetaModel} on the given entity changes.
     *
     * @param variableReference The variable to be changed.
     * @param entity The entity that has the variable.
     */
    void beforeVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity);

    /**
     * Called after the variable corresponding to the {@link VariableMetaModel} on the given entity changes.
     *
     * @param variableReference The variable that changed.
     * @param entity The entity that has the variable.
     */
    void afterVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity);

    /**
     * Called before the planning list variable corresponding to the {@link VariableMetaModel}
     * on the given entity changes.
     * Removes the edges of declarative shadow variables sourced from the elements
     * in the changed range, since those elements may be moved elsewhere.
     *
     * @param variableReference The planning list variable to be changed.
     * @param entity The entity that has the list variable.
     * @param elementList The current (pre-change) elements of the list variable.
     * @param fromIndex The start of the changed range, inclusive.
     * @param toIndex The end of the changed range, exclusive.
     */
    default void beforeListVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity,
            List<Object> elementList, int fromIndex, int toIndex) {
        // Most graphs do not have edges that depend on a list variable's contents.
    }

    /**
     * Called after the planning list variable corresponding to the {@link VariableMetaModel}
     * on the given entity changes.
     * Adds the edges of declarative shadow variables sourced from the elements
     * in the changed range.
     *
     * @param variableReference The planning list variable that changed.
     * @param entity The entity that has the list variable.
     * @param elementList The current (post-change) elements of the list variable.
     * @param fromIndex The start of the changed range, inclusive.
     * @param toIndex The end of the changed range, exclusive.
     */
    default void afterListVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity,
            List<Object> elementList, int fromIndex, int toIndex) {
        // Most graphs do not have edges that depend on a list variable's contents.
    }

}
