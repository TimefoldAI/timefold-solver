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

public class TestdataObjectSorter<T extends TestdataObject> implements SelectionSorter<Object, T> {

    @Override
    public List<T> sort(Object solution, List<T> selectionList) {
        var sortedList = new ArrayList<>(selectionList);
        Collections.sort(sortedList, Comparator.comparing(TestdataObject::getCode));
        return sortedList;
    }

    @Override
    public SortedSet<T> sort(Object solution, Set<T> selectionSet) {
        var sortedSet = new TreeSet<T>(Comparator.comparing(TestdataObject::getCode));
        sortedSet.addAll(selectionSet);
        return sortedSet;
    }
}
