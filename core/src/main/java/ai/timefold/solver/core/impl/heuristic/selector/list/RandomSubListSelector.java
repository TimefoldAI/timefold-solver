package ai.timefold.solver.core.impl.heuristic.selector.list;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector.filterPinnedListPlanningVariableValuesWithIndex;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class RandomSubListSelector<Solution_> extends AbstractSelector<Solution_> implements SubListSelector<Solution_> {

    private final EntitySelector<Solution_> entitySelector;
    private final EntityIndependentValueSelector<Solution_> valueSelector;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final int minimumSubListSize;
    private final int maximumSubListSize;

    private TriangleElementFactory triangleElementFactory;
    private ListVariableStateSupply<Solution_> listVariableStateSupply;

    public RandomSubListSelector(
            EntitySelector<Solution_> entitySelector,
            EntityIndependentValueSelector<Solution_> valueSelector,
            int minimumSubListSize, int maximumSubListSize) {
        this.entitySelector = entitySelector;
        this.valueSelector = filterPinnedListPlanningVariableValuesWithIndex(valueSelector, this::getListVariableStateSupply);
        this.listVariableDescriptor = (ListVariableDescriptor<Solution_>) valueSelector.getVariableDescriptor();
        if (minimumSubListSize < 1) {
            // TODO raise this to 2 in Timefold Solver 2.0
            throw new IllegalArgumentException(
                    "The minimumSubListSize (" + minimumSubListSize + ") must be greater than 0.");
        }
        if (minimumSubListSize > maximumSubListSize) {
            throw new IllegalArgumentException("The minimumSubListSize (" + minimumSubListSize
                    + ") must be less than or equal to the maximumSubListSize (" + maximumSubListSize + ").");
        }
        this.minimumSubListSize = minimumSubListSize;
        this.maximumSubListSize = maximumSubListSize;

        phaseLifecycleSupport.addEventListener(this.entitySelector);
        phaseLifecycleSupport.addEventListener(this.valueSelector);
    }

    private ListVariableStateSupply<Solution_> getListVariableStateSupply() {
        return Objects.requireNonNull(listVariableStateSupply,
                "Impossible state: The listVariableStateSupply is not initialized yet.");
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        triangleElementFactory = new TriangleElementFactory(minimumSubListSize, maximumSubListSize, workingRandom);
        var supplyManager = solverScope.getScoreDirector().getSupplyManager();
        listVariableStateSupply = supplyManager.demand(listVariableDescriptor.getStateDemand());
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        listVariableStateSupply = null;
    }

    @Override
    public ListVariableDescriptor<Solution_> getVariableDescriptor() {
        return listVariableDescriptor;
    }

    @Override
    public boolean isCountable() {
        return true;
    }

    @Override
    public boolean isNeverEnding() {
        return true;
    }

    @Override
    public long getSize() {
        long subListCount = 0;
        for (Object entity : ((Iterable<Object>) entitySelector::endingIterator)) {
            int listSize = listVariableDescriptor.getUnpinnedSubListSize(entity);
            // Add subLists bigger than minimum subList size.
            if (listSize >= minimumSubListSize) {
                subListCount += TriangularNumbers.nthTriangle(listSize - minimumSubListSize + 1);
                // Subtract moves with subLists bigger than maximum subList size.
                if (listSize > maximumSubListSize) {
                    subListCount -= TriangularNumbers.nthTriangle(listSize - maximumSubListSize);
                }
            }
        }
        return subListCount;
    }

    @Override
    public Iterator<Object> endingValueIterator() {
        // Child value selector is entity independent, so passing null entity is OK.
        return valueSelector.endingIterator(null);
    }

    @Override
    public long getValueCount() {
        return valueSelector.getSize();
    }

    @Override
    public Iterator<SubList> iterator() {
        // TODO make this incremental https://issues.redhat.com/browse/PLANNER-2507
        int biggestListSize = 0;
        for (Object entity : ((Iterable<Object>) entitySelector::endingIterator)) {
            biggestListSize = Math.max(biggestListSize, listVariableDescriptor.getUnpinnedSubListSize(entity));
        }
        if (biggestListSize < minimumSubListSize) {
            return new UpcomingSelectionIterator<>() {
                @Override
                protected SubList createUpcomingSelection() {
                    return noUpcomingSelection();
                }
            };
        }
        return new RandomSubListIterator(valueSelector.iterator());
    }

    private final class RandomSubListIterator extends UpcomingSelectionIterator<SubList> {

        private final Iterator<Object> valueIterator;

        private RandomSubListIterator(Iterator<Object> valueIterator) {
            this.valueIterator = valueIterator;
        }

        @Override
        protected SubList createUpcomingSelection() {
            Object sourceEntity = null;
            int listSize = 0;

            var firstUnpinnedIndex = 0;
            while (listSize < minimumSubListSize) {
                if (!valueIterator.hasNext()) {
                    throw new IllegalStateException("The valueIterator (" + valueIterator + ") should never end.");
                }
                // Using valueSelector instead of entitySelector is fairer
                // because entities with bigger list variables will be selected more often.
                var value = valueIterator.next();
                sourceEntity = listVariableStateSupply.getInverseSingleton(value);
                if (sourceEntity == null) { // Ignore values which are unassigned.
                    continue;
                }
                firstUnpinnedIndex = listVariableDescriptor.getFirstUnpinnedIndex(sourceEntity);
                listSize = listVariableDescriptor.getListSize(sourceEntity) - firstUnpinnedIndex;
            }

            TriangleElementFactory.TriangleElement triangleElement = triangleElementFactory.nextElement(listSize);
            int subListLength = listSize - triangleElement.level() + 1;
            if (subListLength < 1) {
                throw new IllegalStateException("Impossible state: The subListLength (%s) must be greater than 0."
                        .formatted(subListLength));
            }
            int sourceIndex = triangleElement.indexOnLevel() - 1 + firstUnpinnedIndex;
            return new SubList(sourceEntity, sourceIndex, subListLength);
        }
    }

    public int getMinimumSubListSize() {
        return minimumSubListSize;
    }

    public int getMaximumSubListSize() {
        return maximumSubListSize;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + valueSelector + ")";
    }
}
