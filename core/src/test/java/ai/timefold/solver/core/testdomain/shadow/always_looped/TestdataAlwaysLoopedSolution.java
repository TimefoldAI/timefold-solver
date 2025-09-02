package ai.timefold.solver.core.testdomain.shadow.always_looped;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataAlwaysLoopedSolution {

    public static SolutionDescriptor<TestdataAlwaysLoopedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataAlwaysLoopedSolution.class, TestdataAlwaysLoopedEntity.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataAlwaysLoopedEntity> entities;

    @ValueRangeProvider
    List<Integer> values;

    @PlanningScore
    SimpleScore score;

    public TestdataAlwaysLoopedSolution() {
    }

    public TestdataAlwaysLoopedSolution(List<TestdataAlwaysLoopedEntity> entities, List<Integer> values) {
        this.values = values;
        this.entities = entities;
    }

    public List<Integer> getValues() {
        return values;
    }

    public void setValues(List<Integer> values) {
        this.values = values;
    }

    public List<TestdataAlwaysLoopedEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<TestdataAlwaysLoopedEntity> entities) {
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
        return "TestdataAlwaysLoopedSolution{" +
                "entities=" + entities +
                ", values=" + values +
                ", score=" + score +
                '}';
    }
}
