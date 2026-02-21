package ai.timefold.solver.core.testdomain.common;

import java.util.Comparator;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class TestSortableObjectComparatorFactory
        implements ComparatorFactory<Object, TestSortableObject> {

    @Override
    public Comparator<TestSortableObject> createComparator(Object solution) {
        return new TestSortableObjectComparator();
    }
}
