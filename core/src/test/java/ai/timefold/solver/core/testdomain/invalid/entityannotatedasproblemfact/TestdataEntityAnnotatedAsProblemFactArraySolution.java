package ai.timefold.solver.core.testdomain.invalid.entityannotatedasproblemfact;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataEntityAnnotatedAsProblemFactArraySolution {
    TestdataEntity[] entities;
    TestdataValue[] values;
    SimpleScore score;

    public static SolutionDescriptor<TestdataEntityAnnotatedAsProblemFactArraySolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataEntityAnnotatedAsProblemFactArraySolution.class,
                TestdataEntity.class);
    }

    public TestdataEntityAnnotatedAsProblemFactArraySolution() {
    }

    @ProblemFactCollectionProperty
    public TestdataEntity[] getEntitiesAsFacts() {
        return entities;
    }

    @PlanningEntityCollectionProperty
    public TestdataEntity[] getEntities() {
        return entities;
    }

    public void setEntities(TestdataEntity[] entities) {
        this.entities = entities;
    }

    @ValueRangeProvider(id = "valueRange")
    public TestdataValue[] getValues() {
        return values;
    }

    public void setValues(TestdataValue[] values) {
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
