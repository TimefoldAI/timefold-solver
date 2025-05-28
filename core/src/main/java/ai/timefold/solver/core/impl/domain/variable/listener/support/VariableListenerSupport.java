package ai.timefold.solver.core.impl.domain.variable.listener.support;

import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.BASIC;
import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.CASCADING_UPDATE;
import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.CUSTOM_LISTENER;
import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.DECLARATIVE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.cascade.CascadingUpdateShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultShadowVariableSession;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultShadowVariableSessionFactory;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultTopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.TopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;
import ai.timefold.solver.core.impl.domain.variable.listener.support.violation.ShadowVariablesAssert;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.util.LinkedIdentityHashSet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This class is not thread-safe.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public final class VariableListenerSupport<Solution_> implements SupplyManager {

    public static <Solution_> VariableListenerSupport<Solution_> create(InnerScoreDirector<Solution_, ?> scoreDirector) {
        return new VariableListenerSupport<>(scoreDirector, new NotifiableRegistry<>(scoreDirector.getSolutionDescriptor()),
                TimefoldSolverEnterpriseService.buildOrDefault(service -> service::buildTopologyGraph,
                        () -> DefaultTopologicalOrderGraph::new));
    }

    private static final int SHADOW_VARIABLE_VIOLATION_DISPLAY_LIMIT = 3;
    private final InnerScoreDirector<Solution_, ?> scoreDirector;
    private final NotifiableRegistry<Solution_> notifiableRegistry;
    private final Map<Demand<?>, SupplyWithDemandCount> supplyMap = new HashMap<>();

    private final @Nullable ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final List<ListVariableChangedNotification<Solution_>> listVariableChangedNotificationList;
    private final Set<Object> unassignedValueWithEmptyInverseEntitySet;
    private final List<CascadingUpdateShadowVariableDescriptor<Solution_>> cascadingUpdateShadowVarDescriptorList;
    @NonNull
    private final IntFunction<TopologicalOrderGraph> shadowVariableGraphCreator;

    private boolean notificationQueuesAreEmpty = true;
    private int nextGlobalOrder = 0;
    @Nullable
    private DefaultShadowVariableSession<Solution_> shadowVariableSession = null;
    @Nullable
    private ListVariableStateSupply<Solution_> listVariableStateSupply = null;
    private final List<ShadowVariableType> supportedShadowVariableTypeList;

    VariableListenerSupport(InnerScoreDirector<Solution_, ?> scoreDirector, NotifiableRegistry<Solution_> notifiableRegistry,
            @NonNull IntFunction<TopologicalOrderGraph> shadowVariableGraphCreator) {
        this.scoreDirector = Objects.requireNonNull(scoreDirector);
        this.notifiableRegistry = Objects.requireNonNull(notifiableRegistry);

        // Fields specific to list variable; will be ignored if not necessary.
        this.listVariableDescriptor = scoreDirector.getSolutionDescriptor().getListVariableDescriptor();
        this.cascadingUpdateShadowVarDescriptorList =
                listVariableDescriptor != null ? scoreDirector.getSolutionDescriptor().getEntityDescriptors().stream()
                        .flatMap(e -> e.getDeclaredCascadingUpdateShadowVariableDescriptors().stream())
                        .toList() : Collections.emptyList();
        var hasCascadingUpdates = !cascadingUpdateShadowVarDescriptorList.isEmpty();
        this.listVariableChangedNotificationList = new ArrayList<>();
        this.unassignedValueWithEmptyInverseEntitySet =
                hasCascadingUpdates ? new LinkedIdentityHashSet<>() : Collections.emptySet();
        this.shadowVariableGraphCreator = shadowVariableGraphCreator;
        // Existing dependencies rely on this list
        // to ensure consistency in supporting all available shadow variable types
        // See ShadowVariableUpdateHelper
        this.supportedShadowVariableTypeList = List.of(BASIC, CUSTOM_LISTENER, CASCADING_UPDATE, DECLARATIVE);
    }

    public List<ShadowVariableType> getSupportedShadowVariableTypes() {
        return supportedShadowVariableTypeList;
    }

    public void linkVariableListeners() {
        listVariableStateSupply = listVariableDescriptor == null ? null : demand(listVariableDescriptor.getStateDemand());
        scoreDirector.getSolutionDescriptor().getEntityDescriptors().stream()
                .map(EntityDescriptor::getDeclaredShadowVariableDescriptors)
                .flatMap(Collection::stream)
                .filter(ShadowVariableDescriptor::hasVariableListener)
                .sorted(Comparator.comparingInt(ShadowVariableDescriptor::getGlobalShadowOrder))
                .forEach(descriptor -> {
                    // All information about elements in all shadow variables is tracked in a centralized place.
                    // Therefore, all list-related shadow variables need to be connected to that centralized place.
                    // Shadow variables which are not related to a list variable are processed normally.
                    if (listVariableStateSupply == null) {
                        processShadowVariableDescriptorWithoutListVariable(descriptor);
                    } else {
                        // When multiple variable types are used,
                        // the shadow variable process needs to account for each variable
                        // and process them according to their types.
                        if (descriptor.isListVariableSource()) {
                            processShadowVariableDescriptorWithListVariable(descriptor, listVariableStateSupply);
                        } else {
                            processShadowVariableDescriptorWithoutListVariable(descriptor);
                        }
                    }
                });
    }

    private void processShadowVariableDescriptorWithListVariable(ShadowVariableDescriptor<Solution_> shadowVariableDescriptor,
            ListVariableStateSupply<Solution_> listVariableStateSupply) {
        if (shadowVariableDescriptor instanceof IndexShadowVariableDescriptor<Solution_> indexShadowVariableDescriptor) {
            listVariableStateSupply.externalize(indexShadowVariableDescriptor);
        } else if (shadowVariableDescriptor instanceof InverseRelationShadowVariableDescriptor<Solution_> inverseRelationShadowVariableDescriptor) {
            listVariableStateSupply.externalize(inverseRelationShadowVariableDescriptor);
        } else if (shadowVariableDescriptor instanceof PreviousElementShadowVariableDescriptor<Solution_> previousElementShadowVariableDescriptor) {
            listVariableStateSupply.externalize(previousElementShadowVariableDescriptor);
        } else if (shadowVariableDescriptor instanceof NextElementShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor) {
            listVariableStateSupply.externalize(nextElementShadowVariableDescriptor);
        } else { // The list variable supply supports no other shadow variables.
            processShadowVariableDescriptorWithoutListVariable(shadowVariableDescriptor);
        }
    }

    private void
            processShadowVariableDescriptorWithoutListVariable(ShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        for (var listenerWithSources : shadowVariableDescriptor.buildVariableListeners(this)) {
            var variableListener = listenerWithSources.getVariableListener();
            if (variableListener instanceof Supply supply) {
                // Non-sourced variable listeners (ie. ones provided by the user) can never be a supply.
                var demand = shadowVariableDescriptor.getProvidedDemand();
                supplyMap.put(demand, new SupplyWithDemandCount(supply, 1L));
            }
            var globalOrder = shadowVariableDescriptor.getGlobalShadowOrder();
            notifiableRegistry.registerNotifiable(
                    listenerWithSources.getSourceVariableDescriptors(),
                    AbstractNotifiable.buildNotifiable(scoreDirector, variableListener, globalOrder));
            nextGlobalOrder = globalOrder + 1;
        }
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
        if (supply instanceof SourcedVariableListener) {
            var variableListener = (SourcedVariableListener<Solution_>) supply;
            // An external ScoreDirector can be created before the working solution is set
            if (scoreDirector.getWorkingSolution() != null) {
                variableListener.resetWorkingSolution(scoreDirector);
            }
            notifiableRegistry.registerNotifiable(
                    variableListener.getSourceVariableDescriptor(),
                    AbstractNotifiable.buildNotifiable(scoreDirector, variableListener, nextGlobalOrder++));
        }
        return supply;
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

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public void resetWorkingSolution() {
        for (var notifiable : notifiableRegistry.getAll()) {
            notifiable.resetWorkingSolution();
        }

        if (!scoreDirector.getSolutionDescriptor().getDeclarativeShadowVariableDescriptors().isEmpty()) {
            var shadowVariableSessionFactory = new DefaultShadowVariableSessionFactory<>(
                    scoreDirector.getSolutionDescriptor(),
                    scoreDirector,
                    shadowVariableGraphCreator);
            shadowVariableSession = shadowVariableSessionFactory.forSolution(scoreDirector.getWorkingSolution());
            triggerVariableListenersInNotificationQueues();
        }
    }

    public void close() {
        for (var notifiable : notifiableRegistry.getAll()) {
            notifiable.closeVariableListener();
        }
    }

    public void beforeEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        var notifiables = notifiableRegistry.get(entityDescriptor);
        if (!notifiables.isEmpty()) {
            EntityNotification<Solution_> notification = Notification.entityAdded(entity);
            for (var notifiable : notifiables) {
                notifiable.notifyBefore(notification);
            }
            notificationQueuesAreEmpty = false;
        }
    }

    public void beforeEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        var notifiables = notifiableRegistry.get(entityDescriptor);
        if (!notifiables.isEmpty()) {
            EntityNotification<Solution_> notification = Notification.entityRemoved(entity);
            for (var notifiable : notifiables) {
                notifiable.notifyBefore(notification);
            }
            notificationQueuesAreEmpty = false;
        }
    }

    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        var notifiables = notifiableRegistry.get(variableDescriptor);
        if (!notifiables.isEmpty()) {
            BasicVariableNotification<Solution_> notification = Notification.variableChanged(entity);
            for (var notifiable : notifiables) {
                notifiable.notifyBefore(notification);
            }
            notificationQueuesAreEmpty = false;
        }
        if (shadowVariableSession != null) {
            shadowVariableSession.beforeVariableChanged(variableDescriptor, entity);
            notificationQueuesAreEmpty = false;
        }
    }

    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        if (shadowVariableSession != null) {
            shadowVariableSession.afterVariableChanged(variableDescriptor, entity);
        }
    }

    public void afterElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        var notifiables = notifiableRegistry.get(variableDescriptor);
        if (!notifiables.isEmpty()) {
            ListVariableNotification<Solution_> notification = Notification.elementUnassigned(element);
            for (var notifiable : notifiables) {
                notifiable.notifyAfter(notification);
            }
            notificationQueuesAreEmpty = false;
        }
        if (!cascadingUpdateShadowVarDescriptorList.isEmpty()) { // Only necessary if there is a cascade.
            unassignedValueWithEmptyInverseEntitySet.add(element);
        }
    }

    public void beforeListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        var notifiables = notifiableRegistry.get(variableDescriptor);
        if (!notifiables.isEmpty()) {
            ListVariableNotification<Solution_> notification = Notification.listVariableChanged(entity, fromIndex, toIndex);
            for (var notifiable : notifiables) {
                notifiable.notifyBefore(notification);
            }
            notificationQueuesAreEmpty = false;
        }
    }

    public void afterListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        var notifiables = notifiableRegistry.get(variableDescriptor);
        var notification = Notification.<Solution_> listVariableChanged(entity, fromIndex, toIndex);
        if (!notifiables.isEmpty()) {
            for (var notifiable : notifiables) {
                notifiable.notifyAfter(notification);
            }
            notificationQueuesAreEmpty = false;
        }
        if (!cascadingUpdateShadowVarDescriptorList.isEmpty()) { // Only necessary if there is a cascade.
            listVariableChangedNotificationList.add(notification);
        }
    }

    public InnerScoreDirector<Solution_, ?> getScoreDirector() {
        return scoreDirector;
    }

    public void triggerVariableListenersInNotificationQueues() {
        if (notificationQueuesAreEmpty) {
            // Shortcut in case the trigger is called multiple times in a row,
            // without any notifications inbetween.
            // This is better than trying to ensure that the situation never ever occurs.
            return;
        }
        for (var notifiable : notifiableRegistry.getAll()) {
            notifiable.triggerAllNotifications();
        }
        if (listVariableDescriptor != null) {
            // If there is no cascade, skip the whole thing.
            // If there are no events and no newly unassigned variables, skip the whole thing as well.
            if (!cascadingUpdateShadowVarDescriptorList.isEmpty() &&
                    !(listVariableChangedNotificationList.isEmpty() && unassignedValueWithEmptyInverseEntitySet.isEmpty())) {
                triggerCascadingUpdateShadowVariableUpdate();
            }
            listVariableChangedNotificationList.clear();
        }
        if (shadowVariableSession != null) {
            shadowVariableSession.updateVariables();
            // Some internal variable listeners (such as those used
            // to check for solution corruption) might have a declarative
            // shadow variable as a source and need to be triggered here.
            for (var notifiable : notifiableRegistry.getAll()) {
                notifiable.triggerAllNotifications();
            }
        }
        notificationQueuesAreEmpty = true;
    }

    /**
     * Triggers all cascading update shadow variable user-logic.
     */
    private void triggerCascadingUpdateShadowVariableUpdate() {
        if (listVariableChangedNotificationList.isEmpty() || cascadingUpdateShadowVarDescriptorList.isEmpty()) {
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
        for (var notification : listVariableChangedNotificationList) {
            cascadeListVariableValueUpdates(
                    listVariableDescriptor.getValue(notification.getEntity()),
                    notification.getFromIndex(), notification.getToIndex(),
                    cascadingUpdateShadowVariableDescriptor);
        }
    }

    private void cascadeListVariableValueUpdates(List<Object> values, int fromIndex, int toIndex,
            CascadingUpdateShadowVariableDescriptor<Solution_> cascadingUpdateShadowVariableDescriptor) {
        for (int currentIndex = fromIndex; currentIndex < values.size(); currentIndex++) {
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

        forceTriggerAllVariableListeners(workingSolution);
        return snapshot.createShadowVariablesViolationMessage(SHADOW_VARIABLE_VIOLATION_DISPLAY_LIMIT);
    }

    /**
     * Triggers all variable listeners even though the notification queue is empty.
     *
     * <p>
     * To ensure each listener is triggered,
     * an artificial notification is created for each genuine variable without doing any change on the working solution.
     * If everything works correctly,
     * triggering listeners at this point must not change any shadow variables either.
     *
     * @param workingSolution working solution
     */
    public void forceTriggerAllVariableListeners(Solution_ workingSolution) {
        scoreDirector.getSolutionDescriptor().visitAllEntities(workingSolution, this::simulateGenuineVariableChange);
        triggerVariableListenersInNotificationQueues();
    }

    /**
     * Clear all variable listeners without triggering any logic.
     * The goal is to clear all queues and avoid executing custom listener logic.
     */
    public void clearAllVariableListenerEvents() {
        notifiableRegistry.getAll().forEach(Notifiable::clearAllNotifications);
        notificationQueuesAreEmpty = true;
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
                // Triggering before...() is enough, as that will add the after...() call to the queue automatically.
                beforeVariableChanged(variableDescriptor, entity);
            }
        }
    }

    public void assertNotificationQueuesAreEmpty() {
        if (!notificationQueuesAreEmpty) {
            throw new IllegalStateException(
                    """
                            The notificationQueues might not be empty (%s) so any shadow variables might be stale so score calculation is unreliable.
                            Maybe a %s.before*() method was called without calling %s.triggerVariableListeners(), before calling %s.calculateScore()."""
                            .formatted(notificationQueuesAreEmpty, ScoreDirector.class.getSimpleName(),
                                    ScoreDirector.class.getSimpleName(), ScoreDirector.class.getSimpleName()));
        }
    }

    private record SupplyWithDemandCount(Supply supply, long demandCount) {
    }

}
