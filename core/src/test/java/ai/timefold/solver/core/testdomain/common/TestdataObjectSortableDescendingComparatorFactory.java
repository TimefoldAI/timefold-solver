package ai.timefold.solver.core.testdomain.common;

import java.util.Comparator;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.testdomain.TestdataObject;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class TestdataObjectSortableDescendingComparatorFactory implements ComparatorFactory<Object, TestdataObject> {

    @Override
    public Comparator<TestdataObject> createComparator(Object solution) {
        return new TestdataObjectSortableDescendingComparator();
    }
}
