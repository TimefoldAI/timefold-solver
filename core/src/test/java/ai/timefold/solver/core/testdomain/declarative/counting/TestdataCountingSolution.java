package ai.timefold.solver.core.testdomain.declarative.counting;

import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataCountingSolution extends TestdataObject {
    public static SolutionDescriptor<TestdataCountingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(Set.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataCountingSolution.class, TestdataCountingEntity.class, TestdataCountingValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataCountingEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataCountingValue> values;

    @PlanningScore
    SimpleScore score;

    public TestdataCountingSolution() {
    }

    public TestdataCountingSolution(String code, List<TestdataCountingEntity> entities, List<TestdataCountingValue> values) {
        super(code);
        this.entities = entities;
        this.values = values;
    }

    public List<TestdataCountingEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataCountingEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataCountingValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataCountingValue> values) {
        this.values = values;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
