package ai.timefold.solver.core.testdomain.invalid.entityannotatedasproblemfact;

import java.util.List;

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
public class TestdataEntityAnnotatedAsProblemFactCollectionSolution {
    List<TestdataEntity> entities;
    List<TestdataValue> values;
    SimpleScore score;

    public static SolutionDescriptor<TestdataEntityAnnotatedAsProblemFactCollectionSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataEntityAnnotatedAsProblemFactCollectionSolution.class,
                TestdataEntity.class);
    }

    public TestdataEntityAnnotatedAsProblemFactCollectionSolution() {
    }

    @ProblemFactCollectionProperty
    public List<TestdataEntity> getEntitiesAsFacts() {
        return entities;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataEntity> entities) {
        this.entities = entities;
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
