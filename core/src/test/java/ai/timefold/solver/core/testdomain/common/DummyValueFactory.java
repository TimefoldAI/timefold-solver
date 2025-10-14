package ai.timefold.solver.core.testdomain.common;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.sort.factory.TestdataListFactorySortableSolution;

public class DummyValueFactory
        implements ComparatorFactory<TestdataListFactorySortableSolution, TestdataValue> {

    @Override
    public Comparable<TestdataValue> createSorter(TestdataListFactorySortableSolution solution, TestdataValue selection) {
        return v -> 0;
    }
}
