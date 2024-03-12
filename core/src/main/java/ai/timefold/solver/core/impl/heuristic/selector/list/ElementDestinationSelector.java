package ai.timefold.solver.core.impl.heuristic.selector.list;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector.filterPinnedListPlanningVariableValuesWithIndex;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractSelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Selects destinations for list variable change moves. The destination specifies a future position in a list variable,
 * expressed as an {@link LocationInList}, where a moved element or subList can be inserted.
 * <p>
 * Destination completeness is achieved by using both entity and value child selectors.
 * When an entity <em>A</em> is selected, the destination becomes <em>A[0]</em>.
 * When a value <em>x</em> is selected, its current position <em>A[i]</em> is determined using inverse and index supplies and
 * the destination becomes <em>A[i + 1]</em>.
 * <p>
 * Fairness in random selection is achieved by first deciding between entity and value selector with a probability that is
 * proportional to the entity/value ratio. The child entity and value selectors are assumed to be fair.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class ElementDestinationSelector<Solution_> extends AbstractSelector<Solution_>
        implements DestinationSelector<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final EntitySelector<Solution_> entitySelector;
    private final EntityIndependentValueSelector<Solution_> valueSelector;
    private final boolean randomSelection;

    private ListVariableStateSupply<Solution_> listVariableStateSupply;
    private EntityIndependentValueSelector<Solution_> movableValueSelector;

    public ElementDestinationSelector(EntitySelector<Solution_> entitySelector,
            EntityIndependentValueSelector<Solution_> valueSelector, boolean randomSelection) {
        this.listVariableDescriptor = (ListVariableDescriptor<Solution_>) valueSelector.getVariableDescriptor();
        this.entitySelector = entitySelector;
        this.valueSelector = valueSelector; // At this point, guaranteed to only return assigned values.
        this.randomSelection = randomSelection;

        phaseLifecycleSupport.addEventListener(entitySelector);
        phaseLifecycleSupport.addEventListener(valueSelector);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        var supplyManager = solverScope.getScoreDirector().getSupplyManager();
        listVariableStateSupply = supplyManager.demand(listVariableDescriptor.getStateDemand());
        movableValueSelector = filterPinnedListPlanningVariableValuesWithIndex(valueSelector, listVariableStateSupply);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        listVariableStateSupply = null;
        movableValueSelector = null;
    }

    @Override
    public long getSize() {
        if (entitySelector.getSize() == 0) {
            return 0;
        }
        return entitySelector.getSize() + getEffectiveValueSelector().getSize();
    }

    private EntityIndependentValueSelector<Solution_> getEffectiveValueSelector() { // Simplify tests.
        return Objects.requireNonNullElse(movableValueSelector, valueSelector);
    }

    @Override
    public Iterator<ElementLocation> iterator() {
        if (randomSelection) {
            var allowsUnassignedValues = listVariableDescriptor.allowsUnassignedValues();

            // In case of list var which allows unassigned values, we need to exclude unassigned elements.
            var effectiveValueSelector = getEffectiveValueSelector();
            var totalValueSize = effectiveValueSelector.getSize()
                    - (allowsUnassignedValues ? listVariableStateSupply.getUnassignedCount() : 0);
            var totalSize = Math.addExact(entitySelector.getSize(), totalValueSize);
            return new ElementLocationRandomIterator<>(listVariableStateSupply, entitySelector, effectiveValueSelector,
                    workingRandom, totalSize, allowsUnassignedValues);
        } else {
            if (entitySelector.getSize() == 0) {
                return Collections.emptyIterator();
            }
            // If the list variable allows unassigned values, add the option of unassigning.
            var stream = listVariableDescriptor.allowsUnassignedValues() ? Stream.of(ElementLocation.unassigned())
                    : Stream.<ElementLocation> empty();
            // Start with the first unpinned value of each entity, or zero if no pinning.
            // Entity selector is guaranteed to return only unpinned entities.
            stream = Stream.concat(stream,
                    StreamSupport.stream(entitySelector.spliterator(), false)
                            .map(entity -> ElementLocation.of(entity, listVariableDescriptor.getFirstUnpinnedIndex(entity))));
            // Filter guarantees that we only get values that are actually in one of the lists.
            // Value selector guarantees only unpinned values.
            stream = Stream.concat(stream,
                    StreamSupport.stream(getEffectiveValueSelector().spliterator(), false)
                            .map(v -> listVariableStateSupply.getLocationInList(v))
                            .flatMap(elementLocation -> elementLocation instanceof LocationInList locationInList
                                    ? Stream.of(locationInList)
                                    : Stream.empty())
                            .map(locationInList -> ElementLocation.of(locationInList.entity(), locationInList.index() + 1)));
            return (Iterator<ElementLocation>) stream.iterator();
        }
    }

    @Override
    public boolean isCountable() {
        return entitySelector.isCountable() && getEffectiveValueSelector().isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return randomSelection || entitySelector.isNeverEnding() || getEffectiveValueSelector().isNeverEnding();
    }

    public ListVariableDescriptor<Solution_> getVariableDescriptor() {
        return (ListVariableDescriptor<Solution_>) getEffectiveValueSelector().getVariableDescriptor();
    }

    public EntityDescriptor<Solution_> getEntityDescriptor() {
        return entitySelector.getEntityDescriptor();
    }

    public Iterator<Object> endingIterator() {
        EntityIndependentValueSelector<Solution_> effectiveValueSelector = getEffectiveValueSelector();
        return Stream.concat(
                StreamSupport.stream(Spliterators.spliterator(entitySelector.endingIterator(),
                        entitySelector.getSize(), 0), false),
                StreamSupport.stream(Spliterators.spliterator(effectiveValueSelector.endingIterator(null),
                        effectiveValueSelector.getSize(), 0), false))
                .iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ElementDestinationSelector<?> that = (ElementDestinationSelector<?>) o;
        return randomSelection == that.randomSelection
                && Objects.equals(entitySelector, that.entitySelector)
                && Objects.equals(valueSelector, that.valueSelector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entitySelector, valueSelector, randomSelection);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + entitySelector + ", " + valueSelector + ")";
    }

}
