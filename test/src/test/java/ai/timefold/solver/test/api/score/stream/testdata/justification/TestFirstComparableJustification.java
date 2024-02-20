package ai.timefold.solver.test.api.score.stream.testdata.justification;

import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

public class TestFirstComparableJustification implements ConstraintJustification, Comparable<TestFirstComparableJustification> {

    private final int id;

    public TestFirstComparableJustification(int id) {
        this.id = id;
    }

    @Override
    public int compareTo(TestFirstComparableJustification o) {
        return this.id - o.id;
    }

    @Override
    public String toString() {
        return "TestFirstComparableJustification[" +
                "id=" + id +
                ']';
    }
}
