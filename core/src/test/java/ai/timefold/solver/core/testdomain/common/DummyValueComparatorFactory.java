package ai.timefold.solver.core.testdomain.common;

import java.util.Comparator;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.sort.factory.TestdataListFactorySortableSolution;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DummyValueComparatorFactory implements ComparatorFactory<TestdataListFactorySortableSolution, TestdataValue> {

    @Override
    public Comparator<TestdataValue> createComparator(TestdataListFactorySortableSolution testdataListFactorySortableSolution) {
        return new DummyValueComparator();
    }
}
