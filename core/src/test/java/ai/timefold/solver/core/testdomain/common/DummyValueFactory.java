package ai.timefold.solver.core.testdomain.common;

import ai.timefold.solver.core.api.domain.common.SorterFactory;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.sort.factory.TestdataListFactorySortableSolution;

public class DummyValueFactory
        implements SorterFactory<TestdataListFactorySortableSolution, TestdataValue> {

    @Override
    public Comparable<TestdataValue> createSorter(TestdataListFactorySortableSolution solution, TestdataValue selection) {
        return v -> 0;
    }
}
