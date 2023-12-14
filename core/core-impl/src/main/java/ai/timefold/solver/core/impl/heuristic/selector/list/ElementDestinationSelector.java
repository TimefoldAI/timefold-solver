package ai.timefold.solver.core.impl.heuristic.selector.list;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonListInverseVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractSelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.solver.random.RandomUtils;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Selects destinations for list variable change moves. The destination specifies a future position in a list variable,
 * expressed as an {@link ElementRef}, where a moved element or subList can be inserted.
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

    /**
     * Only selects entities that are not pinned.
     * Pinned entities have their entire list variables pinned, therefore are not useful as destinations.
     */
    private final EntitySelector<Solution_> entitySelector;
    private final EntityIndependentValueSelector<Solution_> valueSelector;
    private final boolean randomSelection;

    private SingletonInverseVariableSupply inverseVariableSupply;
    private IndexVariableSupply indexVariableSupply;
    private EntityIndependentValueSelector<Solution_> movableValueSelector;

    public ElementDestinationSelector(
            EntitySelector<Solution_> entitySelector,
            EntityIndependentValueSelector<Solution_> valueSelector,
            boolean randomSelection) {
        this.entitySelector = entitySelector;
        this.valueSelector = valueSelector;
        this.randomSelection = randomSelection;

        phaseLifecycleSupport.addEventListener(entitySelector);
        phaseLifecycleSupport.addEventListener(valueSelector);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        SupplyManager supplyManager = solverScope.getScoreDirector().getSupplyManager();
        ListVariableDescriptor<?> listVariableDescriptor = (ListVariableDescriptor<?>) valueSelector.getVariableDescriptor();
        inverseVariableSupply = supplyManager.demand(new SingletonListInverseVariableDemand<>(listVariableDescriptor));
        movableValueSelector =
                ListChangeMoveSelector.filterPinnedListPlanningVariableValues(valueSelector, inverseVariableSupply);
        indexVariableSupply = supplyManager.demand(new IndexVariableDemand<>(listVariableDescriptor));
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        inverseVariableSupply = null;
        indexVariableSupply = null;
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
        if (movableValueSelector == null) {
            return valueSelector;
        } else {
            return movableValueSelector;
        }
    }

    @Override
    public Iterator<ElementRef> iterator() {
        if (randomSelection) {
            long totalSize = Math.addExact(entitySelector.getSize(), valueSelector.getSize());
            Iterator<Object> entityIterator = entitySelector.iterator();
            Iterator<Object> valueIterator = getEffectiveValueSelector().iterator();

            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    // The valueSelector's hasNext() is insignificant. The next random destination exists if and only if
                    // there is a next entity.
                    return entityIterator.hasNext();
                }

                @Override
                public ElementRef next() {
                    long entitySize = entitySelector.getSize();
                    if (RandomUtils.nextLong(workingRandom, totalSize) < entitySize) {
                        Object entity = entityIterator.next();
                        return new ElementRef(entity, 0);
                    }
                    Object value = valueIterator.next();
                    Object entity = inverseVariableSupply.getInverseSingleton(value);
                    return new ElementRef(entity, indexVariableSupply.getIndex(value) + 1);
                }
            };
        } else {
            if (entitySelector.getSize() == 0) {
                return Collections.emptyIterator();
            }
            return Stream.concat(
                    StreamSupport.stream(entitySelector.spliterator(), false)
                            .map(entity -> new ElementRef(entity, 0)),
                    StreamSupport.stream(getEffectiveValueSelector().spliterator(), false)
                            .map(value -> {
                                Object entity = inverseVariableSupply.getInverseSingleton(value);
                                return new ElementRef(entity, indexVariableSupply.getIndex(value) + 1);
                            }))
                    .iterator();
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
