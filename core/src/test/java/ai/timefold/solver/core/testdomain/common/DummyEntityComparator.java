package ai.timefold.solver.core.testdomain.common;

import java.util.Comparator;

import ai.timefold.solver.core.testdomain.TestdataEntity;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DummyEntityComparator implements Comparator<TestdataEntity> {
    @Override
    public int compare(TestdataEntity testdataEntity, TestdataEntity testdataEntity2) {
        return 0;
    }
}
