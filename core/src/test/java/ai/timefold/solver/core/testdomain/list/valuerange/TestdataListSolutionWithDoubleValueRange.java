package ai.timefold.solver.core.testdomain.list.valuerange;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.primdouble.DoubleValueRange;

@PlanningSolution
public class TestdataListSolutionWithDoubleValueRange {

    public static SolutionDescriptor<TestdataListSolutionWithDoubleValueRange> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataListSolutionWithDoubleValueRange.class,
                TestdataListEntityWithDoubleValueRange.class);
    }

    public static TestdataListSolutionWithDoubleValueRange generateSolution() {
        var solution = new TestdataListSolutionWithDoubleValueRange();
        var entity = new TestdataListEntityWithDoubleValueRange("e1");
        solution.setEntity(entity);
        solution.setDoubleValueRange(new DoubleValueRange(0, 10));
        return solution;
    }

    private DoubleValueRange doubleValueRange;
    private TestdataListEntityWithDoubleValueRange entity;
    private SimpleScore score;

    @ValueRangeProvider(id = "doubleValueRange")
    @ProblemFactProperty
    public DoubleValueRange getDoubleValueRange() {
        return doubleValueRange;
    }

    public void setDoubleValueRange(DoubleValueRange doubleValueRange) {
        this.doubleValueRange = doubleValueRange;
    }

    @PlanningEntityProperty
    public TestdataListEntityWithDoubleValueRange getEntity() {
        return entity;
    }

    public void setEntity(TestdataListEntityWithDoubleValueRange entity) {
        this.entity = entity;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
