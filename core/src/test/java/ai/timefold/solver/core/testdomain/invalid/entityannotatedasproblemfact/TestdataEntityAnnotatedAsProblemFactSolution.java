package ai.timefold.solver.core.testdomain.invalid.entityannotatedasproblemfact;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataEntityAnnotatedAsProblemFactSolution {
    TestdataEntity entity;
    List<TestdataValue> values;
    SimpleScore score;

    public static SolutionDescriptor<TestdataEntityAnnotatedAsProblemFactSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataEntityAnnotatedAsProblemFactSolution.class,
                TestdataEntity.class);
    }

    public TestdataEntityAnnotatedAsProblemFactSolution() {
    }

    @PlanningEntityProperty
    public TestdataEntity getEntity() {
        return entity;
    }

    @ProblemFactProperty
    public TestdataEntity getEntityAsFact() {
        return entity;
    }

    public void setEntity(TestdataEntity entity) {
        this.entity = entity;
    }

    @ValueRangeProvider(id = "valueRange")
    public List<TestdataValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataValue> values) {
        this.values = values;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
