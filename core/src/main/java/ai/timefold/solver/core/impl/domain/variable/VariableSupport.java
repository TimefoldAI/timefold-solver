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
import java.util.function.Predicate;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.VariableLoop;
import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.cascade.CascadingUpdateShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.ConsistencyTracker;
import ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultShadowVariableSession;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultShadowVariableSessionFactory;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultTopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.ShadowVariablesInconsistentVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.TopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.VariableReferenceGraph.VariableChangeHook;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.domain.variable.violation.BasicVariableTracker;
import ai.timefold.solver.core.impl.domain.variable.violation.ListVariableTracker;
import ai.timefold.solver.core.impl.domain.variable.violation.ShadowVariablesAssert;
import ai.timefold.solver.core.impl.domain.variable.violation.TrackerResolver;
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
public final class VariableSupport<Solution_> implements TrackerResolver<Solution_>, SupplyManager {

    public static <Solution_> VariableSupport<Solution_> create(InnerScoreDirector<Solution_, ?> scoreDirector) {
        return new VariableSupport<>(scoreDirector,
                TimefoldSolverEnterpriseService.loadOrDefault(
                        service -> size -> service.buildTopologyGraph(size,
                                scoreDirector.ignoreInconsistentSolutions()),
                        () -> DefaultTopologicalOrderGraph::new));
    }

    private static final int SHADOW_VARIABLE_VIOLATION_DISPLAY_LIMIT = 3;
    private final InnerScoreDirector<Solution_, ?> scoreDirector;
    private final Map<Demand<?>, SupplyWithDemandCount> supplyMap = new HashMap<>();

    /**
     * Everything a change of a basic or shadow variable is dispatched to, resolved once per variable.
     * Indexed by [{@link EntityDescriptor#getOrdinal()}][{@link VariableDescriptor#getOrdinal()}].
     */
    private final VariableDispatch<Solution_>[][] variableDispatchArray;
    private final @Nullable ListVariableDescriptor<Solution_> listVariableDescriptor;
    /**
     * The single source of truth for the list variable state, created at first request.
     */
    private @Nullable ListVariableState<Solution_, ?, ?> listVariableState;
    /**
     * The single source of truth for the list variable tracker, created at first request.
     */
    private @Nullable ListVariableTracker<Solution_> listVariableTracker;
    /**
     * The current list of variable change handlers
     */
    private final List<ListVariableChangeHandler<Solution_>> listVariableChangeHandlerList;

    private final List<ListVariableChange> listVariableChangeList;
    private final Set<Object> unassignedValueWithEmptyInverseEntitySet;
    private final List<CascadingUpdateShadowVariableDescriptor<Solution_>> cascadingUpdateShadowVarDescriptorList;
    private final IntFunction<TopologicalOrderGraph> shadowVariableGraphCreator;

    private boolean dirty = false;
    private boolean isUpdatingDeclarativeShadows = false;
    private boolean updateSuccessful = true;
    // Pending graph marks without a hooked event (e.g. after a reset) wait for the next event of any kind.
    private boolean eventSinceUpdate = false;
    @Nullable
    private DefaultShadowVariableSession<Solution_> shadowVariableSession = null;
    private ConsistencyTracker<Solution_> consistencyTracker = new ConsistencyTracker<>();

    @SuppressWarnings("unchecked")
    VariableSupport(InnerScoreDirector<Solution_, ?> scoreDirector,
            IntFunction<TopologicalOrderGraph> shadowVariableGraphCreator) {
        this.scoreDirector = Objects.requireNonNull(scoreDirector);

        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptorList = solutionDescriptor.getEntityDescriptors();
        this.variableDispatchArray = new VariableDispatch[entityDescriptorList.size()][];
        for (var entityDescriptor : entityDescriptorList) {
            var declaredVariableDescriptorList = entityDescriptor.getDeclaredVariableDescriptors();
            var dispatchArray = new VariableDispatch[declaredVariableDescriptorList.size()];
            for (var variableDescriptor : declaredVariableDescriptorList) {
                dispatchArray[variableDescriptor.getOrdinal()] = new VariableDispatch<>(variableDescriptor);
            }
            variableDispatchArray[entityDescriptor.getOrdinal()] = dispatchArray;
        }

        // Fields specific to list variable; will be ignored if not necessary.
        this.listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        this.listVariableChangeHandlerList = listVariableDescriptor == null ? Collections.emptyList() : new ArrayList<>();
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
        if (listVariableDescriptor != null) {
            getListVariableState(listVariableDescriptor, false);
        }
        scoreDirector.getSolutionDescriptor().getEntityDescriptors().stream()
                .map(EntityDescriptor::getDeclaredShadowVariableDescriptors)
                .flatMap(Collection::stream)
                .forEach(this::linkShadowVariable);
    }

    /**
     * Connects list-related shadow variables to the centralized list state; others are processed normally.
     * Cascading, declarative, and inconsistent shadow variables are routed elsewhere.
     */
    private void linkShadowVariable(ShadowVariableDescriptor<Solution_> descriptor) {
        var currentListVariableState = getListVariableState(listVariableDescriptor, false);
        if (descriptor instanceof InverseRelationShadowVariableDescriptor<Solution_> inverseRelationShadowVariableDescriptor) {
            if (inverseRelationShadowVariableDescriptor.getSourceVariableDescriptor() instanceof ListVariableDescriptor<?>) {
                if (currentListVariableState != null) {
                    processShadowVariableDescriptorWithListVariable(inverseRelationShadowVariableDescriptor,
                            currentListVariableState);
                }
            } else {
                var basicVariableState = getBasicVariableState(
                        Objects.requireNonNull(inverseRelationShadowVariableDescriptor.getSourceVariableDescriptor()), false);
                basicVariableState.externalize(inverseRelationShadowVariableDescriptor);
            }
        } else if (currentListVariableState != null) {
            switch (descriptor) {
                // When multiple variable types are used,
                // the shadow variable process needs to account for each variable
                // and process them according to their types.
                case IndexShadowVariableDescriptor<Solution_> d ->
                    processShadowVariableDescriptorWithListVariable(d, currentListVariableState);
                case PreviousElementShadowVariableDescriptor<Solution_> d ->
                    processShadowVariableDescriptorWithListVariable(d, currentListVariableState);
                case NextElementShadowVariableDescriptor<Solution_> d ->
                    processShadowVariableDescriptorWithListVariable(d, currentListVariableState);
                case DeclarativeShadowVariableDescriptor<Solution_> ignored -> {
                    // Needs no handling here.
                }
                case ShadowVariablesInconsistentVariableDescriptor<Solution_> ignored -> {
                    // Needs no handling here.
                }
                case CascadingUpdateShadowVariableDescriptor<Solution_> ignored -> {
                    // Needs no handling here.
                }
                // Fail-safe.
                default -> throw new IllegalStateException("Impossible state: unknown shadow variable type (%s)."
                        .formatted(descriptor));
            }
        }
    }

    private void processShadowVariableDescriptorWithListVariable(ShadowVariableDescriptor<Solution_> shadowVariableDescriptor,
            ListVariableState<Solution_, Object, Object> listVariableState) {
        switch (shadowVariableDescriptor) {
            case IndexShadowVariableDescriptor<Solution_> indexShadowVariableDescriptor ->
                listVariableState.externalize(indexShadowVariableDescriptor);
            case InverseRelationShadowVariableDescriptor<Solution_> inverseRelationShadowVariableDescriptor ->
                listVariableState.externalize(inverseRelationShadowVariableDescriptor);
            case PreviousElementShadowVariableDescriptor<Solution_> previousElementShadowVariableDescriptor ->
                listVariableState.externalize(previousElementShadowVariableDescriptor);
            case NextElementShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor ->
                listVariableState.externalize(nextElementShadowVariableDescriptor);
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
            var newSupplyWithDemandCount = new SupplyWithDemandCount(demand.createExternalizedSupply(this), 1L);
            supplyMap.put(demand, newSupplyWithDemandCount);
            return (Supply_) newSupplyWithDemandCount.supply;
        } else {
            var supply = supplyWithDemandCount.supply;
            var newSupplyWithDemandCount = new SupplyWithDemandCount(supply, supplyWithDemandCount.demandCount + 1L);
            supplyMap.put(demand, newSupplyWithDemandCount);
            return (Supply_) supply;
        }
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
    // List variable methods
    // ************************************************************************

    public @Nullable <Entity_, Value_> ListVariableState<Solution_, Entity_, Value_>
            getListVariableState(@Nullable ListVariableDescriptor<Solution_> targetVariableDescriptor) {
        return getListVariableState(targetVariableDescriptor, true);
    }

    @SuppressWarnings("unchecked")
    private @Nullable <Entity_, Value_> ListVariableState<Solution_, Entity_, Value_>
            getListVariableState(@Nullable ListVariableDescriptor<Solution_> targetVariableDescriptor, boolean reset) {
        if (targetVariableDescriptor != listVariableDescriptor) {
            throw new IllegalStateException(
                    "The variableDescriptor (%s) is not the same as the solution's variableDescriptor (%s)."
                            .formatted(targetVariableDescriptor, listVariableDescriptor));
        }
        if (targetVariableDescriptor == null) {
            return null;
        }
        if (listVariableState == null) { // The list state has not been loaded yet.
            listVariableState = new DefaultListVariableState<>(targetVariableDescriptor, getStateChangeNotifier());
            registerListVariableHandler(listVariableState, reset);
        }
        return (ListVariableState<Solution_, Entity_, Value_>) listVariableState;
    }

    @Override
    public @Nullable ListVariableTracker<Solution_>
            getListVariableTracker(@Nullable ListVariableDescriptor<Solution_> targetVariableDescriptor) {
        if (targetVariableDescriptor != listVariableDescriptor) {
            throw new IllegalStateException(
                    "The variableDescriptor (%s) is not the same as the solution's variableDescriptor (%s)."
                            .formatted(targetVariableDescriptor, listVariableDescriptor));
        }
        if (targetVariableDescriptor == null) {
            return null;
        }
        if (listVariableTracker == null) {
            listVariableTracker = new ListVariableTracker<>(listVariableDescriptor);
            registerListVariableHandler(listVariableTracker, true);
        }
        return listVariableTracker;
    }

    private void registerListVariableHandler(ListVariableChangeHandler<Solution_> handler, boolean reset) {
        if (reset) {
            resetWorkingSolutionIfSet(() -> handler.resetWorkingSolution(scoreDirector));
        }
        listVariableChangeHandlerList.add(handler);
    }

    // ************************************************************************
    // Basic variable methods
    // ************************************************************************

    public BasicVariableState<Solution_> getBasicVariableState(VariableDescriptor<Solution_> variableDescriptor) {
        return getBasicVariableState(variableDescriptor, true);
    }

    private BasicVariableState<Solution_> getBasicVariableState(VariableDescriptor<Solution_> variableDescriptor,
            boolean reset) {
        BasicVariableState<Solution_> state =
                findBasicHandler(variableDescriptor, handler -> handler instanceof BasicVariableState<Solution_> handlerState
                        && handlerState.getSourceVariableDescriptor() == variableDescriptor);
        if (state != null) {
            return state;
        }
        // The state has not been loaded yet; there must only ever be one per variable,
        // as it is the single source of truth for the inverse relation of that variable.
        var basicVariableState = new BasicVariableState<>(variableDescriptor, getStateChangeNotifier());
        registerBasicVariableChangeHandler(basicVariableState, reset);
        return basicVariableState;
    }

    @Override
    public BasicVariableTracker<Solution_> getBasicVariableTracker(VariableDescriptor<Solution_> variableDescriptor) {
        BasicVariableTracker<Solution_> tracker =
                findBasicHandler(variableDescriptor, handler -> handler instanceof BasicVariableTracker<Solution_> handleTracker
                        && handleTracker.getSourceVariableDescriptor() == variableDescriptor);
        if (tracker != null) {
            return tracker;
        }
        // The tracker has not been loaded yet; there must only ever be one per variable
        var basicVariableTracker = new BasicVariableTracker<>(variableDescriptor);
        registerBasicVariableChangeHandler(basicVariableTracker, true);
        return basicVariableTracker;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <Type_ extends BasicVariableChangeHandler<Solution_>> Type_ findBasicHandler(
            VariableDescriptor<Solution_> variableDescriptor, Predicate<BasicVariableChangeHandler<Solution_>> checkFunction) {
        var handlerList = getBasicVariableChangeHandlerList(variableDescriptor);
        if (handlerList.isEmpty()) {
            return null;
        }
        var firstHandler = handlerList.getFirst();
        if (checkFunction.test(firstHandler)) {
            return (Type_) firstHandler;
        }
        if (handlerList.size() == 1) {
            return null;
        }
        var secondHandler = handlerList.get(1);
        if (checkFunction.test(secondHandler)) {
            return (Type_) secondHandler;
        }
        return null;
    }

    private VariableDispatch<Solution_> getVariableDispatch(VariableDescriptor<Solution_> variableDescriptor) {
        return variableDispatchArray[variableDescriptor.getEntityDescriptor().getOrdinal()][variableDescriptor.getOrdinal()];
    }

    private List<BasicVariableChangeHandler<Solution_>>
            getBasicVariableChangeHandlerList(VariableDescriptor<Solution_> variableDescriptor) {
        return getVariableDispatch(variableDescriptor).handlerList;
    }

    private void registerBasicVariableChangeHandler(BasicVariableChangeHandler<Solution_> handler, boolean reset) {
        if (reset) {
            resetWorkingSolutionIfSet(() -> handler.resetWorkingSolution(scoreDirector));
        }
        var variableDescriptor = handler.getSourceVariableDescriptor();
        var handlerList = getBasicVariableChangeHandlerList(variableDescriptor);
        if (handlerList.size() >= 2) {
            throw new IllegalStateException(
                    "Impossible state: a basic variable cannot have more than two handlers assigned to it.");
        }
        handlerList.add(handler);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    private void resetWorkingSolutionIfSet(Runnable resetWorkingSolution) {
        // An external ScoreDirector can be created before the working solution is set.
        if (scoreDirector.getWorkingSolution() != null) {
            resetWorkingSolution.run();
        }
    }

    public void resetWorkingSolution() {
        for (var handler : listVariableChangeHandlerList) {
            handler.resetWorkingSolution(scoreDirector);
        }
        for (var dispatchArray : variableDispatchArray) {
            for (var dispatch : dispatchArray) {
                for (var handler : dispatch.handlerList) {
                    handler.resetWorkingSolution(scoreDirector);
                }
            }
        }

        if (!scoreDirector.getSolutionDescriptor().getDeclarativeShadowVariableDescriptors().isEmpty()
                && !consistencyTracker.isFrozen()) {
            for (var dispatchArray : variableDispatchArray) {
                for (var dispatch : dispatchArray) {
                    dispatch.graphHook = null; // Events sent while the new graph is built must not reach the old one.
                }
            }
            var shadowVariableSessionFactory = new DefaultShadowVariableSessionFactory<>(
                    scoreDirector.getSolutionDescriptor(),
                    scoreDirector,
                    shadowVariableGraphCreator);
            var session = shadowVariableSessionFactory.forSolution(consistencyTracker,
                    scoreDirector.getWorkingSolution(),
                    scoreDirector.ignoreInconsistentSolutions());
            shadowVariableSession = session;
            for (var dispatchArray : variableDispatchArray) {
                for (var dispatch : dispatchArray) {
                    dispatch.graphHook = session.resolveHookFor(dispatch.variableDescriptor);
                }
            }
        }
    }

    public void close() {
        for (var handler : listVariableChangeHandlerList) {
            handler.close();
        }
        for (var dispatchArray : variableDispatchArray) {
            for (var dispatch : dispatchArray) {
                for (var handler : dispatch.handlerList) {
                    handler.close();
                }
            }
        }
    }

    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        var dispatch = getVariableDispatch(variableDescriptor);
        var handlerList = dispatch.handlerList;
        for (var i = 0; i < handlerList.size(); i++) { // Avoid iterator allocations on the hot path.
            var handler = handlerList.get(i);
            handler.beforeVariableChanged(scoreDirector, entity);
        }
        eventSinceUpdate |= shadowVariableSession != null;
        var graphHook = dispatch.graphHook;
        if (graphHook != null) {
            graphHook.beforeVariableChanged(entity);
            dirty = true;
        }
    }

    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        var dispatch = getVariableDispatch(variableDescriptor);
        var handlerList = dispatch.handlerList;
        for (var i = 0; i < handlerList.size(); i++) { // Avoid iterator allocations on the hot path.
            var handler = handlerList.get(i);
            handler.afterVariableChanged(scoreDirector, entity);
        }
        eventSinceUpdate |= shadowVariableSession != null;
        var graphHook = dispatch.graphHook;
        if (graphHook != null) {
            graphHook.afterVariableChanged(entity);
            dirty = true;
        }
    }

    public void afterElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        for (var handler : listVariableChangeHandlerList) {
            handler.afterListElementUnassigned(scoreDirector, element);
        }
        if (!cascadingUpdateShadowVarDescriptorList.isEmpty()) { // Only necessary if there is a cascade.
            unassignedValueWithEmptyInverseEntitySet.add(element);
            dirty = true;
        }
        if (shadowVariableSession != null) { // Defensive; an unassignment may change declarative inputs.
            dirty = true;
        }
    }

    public void beforeListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        assertNotUpdatingDeclarativeShadows();
        for (var i = 0; i < listVariableChangeHandlerList.size(); i++) { // Avoid iterator allocations on the hot path.
            var handler = listVariableChangeHandlerList.get(i);
            handler.beforeListVariableChanged(scoreDirector, entity, fromIndex, toIndex);
        }
        eventSinceUpdate |= shadowVariableSession != null;
        var graphHook = getVariableDispatch(variableDescriptor).graphHook;
        if (graphHook != null) {
            graphHook.beforeListVariableChanged(entity, variableDescriptor.getValue(entity), fromIndex, toIndex);
            dirty = true;
        }
    }

    public void afterListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        assertNotUpdatingDeclarativeShadows();
        for (var i = 0; i < listVariableChangeHandlerList.size(); i++) { // Avoid iterator allocations on the hot path.
            var handler = listVariableChangeHandlerList.get(i);
            handler.afterListVariableChanged(scoreDirector, entity, fromIndex, toIndex);
        }
        eventSinceUpdate |= shadowVariableSession != null;
        if (!cascadingUpdateShadowVarDescriptorList.isEmpty()) { // Only necessary if there is a cascade.
            listVariableChangeList.add(new ListVariableChange(entity, fromIndex, toIndex));
            dirty = true;
        }
        var graphHook = getVariableDispatch(variableDescriptor).graphHook;
        if (graphHook != null) {
            dirty = true;
            graphHook.afterListVariableChanged(entity, variableDescriptor.getValue(entity), fromIndex, toIndex);
        }
    }

    private void assertNotUpdatingDeclarativeShadows() {
        if (isUpdatingDeclarativeShadows) { // Declarative shadow variable updates never change a list variable.
            throw new IllegalStateException("Impossible state: list variable changed during shadow variable update.");
        }
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> InnerScoreDirector<Solution_, Score_> getScoreDirector() {
        return (InnerScoreDirector<Solution_, Score_>) scoreDirector;
    }

    public boolean updateShadowVariables() {
        if (isCurrent()) {
            // Shortcut in case the trigger is called multiple times in a row,
            // without any notifications inbetween.
            // This is better than trying to ensure that the situation never ever occurs.
            return updateSuccessful;
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
            isUpdatingDeclarativeShadows = true;
            var success = shadowVariableSession.updateVariables();
            isUpdatingDeclarativeShadows = false;
            if (!success) {
                updateSuccessful = false;
                return false;
            }
        }
        dirty = false;
        eventSinceUpdate = false;
        updateSuccessful = true;
        return true;
    }

    private boolean isCurrent() {
        if (dirty) {
            return false;
        }
        return !eventSinceUpdate || shadowVariableSession == null || !shadowVariableSession.hasPendingChanges();
    }

    public List<VariableLoop> getVariableLoops() {
        if (shadowVariableSession == null) {
            return Collections.emptyList();
        }
        return shadowVariableSession.getVariableLoops();
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
    public boolean forceUpdateAllShadowVariables(Solution_ workingSolution) {
        scoreDirector.getSolutionDescriptor().visitAllEntities(workingSolution, this::simulateGenuineVariableChange);
        return updateShadowVariables();
    }

    /**
     * Marks shadow variables as up to date without applying pending updates or running custom listeners.
     * Pending graph marks stay and are applied after the next event.
     * Only ever triggered by {@link ConstraintVerifier}.
     */
    public void clearPendingShadowVariableUpdates() {
        dirty = false;
        eventSinceUpdate = false;
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
        if (isCurrent()) {
            return;
        }
        var scoreDirectorSimpleClassName = ScoreDirector.class.getSimpleName();
        throw new IllegalStateException("""
                The shadow variables might be stale, so score calculation is unreliable.
                Maybe a %s.before*() method was called without calling %s.updateShadowVariables(), \
                before calling %s.calculateScore()."""
                .formatted(scoreDirectorSimpleClassName, scoreDirectorSimpleClassName, scoreDirectorSimpleClassName));
    }

    private record ListVariableChange(Object entity, int fromIndex, int toIndex) {
    }

    private record SupplyWithDemandCount(Supply supply, long demandCount) {
    }

    private static final class VariableDispatch<Solution_> {

        private final VariableDescriptor<Solution_> variableDescriptor;
        private final List<BasicVariableChangeHandler<Solution_>> handlerList = new ArrayList<>(2);
        /**
         * Null if there is no session, or if the session does not react to this variable.
         */
        private @Nullable VariableChangeHook graphHook = null;

        private VariableDispatch(VariableDescriptor<Solution_> variableDescriptor) {
            this.variableDescriptor = variableDescriptor;
        }

    }

}
