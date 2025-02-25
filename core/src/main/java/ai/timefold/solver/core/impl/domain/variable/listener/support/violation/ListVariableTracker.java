package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NonNull;

/**
 * Tracks variable listener events for a given {@link ai.timefold.solver.core.api.domain.variable.PlanningListVariable}.
 */
public class ListVariableTracker<Solution_>
        implements SourcedVariableListener<Solution_>, ListVariableListener<Solution_, Object, Object>, Supply {
    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Map<Object, SortedSet<ChangeRange>> beforeVariableChangeEventMap;
    private final Map<Object, SortedSet<ChangeRange>> afterVariableChangeEventMap;
    private final Set<Object> afterUnassignedEvents;

    private record ChangeRange(int start, int end) implements Comparable<ChangeRange> {
        @Override
        public int compareTo(@NonNull ChangeRange other) {
            return Comparator.comparingInt(ChangeRange::end)
                    .thenComparing(ChangeRange::start)
                    .reversed()
                    .compare(this, other);
        }
    }

    public ListVariableTracker(ListVariableDescriptor<Solution_> variableDescriptor) {
        this.variableDescriptor = variableDescriptor;
        beforeVariableChangeEventMap = new IdentityHashMap<>();
        afterVariableChangeEventMap = new IdentityHashMap<>();
        afterUnassignedEvents = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    @Override
    public VariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return variableDescriptor;
    }

    @Override
    public void resetWorkingSolution(@NonNull ScoreDirector<Solution_> scoreDirector) {
        beforeVariableChangeEventMap.clear();
        afterVariableChangeEventMap.clear();
        afterUnassignedEvents.clear();
    }

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {

    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {

    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {

    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {

    }

    @Override
    public void afterListVariableElementUnassigned(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object element) {
        afterUnassignedEvents.add(element);
    }

    @Override
    public void beforeListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity,
            int fromIndex, int toIndex) {
        beforeVariableChangeEventMap.computeIfAbsent(entity, k -> new TreeSet<>())
                .add(new ChangeRange(fromIndex, toIndex));
    }

    @Override
    public void afterListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity, int fromIndex,
            int toIndex) {
        afterVariableChangeEventMap.computeIfAbsent(entity, k -> new TreeSet<>())
                .add(new ChangeRange(fromIndex, toIndex));
    }

    public List<String> getEntitiesMissingBeforeAfterEvents(
            List<VariableId<Solution_>> changedVariables,
            VariableSnapshotTotal<Solution_> beforeSolution,
            VariableSnapshotTotal<Solution_> afterSolution) {
        List<String> out = new ArrayList<>();
        Set<Object> allBeforeValues = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> allAfterValues = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var changedVariable : changedVariables) {
            if (!variableDescriptor.equals(changedVariable.variableDescriptor())) {
                continue;
            }
            Object entity = changedVariable.entity();

            if (!beforeVariableChangeEventMap.containsKey(entity)) {
                out.add("Entity (" + entity
                        + ") is missing a beforeListVariableChanged call for list variable ("
                        + variableDescriptor.getVariableName() + ").");
            }
            if (!afterVariableChangeEventMap.containsKey(entity)) {
                out.add("Entity (" + entity
                        + ") is missing a afterListVariableChanged call for list variable ("
                        + variableDescriptor.getVariableName() + ").");
            }

            List<Object> beforeList =
                    new ArrayList<>((List<Object>) beforeSolution.getVariableSnapshot(changedVariable).value());

            List<Object> afterList = new ArrayList<>((List<Object>) afterSolution.getVariableSnapshot(changedVariable).value());

            allBeforeValues.addAll(beforeList);
            allAfterValues.addAll(afterList);
        }

        var unassignedValues = Collections.newSetFromMap(new IdentityHashMap<>());
        unassignedValues.addAll(allBeforeValues);
        unassignedValues.removeAll(allAfterValues);

        for (var unassignedValue : unassignedValues) {
            if (!afterUnassignedEvents.contains(unassignedValue)) {
                out.add("Missing afterListElementUnassigned: " + unassignedValue);
            }
        }

        beforeVariableChangeEventMap.clear();
        afterVariableChangeEventMap.clear();
        afterUnassignedEvents.clear();
        return out;
    }

    public TrackerDemand demand() {
        return new TrackerDemand();
    }

    /**
     * In order for the {@link ListVariableTracker} to be registered as a variable listener,
     * it needs to be passed to the {@link InnerScoreDirector#getSupplyManager()}, which requires a {@link Demand}.
     * <p>
     * Unlike most other {@link Demand}s, there will only be one instance of
     * {@link ListVariableTracker} in the {@link InnerScoreDirector} for each list variable.
     */
    public class TrackerDemand implements Demand<ListVariableTracker<Solution_>> {
        @Override
        public ListVariableTracker<Solution_> createExternalizedSupply(SupplyManager supplyManager) {
            return ListVariableTracker.this;
        }
    }
}
