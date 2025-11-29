package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.testutil.CodeAssertable;

public class CodeAssertableSorter<T extends CodeAssertable> implements SelectionSorter<Object, T> {

    @Override
    public void sort(Object solution, List<T> selectionList) {
        selectionList.sort(Comparator.comparing(CodeAssertable::getCode));
    }

    @Override
    public SortedSet<T> sort(Object solution, Set<T> selectionSet) {
        var sortedSet = new TreeSet<T>(Comparator.comparing(CodeAssertable::getCode));
        sortedSet.addAll(selectionSet);
        return sortedSet;
    }
}
