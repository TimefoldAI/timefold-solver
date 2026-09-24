package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Updates the shadow variables of a {@link GraphNode} when the node is processed
 * by {@link AffectedEntitiesUpdater}.
 * <p>
 * A node usually carries one {@link VariableUpdaterInfo} per declarative shadow variable,
 * which recomputes a single value for a single entity (or an aligned group of entities).
 * A {@link ListElementBlockUpdater} instead updates a whole planning list variable's
 * elements, whose values differ per element and whose set changes during solving.
 */
@NullMarked
public sealed interface VariableUpdater<Solution_> permits VariableUpdaterInfo, ListElementBlockUpdater {

    /**
     * Identifies the node in {@link AbstractVariableReferenceGraph} lookups
     * and in error messages.
     */
    VariableMetaModel<Solution_, ?, ?> id();

    /**
     * The bucket {@link VariableReferenceGraphBuilder#addVariableReferenceEntity} groups nodes by:
     * it creates one node per key and entity, so the updaters sharing a key on the same entity
     * share a node.
     */
    Object nodeGroupKey();

    /**
     * The aligned entities all receiving this updater's value, or null when the updater
     * targets the single entity it is invoked with.
     */
    @Nullable
    Object[] groupEntities();

    /**
     * The consistency state of the entity class this updater's looped status applies to.
     */
    EntityConsistencyState<Solution_, Object> entityConsistencyState();

    /**
     * Recomputes the updater's shadow variables on the given entity,
     * or sets them to null when the entity is part of a dependency loop.
     *
     * @return true if any shadow variable value changed
     */
    boolean update(Object entity, boolean isEntityInconsistent, ChangedVariableNotifier<Solution_> changedVariableNotifier);
}
