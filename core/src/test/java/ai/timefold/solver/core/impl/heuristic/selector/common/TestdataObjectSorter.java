package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.ArrayList;
import java.util.Collections;
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
    public List<T> sort(S solution, List<T> selectionList) {
        var sortedList = new ArrayList<>(selectionList);
        var comparator = Comparator.comparing(TestdataObject::getCode);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        var updatedList = new ArrayList<>(sortedList.stream().map(v -> (TestdataObject) v).toList());
        Collections.sort(updatedList, comparator);
        return (List<T>) updatedList;
    }

    @Override
    public SortedSet<T> sort(S solution, Set<T> selectionSet) {
        var comparator = Comparator.comparing(TestdataObject::getCode);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        var sortedSet = new TreeSet<>(comparator);
        sortedSet.addAll(selectionSet.stream().map(v -> (TestdataObject) v).toList());
        return (SortedSet<T>) sortedSet;
    }
}
