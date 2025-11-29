package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataObjectSorter<S, T> implements SelectionSorter<S, T> {

    private final boolean ascending;

    public TestdataObjectSorter() {
        this(true);
    }

    public TestdataObjectSorter(boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public void sort(S solution, List<T> selectionList) {
        var comparator = Comparator.comparing(v -> ((TestdataObject) v).getCode());
        if (!ascending) {
            comparator = comparator.reversed();
        }
        selectionList.sort(comparator);
    }

    @Override
    public SortedSet<T> sort(S solution, Set<T> selectionSet) {
        var comparator = Comparator.comparing(v -> ((TestdataObject) v).getCode());
        if (!ascending) {
            comparator = comparator.reversed();
        }
        var sortedSet = new TreeSet<>(comparator);
        sortedSet.addAll(selectionSet);
        return (SortedSet<T>) sortedSet;
    }
}
