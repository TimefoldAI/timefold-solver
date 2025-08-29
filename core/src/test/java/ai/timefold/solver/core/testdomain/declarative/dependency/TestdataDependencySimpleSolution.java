package ai.timefold.solver.core.testdomain.declarative.dependency;

import java.util.EnumSet;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

/**
 * Exists solely to be used in Constraint Steam tests, which require the score type
 * to be SimpleScore
 */
@PlanningSolution
public class TestdataDependencySimpleSolution {
    public static SolutionDescriptor<TestdataDependencySimpleSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(EnumSet.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataDependencySimpleSolution.class, TestdataDependencyEntity.class, TestdataDependencyValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataDependencyEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataDependencyValue> values;

    @PlanningScore
    SimpleScore score;

    public TestdataDependencySimpleSolution() {
    }

    public TestdataDependencySimpleSolution(List<TestdataDependencyEntity> entities, List<TestdataDependencyValue> values) {
        this.values = values;
        this.entities = entities;
    }

    public List<TestdataDependencyValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataDependencyValue> values) {
        this.values = values;
    }

    public List<TestdataDependencyEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<TestdataDependencyEntity> entities) {
        this.entities = entities;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "TestdataPredecessorSolution{" +
                "entities=" + entities +
                ", values=" + values +
                ", score=" + score +
                '}';
    }
}
