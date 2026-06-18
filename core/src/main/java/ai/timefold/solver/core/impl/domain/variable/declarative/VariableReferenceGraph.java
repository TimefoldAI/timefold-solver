package ai.timefold.solver.core.impl.domain.variable.declarative;

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
     *
     * @return true if the update successful; false otherwise
     */
    boolean updateChanged();

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

}
