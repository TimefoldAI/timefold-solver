package ai.timefold.solver.core.testdomain.shadow.dependency;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataDependencySolution {
    public static SolutionDescriptor<TestdataDependencySolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataDependencySolution.class, TestdataDependencyEntity.class, TestdataDependencyValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataDependencyEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataDependencyValue> values;

    @PlanningScore
    HardSoftScore score;

    public TestdataDependencySolution() {
    }

    public TestdataDependencySolution(List<TestdataDependencyEntity> entities, List<TestdataDependencyValue> values) {
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

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
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
