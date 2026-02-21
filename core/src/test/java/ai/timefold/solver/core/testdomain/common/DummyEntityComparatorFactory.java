package ai.timefold.solver.core.testdomain.common;

import java.util.Comparator;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DummyEntityComparatorFactory
        implements ComparatorFactory<TestdataSolution, TestdataEntity> {

    @Override
    public Comparator<TestdataEntity> createComparator(TestdataSolution solution) {
        return new DummyEntityComparator();
    }

}
