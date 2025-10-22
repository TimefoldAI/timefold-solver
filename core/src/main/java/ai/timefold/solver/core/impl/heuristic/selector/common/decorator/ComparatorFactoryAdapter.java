package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;

public class ComparatorFactoryAdapter<Solution_, T, V extends Comparable<V>> implements ComparatorFactory<Solution_, T, V> {

    private final SelectionSorterWeightFactory<Solution_, T> sorterWeightFactory;

    public ComparatorFactoryAdapter(SelectionSorterWeightFactory<Solution_, T> sorterWeightFactory) {
        this.sorterWeightFactory = sorterWeightFactory;
    }

    @Override
    public V createSorter(Solution_ solution, T selection) {
        return (V) sorterWeightFactory.createSorterWeight(solution, selection);
    }
}
