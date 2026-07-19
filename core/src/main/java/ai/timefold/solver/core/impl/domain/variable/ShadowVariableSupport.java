package ai.timefold.solver.core.impl.domain.variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.cascade.CascadingUpdateShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.ConsistencyTracker;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultShadowVariableSession;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultShadowVariableSessionFactory;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultTopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.TopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.domain.variable.violation.ListVariableTracker;
import ai.timefold.solver.core.impl.domain.variable.violation.ShadowVariablesAssert;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.util.LinkedIdentityHashSet;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This class is not thread-safe.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public final class ShadowVariableSupport<Solution_> implements SupplyManager {

    public static <Solution_> ShadowVariableSupport<Solution_> create(InnerScoreDirector<Solution_, ?> scoreDirector) {
        return new ShadowVariableSupport<>(scoreDirector,
                TimefoldSolverEnterpriseService.loadOrDefault(service -> service::buildTopologyGraph,
                        () -> DefaultTopologicalOrderGraph::new));
    }

    private static final int SHADOW_VARIABLE_VIOLATION_DISPLAY_LIMIT = 3;
    private final InnerScoreDirector<Solution_, ?> scoreDirector;
    private final Map<Demand<?>, SupplyWithDemandCount> supplyMap = new HashMap<>();

    private final @Nullable ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final List<ListVariableChange> listVariableChangeList;
    private final Set<Object> unassignedValueWithEmptyInverseEntitySet;
    private final List<CascadingUpdateShadowVariableDescriptor<Solution_>> cascadingUpdateShadowVarDescriptorList;
    private final IntFunction<TopologicalOrderGraph> shadowVariableGraphCreator;

    /**
     * Indexed by [{@link EntityDescriptor#getOrdinal()}][{@link VariableDescriptor#getOrdinal()}].
     */
    private final List<BasicVariableChangeHandler<Solution_>>[][] basicVariableChangeHandlerArray;
    private final List<ListVariableTracker<Solution_>> listVariableTrackerList = new ArrayList<>();

    private boolean dirty = false;
    @Nullable
    private DefaultShadowVariableSession<Solution_> shadowVariableSession = null;
    private ConsistencyTracker<Solution_> consistencyTracker = new ConsistencyTracker<>();

    @Nullable
    private ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply = null;
    private final List<BasicVariableStateDemand<Solution_>> basicVariableStateDemandList = new ArrayList<>();

    @SuppressWarnings("unchecked")
    ShadowVariableSupport(InnerScoreDirector<Solution_, ?> scoreDirector,
            IntFunction<TopologicalOrderGraph> shadowVariableGraphCreator) {
        this.scoreDirector = Objects.requireNonNull(scoreDirector);

        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptorList = solutionDescriptor.getEntityDescriptors();
        this.basicVariableChangeHandlerArray = new List[entityDescriptorList.size()][];
        for (var entityDescriptor : entityDescriptorList) {
            var declaredVariableDescriptorList = entityDescriptor.getDeclaredVariableDescriptors();
            var array = new List[declaredVariableDescriptorList.size()];
            for (var variableDescriptor : declaredVariableDescriptorList) {
                array[variableDescriptor.getOrdinal()] = new ArrayList<BasicVariableChangeHandler<Solution_>>();
            }
            basicVariableChangeHandlerArray[entityDescriptor.getOrdinal()] = array;
        }

        // Fields specific to list variable; will be ignored if not necessary.
        this.listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        this.cascadingUpdateShadowVarDescriptorList =
                listVariableDescriptor != null ? solutionDescriptor.getEntityDescriptors().stream()
                        .flatMap(e -> e.getDeclaredCascadingUpdateShadowVariableDescriptors().stream())
                        .toList() : Collections.emptyList();
        var hasCascadingUpdates = !cascadingUpdateShadowVarDescriptorList.isEmpty();
        this.listVariableChangeList = new ArrayList<>();
        this.unassignedValueWithEmptyInverseEntitySet =
                hasCascadingUpdates ? new LinkedIdentityHashSet<>() : Collections.emptySet();
        this.shadowVariableGraphCreator = shadowVariableGraphCreator;
    }

    public void linkShadowVariables() {
        listVariableStateSupply = listVariableDescriptor == null ? null : demand(listVariableDescriptor.getStateDemand());
        scoreDirector.getSolutionDescriptor().getEntityDescriptors().stream()
                .map(EntityDescriptor::getDeclaredShadowVariableDescriptors)
                .flatMap(Collection::stream)
                .forEach(this::linkShadowVariable);
    }

    // All information about elements in all shadow variables is tracked in a centralized place.
    // Therefore, all list-related shadow variables need to be connected to that centralized place.
    // Shadow variables which are not related to a list variable are processed normally.
    // Cascading, declarative, and inconsistent shadow variables are routed elsewhere and need no wiring here.
    private void linkShadowVariable(ShadowVariableDescriptor<Solution_> descriptor) {
        if (descriptor instanceof InverseRelationShadowVariableDescriptor<Solution_> inverseRelationShadowVariableDescriptor) {
            if (inverseRelationShadowVariableDescriptor.isListVariableSource()) {
                if (listVariableStateSupply != null) {
                    processShadowVariableDescriptorWithListVariable(inverseRelationShadowVariableDescriptor,
                            listVariableStateSupply);
                }
            } else {
                var basicVariableStateDemand = inverseRelationShadowVariableDescriptor.getProvidedDemand();
                demand(basicVariableStateDemand).externalize(inverseRelationShadowVariableDescriptor);
                basicVariableStateDemandList.add(basicVariableStateDemand);
            }
        } else if (listVariableStateSupply != null
                && (descriptor instanceof IndexShadowVariableDescriptor<Solution_>
                        || descriptor instanceof PreviousElementShadowVariableDescriptor<Solution_>
                        || descriptor instanceof NextElementShadowVariableDescriptor<Solution_>)) {
            // When multiple variable types are used,
            // the shadow variable process needs to account for each variable
            // and process them according to their types.
            processShadowVariableDescriptorWithListVariable(descriptor, listVariableStateSupply);
        }
    }

    private void processShadowVariableDescriptorWithListVariable(ShadowVariableDescriptor<Solution_> shadowVariableDescriptor,
            ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply) {
        switch (shadowVariableDescriptor) {
            case IndexShadowVariableDescriptor<Solution_> indexShadowVariableDescriptor ->
                listVariableStateSupply.externalize(indexShadowVariableDescriptor);
            case InverseRelationShadowVariableDescriptor<Solution_> inverseRelationShadowVariableDescriptor ->
                listVariableStateSupply.externalize(inverseRelationShadowVariableDescriptor);
            case PreviousElementShadowVariableDescriptor<Solution_> previousElementShadowVariableDescriptor ->
                listVariableStateSupply.externalize(previousElementShadowVariableDescriptor);
            case NextElementShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor ->
                listVariableStateSupply.externalize(nextElementShadowVariableDescriptor);
            default -> // The list variable supply supports no other shadow variables.
                throw new IllegalStateException(
                        "Impossible state: list-variable-source shadow variable %s (%s) is not Index, InverseRelation, Previous, or Next."
                                .formatted(shadowVariableDescriptor.getVariableName(),
                                        shadowVariableDescriptor.getClass().getSimpleName()));
        }
    }

    @Override
    public Consumer<Object> getStateChangeNotifier() {
        return scoreDirector.getNeighborhoodNotifier();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Supply_ extends Supply> Supply_ demand(Demand<Supply_> demand) {
        var supplyWithDemandCount = supplyMap.get(demand);
        if (supplyWithDemandCount == null) {
            var newSupplyWithDemandCount = new SupplyWithDemandCount(createSupply(demand), 1L);
            supplyMap.put(demand, newSupplyWithDemandCount);
            return (Supply_) newSupplyWithDemandCount.supply;
        } else {
            var supply = supplyWithDemandCount.supply;
            var newSupplyWithDemandCount = new SupplyWithDemandCount(supply, supplyWithDemandCount.demandCount + 1L);
            supplyMap.put(demand, newSupplyWithDemandCount);
            return (Supply_) supply;
        }
    }

    @SuppressWarnings("unchecked")
    private Supply createSupply(Demand<?> demand) {
        var supply = demand.createExternalizedSupply(this);
        if (supply instanceof BasicVariableChangeHandler<?> handler) {
            var basicVariableChangeHandler = (BasicVariableChangeHandler<Solution_>) handler;
            resetWorkingSolutionIfSet(() -> basicVariableChangeHandler.resetWorkingSolution(scoreDirector));
            registerBasicVariableChangeHandler(basicVariableChangeHandler);
        } else if (supply instanceof ListVariableStateSupply<?, ?, ?> rawListStateSupply) {
            var listStateSupply = (ListVariableStateSupply<Solution_, ?, ?>) rawListStateSupply;
            resetWorkingSolutionIfSet(() -> listStateSupply.resetWorkingSolution(scoreDirector));
        } else if (supply instanceof ListVariableTracker<?> tracker) {
            var listVariableTracker = (ListVariableTracker<Solution_>) tracker;
            resetWorkingSolutionIfSet(() -> listVariableTracker.resetWorkingSolution(scoreDirector));
            listVariableTrackerList.add(listVariableTracker);
        }
        return supply;
    }

    private void resetWorkingSolutionIfSet(Runnable resetWorkingSolution) {
        // An external ScoreDirector can be created before the working solution is set.
        if (scoreDirector.getWorkingSolution() != null) {
            resetWorkingSolution.run();
        }
    }

    private void registerBasicVariableChangeHandler(BasicVariableChangeHandler<Solution_> handler) {
        var variableDescriptor = handler.getSourceVariableDescriptor();
        var handlerList = getBasicVariableChangeHandlerList(variableDescriptor);
        handlerList.add(handler);
    }

    private List<BasicVariableChangeHandler<Solution_>>
            getBasicVariableChangeHandlerList(VariableDescriptor<Solution_> variableDescriptor) {
        return basicVariableChangeHandlerArray[variableDescriptor.getEntityDescriptor().getOrdinal()][variableDescriptor
                .getOrdinal()];
    }

    @Override
    public <Supply_ extends Supply> boolean cancel(Demand<Supply_> demand) {
        var supplyWithDemandCount = supplyMap.get(demand);
        if (supplyWithDemandCount == null) {
            return false;
        }
        if (supplyWithDemandCount.demandCount == 1L) {
            supplyMap.remove(demand);
        } else {
            supplyMap.put(demand,
                    new SupplyWithDemandCount(supplyWithDemandCount.supply, supplyWithDemandCount.demandCount - 1L));
        }
        return true;
    }

    @Override
    public <Supply_ extends Supply> long getActiveCount(Demand<Supply_> demand) {
        var supplyAndDemandCounter = supplyMap.get(demand);
        if (supplyAndDemandCounter == null) {
            return 0L;
        } else {
            return supplyAndDemandCounter.demandCount;
        }
    }

    public ConsistencyTracker<Solution_> getConsistencyTracker() {
        return consistencyTracker;
    }

    public void setConsistencyTracker(ConsistencyTracker<Solution_> consistencyTracker) {
        this.consistencyTracker = consistencyTracker;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public void resetWorkingSolution() {
        if (listVariableStateSupply != null) {
            listVariableStateSupply.resetWorkingSolution(scoreDirector);
        }
        for (var handlerList : basicVariableChangeHandlerArray) {
            for (var handlers : handlerList) {
                for (var handler : handlers) {
                    handler.resetWorkingSolution(scoreDirector);
                }
            }
        }
        for (var listVariableTracker : listVariableTrackerList) {
            listVariableTracker.resetWorkingSolution(scoreDirector);
        }

        if (!scoreDirector.getSolutionDescriptor().getDeclarativeShadowVariableDescriptors().isEmpty()
                && !consistencyTracker.isFrozen()) {
            var shadowVariableSessionFactory = new DefaultShadowVariableSessionFactory<>(
                    scoreDirector.getSolutionDescriptor(),
                    scoreDirector,
                    shadowVariableGraphCreator);
            shadowVariableSession =
                    shadowVariableSessionFactory.forSolution(consistencyTracker, scoreDirector.getWorkingSolution());
        }
    }

    public void close() {
        if (listVariableStateSupply != null) {
            listVariableStateSupply.close();
        }
        for (var handlerList : basicVariableChangeHandlerArray) {
            for (var handlers : handlerList) {
                for (var handler : handlers) {
                    handler.close();
                }
            }
        }
        for (var listVariableTracker : listVariableTrackerList) {
            listVariableTracker.close();
        }
        if (listVariableDescriptor != null && listVariableStateSupply != null) {
            cancel(listVariableDescriptor.getStateDemand());
        }
        for (var basicVariableStateDemand : basicVariableStateDemandList) {
            cancel(basicVariableStateDemand);
        }
    }

    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        var handlerList = getBasicVariableChangeHandlerList(variableDescriptor);
        for (var handler : handlerList) {
            handler.beforeVariableChanged(scoreDirector, entity);
        }
        if (shadowVariableSession != null) {
            shadowVariableSession.beforeVariableChanged(variableDescriptor, entity);
            dirty = true;
        }
    }

    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        var handlerList = getBasicVariableChangeHandlerList(variableDescriptor);
        for (var handler : handlerList) {
            handler.afterVariableChanged(scoreDirector, entity);
        }
        if (shadowVariableSession != null) {
            shadowVariableSession.afterVariableChanged(variableDescriptor, entity);
        }
    }

    public void afterElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        if (listVariableStateSupply != null) {
            listVariableStateSupply.afterListElementUnassigned(scoreDirector, element);
        }
        for (var listVariableTracker : listVariableTrackerList) {
            listVariableTracker.afterListElementUnassigned(scoreDirector, element);
        }
        if (!cascadingUpdateShadowVarDescriptorList.isEmpty()) { // Only necessary if there is a cascade.
            unassignedValueWithEmptyInverseEntitySet.add(element);
            dirty = true;
        }
        if (shadowVariableSession != null) {
            // List changes may affect declarative shadow variables even when no basic variable event fires,
            // because the externalized shadow processors skip writes when the recomputed value is unchanged.
            dirty = true;
        }
    }

    public void beforeListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        if (listVariableStateSupply != null) {
            listVariableStateSupply.beforeListVariableChanged(scoreDirector, entity, fromIndex, toIndex);
        }
        for (var listVariableTracker : listVariableTrackerList) {
            listVariableTracker.beforeListVariableChanged(scoreDirector, entity, fromIndex, toIndex);
        }
    }

    public void afterListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        if (listVariableStateSupply != null) {
            listVariableStateSupply.afterListVariableChanged(scoreDirector, entity, fromIndex, toIndex);
        }
        for (var listVariableTracker : listVariableTrackerList) {
            listVariableTracker.afterListVariableChanged(scoreDirector, entity, fromIndex, toIndex);
        }
        if (!cascadingUpdateShadowVarDescriptorList.isEmpty()) { // Only necessary if there is a cascade.
            listVariableChangeList.add(new ListVariableChange(entity, fromIndex, toIndex));
            dirty = true;
        }
        if (shadowVariableSession != null) {
            // See afterElementUnassigned().
            dirty = true;
        }
    }

    public InnerScoreDirector<Solution_, ?> getScoreDirector() {
        return scoreDirector;
    }

    public void updateShadowVariables() {
        if (!dirty) {
            // Shortcut in case the trigger is called multiple times in a row,
            // without any notifications inbetween.
            // This is better than trying to ensure that the situation never ever occurs.
            return;
        }
        if (listVariableDescriptor != null) {
            // If there is no cascade, skip the whole thing.
            // If there are no events and no newly unassigned variables, skip the whole thing as well.
            if (!cascadingUpdateShadowVarDescriptorList.isEmpty() &&
                    !(listVariableChangeList.isEmpty() && unassignedValueWithEmptyInverseEntitySet.isEmpty())) {
                triggerCascadingUpdateShadowVariableUpdate();
            }
            listVariableChangeList.clear();
        }
        if (shadowVariableSession != null) {
            shadowVariableSession.updateVariables();
        }
        dirty = false;
    }

    /**
     * Triggers all cascading update shadow variable user-logic.
     */
    private void triggerCascadingUpdateShadowVariableUpdate() {
        if (listVariableChangeList.isEmpty() || cascadingUpdateShadowVarDescriptorList.isEmpty()) {
            return;
        }
        for (var cascadingUpdateShadowVariableDescriptor : cascadingUpdateShadowVarDescriptorList) {
            cascadeListVariableChangedNotifications(cascadingUpdateShadowVariableDescriptor);
            // When the unassigned element has no inverse entity,
            // it indicates that it is not reverting to a previous entity.
            // In this case, we need to invoke the cascading logic,
            // or its related shadow variables will remain unchanged.
            cascadeUnassignedValues(cascadingUpdateShadowVariableDescriptor);
        }
        unassignedValueWithEmptyInverseEntitySet.clear();
    }

    private void cascadeListVariableChangedNotifications(
            CascadingUpdateShadowVariableDescriptor<Solution_> cascadingUpdateShadowVariableDescriptor) {
        for (var change : listVariableChangeList) {
            cascadeListVariableValueUpdates(
                    listVariableDescriptor.getValue(change.entity()),
                    change.fromIndex(), change.toIndex(),
                    cascadingUpdateShadowVariableDescriptor);
        }
    }

    private void cascadeListVariableValueUpdates(List<Object> values, int fromIndex, int toIndex,
            CascadingUpdateShadowVariableDescriptor<Solution_> cascadingUpdateShadowVariableDescriptor) {
        for (var currentIndex = fromIndex; currentIndex < values.size(); currentIndex++) {
            var value = values.get(currentIndex);
            // The value is present in the unassigned values,
            // but the cascade logic is triggered by a list event.
            // So, we can remove it from the unassigned list
            // since the entity will be reverted to a previous entity.
            unassignedValueWithEmptyInverseEntitySet.remove(value);
            // Force updates within the range.
            // Outside the range, only update while the values keep changing.
            var forceUpdate = currentIndex < toIndex;
            if (!cascadingUpdateShadowVariableDescriptor.update(scoreDirector, value) && !forceUpdate) {
                break;
            }
        }
    }

    private void cascadeUnassignedValues(
            CascadingUpdateShadowVariableDescriptor<Solution_> cascadingUpdateShadowVariableDescriptor) {
        for (var unassignedValue : unassignedValueWithEmptyInverseEntitySet) {
            cascadingUpdateShadowVariableDescriptor.update(scoreDirector, unassignedValue);
        }
    }

    /**
     * @return null if there are no violations
     */
    public @Nullable String createShadowVariablesViolationMessage() {
        var workingSolution = scoreDirector.getWorkingSolution();
        var snapshot =
                ShadowVariablesAssert.takeSnapshot(scoreDirector.getSolutionDescriptor(), workingSolution);

        forceUpdateAllShadowVariables(workingSolution);
        return snapshot.createShadowVariablesViolationMessage(SHADOW_VARIABLE_VIOLATION_DISPLAY_LIMIT);
    }

    /**
     * Updates all shadow variables even when no change is pending.
     *
     * <p>
     * To ensure each listener is triggered,
     * an artificial notification is created for each genuine variable without doing any change on the working solution.
     * If everything works correctly,
     * triggering listeners at this point must not change any shadow variables either.
     *
     * @param workingSolution working solution
     */
    public void forceUpdateAllShadowVariables(Solution_ workingSolution) {
        scoreDirector.getSolutionDescriptor().visitAllEntities(workingSolution, this::simulateGenuineVariableChange);
        updateShadowVariables();
    }

    /**
     * Discards pending shadow variable updates without applying them.
     * The goal is to clear all queues and avoid executing custom listener logic.
     */
    public void clearPendingShadowVariableUpdates() {
        dirty = false;
    }

    private void simulateGenuineVariableChange(Object entity) {
        var entityDescriptor = scoreDirector.getSolutionDescriptor()
                .findEntityDescriptorOrFail(entity.getClass());
        if (!entityDescriptor.isGenuine()) {
            return;
        }
        for (var variableDescriptor : entityDescriptor.getGenuineVariableDescriptorList()) {
            if (variableDescriptor.isListVariable()) {
                var descriptor = (ListVariableDescriptor<Solution_>) variableDescriptor;
                var size = descriptor.getValue(entity).size();
                beforeListVariableChanged(descriptor, entity, 0, size);
                afterListVariableChanged(descriptor, entity, 0, size);
            } else {
                beforeVariableChanged(variableDescriptor, entity);
                afterVariableChanged(variableDescriptor, entity);
            }
        }
    }

    public void assertShadowVariablesAreUpToDate() {
        if (dirty) {
            throw new IllegalStateException(
                    """
                            The shadow variables might be stale (%s) so score calculation is unreliable.
                            Maybe a %s.before*() method was called without calling %s.updateShadowVariables(), before calling %s.calculateScore()."""
                            .formatted(dirty, ScoreDirector.class.getSimpleName(),
                                    ScoreDirector.class.getSimpleName(), ScoreDirector.class.getSimpleName()));
        }
    }

    private record ListVariableChange(Object entity, int fromIndex, int toIndex) {
    }

    private record SupplyWithDemandCount(Supply supply, long demandCount) {
    }

}
