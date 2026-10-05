package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import ai.timefold.solver.core.api.score.analysis.EntityVariablePair;
import ai.timefold.solver.core.api.score.analysis.VariableLoop;
import ai.timefold.solver.core.impl.domain.variable.ListVariableState;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;

/**
 * A variable reference graph that excludes a planning list variable's elements from the graph
 * and represents each list entity's chain by a single node instead;
 * see {@link GraphStructure#LIST_CHAIN}.
 * <p>
 * The graph itself covers everything else and is built by the normal machinery,
 * with fixed edges from each entity's declarative pre-chain variables to its chain node
 * and from its chain node to its post-chain variables;
 * a single {@link #updateChanged()} pass in topological order therefore walks each dirty chain
 * exactly once, after its pre-chain variables and before its post-chain variables.
 * This wrapper tracks the changes the chain nodes need, like any graph tracks its changed nodes:
 * it records the elements whose source variables changed and the list entities whose list
 * or genuine pre-chain variables changed,
 * classifies the elements by the chain of their entity, into the chain state the updater walks from,
 * and marks the dirty entities' chain nodes before delegating the update.
 * It also marks the list entity's post-chain variables changed on a list change,
 * which the graph derives from the list element locators the chain node skips.
 * <p>
 * The classification runs before the delegated update, and recomputes the elements that left their
 * list along the way; those writes come back here through the score director. The graph's own
 * reentrancy guard does not cover that window, since it only spans the update it delegates to,
 * hence {@link #isUpdating}: without it the classification would add to the list it is iterating.
 */
@NullMarked
final class ListChainVariableReferenceGraph<Solution_> implements VariableReferenceGraph {

    // These are immutable.
    private final AbstractVariableReferenceGraph<Solution_, ?> innerGraph;
    private final ListChainUpdater<Solution_> chainUpdater;
    private final ListVariableState<Solution_, Object, Object> listVariableState;
    private final String listVariableName;
    private final Class<?> elementEntityClass;
    private final Set<VariableMetaModel<?, ?, ?>> monitoredSourceVariableSet;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    /**
     * The list variable's after processors, which mark the list entity's post-chain variables changed.
     * Hoisted at construction to save a lookup by variable for every list change.
     */
    private final List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>> listVariableAfterProcessorList;

    // These are mutable.
    private final List<Object> changedElementList;
    // The unassigned elements the classification in progress recomputed, so that each is recomputed once.
    // The set finds them; the list empties the set one element at a time, since clearing it would cost
    // its capacity, which the initial update sizes for every unassigned element of the solution.
    private final Set<Object> recomputedUnassignedElementSet;
    private final List<Object> recomputedUnassignedElementList;
    private final List<ListChainUpdater.ChainState<Solution_>> dirtyChainStateList;
    private boolean isUpdating;

    ListChainVariableReferenceGraph(
            AbstractVariableReferenceGraph<Solution_, ?> innerGraph,
            ListChainUpdater<Solution_> chainUpdater,
            ListVariableState<Solution_, Object, Object> listVariableState,
            VariableMetaModel<Solution_, ?, ?> listVariableMetaModel,
            Class<?> elementEntityClass,
            EntityConsistencyState<Solution_, Object> elementConsistencyState,
            List<DeclarativeShadowVariableDescriptor<Solution_>> elementDescriptorList,
            ChangedVariableNotifier<Solution_> changedVariableNotifier,
            Object[] entities) {
        this.innerGraph = innerGraph;
        this.chainUpdater = chainUpdater;
        this.listVariableState = listVariableState;
        this.listVariableName = listVariableMetaModel.name();
        this.elementEntityClass = elementEntityClass;
        this.changedVariableNotifier = changedVariableNotifier;
        this.listVariableAfterProcessorList =
                innerGraph.variableReferenceToAfterProcessor.getOrDefault(listVariableMetaModel, List.of());
        this.changedElementList = new ArrayList<>();
        this.recomputedUnassignedElementSet = Collections.newSetFromMap(new IdentityHashMap<>());
        this.recomputedUnassignedElementList = new ArrayList<>();
        this.dirtyChainStateList = new ArrayList<>();
        this.isUpdating = false;

        this.monitoredSourceVariableSet = new HashSet<>();
        for (var descriptor : elementDescriptorList) {
            for (var source : descriptor.getSources()) {
                for (var sourceReference : source.variableSourceReferences()) {
                    monitoredSourceVariableSet.add(sourceReference.variableMetaModel());
                }
            }
        }

        // Every element gets an initial computation and starts consistent;
        // its consistency can only change when its entity becomes inconsistent.
        for (var entity : entities) {
            if (elementEntityClass.isInstance(entity)) {
                elementConsistencyState.setEntityIsInconsistent(changedVariableNotifier, entity, false);
                recordChangedElement(entity);
            }
        }
        updateChanged();
    }

    @Override
    public void beforeVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (isUpdating) {
            // A reentrant event of this graph's own update.
            return;
        }
        innerGraph.beforeVariableChanged(variableReference, entity);
    }

    @Override
    public void afterVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (isUpdating) {
            return;
        }
        if (monitoredSourceVariableSet.contains(variableReference)) {
            if (elementEntityClass.isInstance(entity)) {
                recordChangedElement(entity);
            } else {
                // A genuine pre-chain variable has no graph node to reach the chain node through.
                // Null for anything but a list entity of this graph, such as one of another working solution.
                var chainState = chainUpdater.getChainState(entity);
                if (chainState != null) {
                    markChainDirty(chainState);
                }
            }
        }
        innerGraph.afterVariableChanged(variableReference, entity);
    }

    @Override
    public void beforeListVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity,
            List<Object> elementList, int fromIndex, int toIndex) {
        // Nothing is recorded here: the list variable state changes the inverse and the previous or next
        // element of the elements that leave the list, and afterVariableChanged records them from those.
        innerGraph.beforeListVariableChanged(variableReference, entity, elementList, fromIndex, toIndex);
    }

    @Override
    public void afterListVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity,
            List<Object> elementList, int fromIndex, int toIndex) {
        // Delegated first, so that the graph fails fast on a list change during an update
        // before anything is recorded.
        innerGraph.afterListVariableChanged(variableReference, entity, elementList, fromIndex, toIndex);
        if (fromIndex < toIndex) {
            // The elements whose source variables changed record themselves; when none of them is in this list,
            // as when a forced update of every shadow variable simulates a change on every list,
            // the whole chain is walked.
            markChainDirty(Objects.requireNonNull(chainUpdater.getChainState(entity)));
        }
        markPostChainVariablesChanged(entity);
    }

    @Override
    public boolean updateChanged() {
        isUpdating = true;
        classifyChangedElements();
        var isUpdated = innerGraph.updateChanged();
        endUpdate(isUpdated);
        isUpdating = false;
        return isUpdated;
    }

    @Override
    public List<VariableLoop> getVariableLoops() {
        var innerVariableLoopList = innerGraph.getVariableLoops();
        var variableLoopList = new ArrayList<VariableLoop>(innerVariableLoopList.size());
        for (var innerVariableLoop : innerVariableLoopList) {
            // A loop that closes through a chain runs through its entity's chain node, which the graph
            // reports as the list variable; like the arbitrary graph, report the chain's elements instead.
            var involvedVariableSet = new LinkedHashSet<EntityVariablePair>();
            for (var entityVariablePair : innerVariableLoop.involvedVariableSet()) {
                var entity = entityVariablePair.entity();
                if (entityVariablePair.variableName().equals(listVariableName)
                        && chainUpdater.getChainState(entity) != null) {
                    chainUpdater.addElementVariables(entity, involvedVariableSet);
                } else {
                    involvedVariableSet.add(entityVariablePair);
                }
            }
            variableLoopList.add(new VariableLoop(involvedVariableSet));
        }
        return variableLoopList;
    }

    /**
     * Stands in for the mark the graph derives from the list element locators, which the chain node
     * skips along with the edges. Without it, removing the list's last element would leave no element
     * to walk and no edge to the entity, so nothing would recompute its post-chain variables.
     */
    private void markPostChainVariablesChanged(Object entity) {
        innerGraph.processEntity(listVariableAfterProcessorList, entity);
    }

    private void recordChangedElement(Object element) {
        // An element's list variable state changes all at once, so its events come in a row.
        if (changedElementList.isEmpty() || changedElementList.getLast() != element) {
            changedElementList.add(element);
        }
    }

    /**
     * Classifies the recorded elements by the chain of their list entity.
     * An unassigned element has no chain node to walk it, so it is recomputed here.
     */
    private void classifyChangedElements() {
        for (var element : changedElementList) {
            var listEntity = listVariableState.getInverseSingleton(element);
            if (listEntity == null) {
                // A move changing an element before unassigning it records it twice, apart.
                if (recomputedUnassignedElementSet.add(element)) {
                    recomputedUnassignedElementList.add(element);
                    chainUpdater.recomputeUnassignedElement(element, changedVariableNotifier);
                }
                continue;
            }
            var chainState = Objects.requireNonNull(chainUpdater.getChainState(listEntity));
            chainState.addChangedElementIndex(listVariableState.getIndexOrFail(element));
            markChainDirty(chainState);
        }
        changedElementList.clear();
        forgetRecomputedUnassignedElements();
    }

    private void markChainDirty(ListChainUpdater.ChainState<Solution_> chainState) {
        if (chainState.markDirty()) {
            dirtyChainStateList.add(chainState);
            innerGraph.markChanged(chainState.chainNode());
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private void forgetRecomputedUnassignedElements() {
        // Avoid creation of iterators on the hot path.
        for (var i = 0; i < recomputedUnassignedElementList.size(); i++) {
            recomputedUnassignedElementSet.remove(recomputedUnassignedElementList.get(i));
        }
        recomputedUnassignedElementList.clear();
    }

    private void endUpdate(boolean isUpdated) {
        for (var chainState : dirtyChainStateList) {
            chainState.endUpdate(isUpdated);
        }
        dirtyChainStateList.clear();
    }
}
