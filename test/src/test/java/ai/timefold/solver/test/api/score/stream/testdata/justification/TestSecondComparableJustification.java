package ai.timefold.solver.test.api.score.stream.testdata.justification;

import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

public class TestSecondComparableJustification
        implements ConstraintJustification, Comparable<TestSecondComparableJustification> {

    private final String id;

    public TestSecondComparableJustification(String id) {
        this.id = id;
    }

    @Override
    public int compareTo(TestSecondComparableJustification o) {
        return this.id.compareTo(o.id);
    }

    @Override
    public String toString() {
        return "TestSecondComparableJustification[" +
                "id=" + id +
                ']';
    }
}
