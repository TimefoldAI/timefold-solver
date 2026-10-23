package ai.timefold.solver.core.testdomain.shadow.aligned_dependency;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataAlignedDependencySolution {
    public static SolutionDescriptor<TestdataAlignedDependencySolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataAlignedDependencySolution.class, TestdataAlignedDependencyEntity.class,
                TestdataAlignedDependencyValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataAlignedDependencyEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataAlignedDependencyValue> values;

    @PlanningScore
    HardSoftScore score;

    public TestdataAlignedDependencySolution() {
    }

    public TestdataAlignedDependencySolution(List<TestdataAlignedDependencyEntity> entities,
            List<TestdataAlignedDependencyValue> values) {
        this.entities = entities;
        this.values = values;
    }

    public List<TestdataAlignedDependencyEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataAlignedDependencyEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataAlignedDependencyValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataAlignedDependencyValue> values) {
        this.values = values;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "TestdataAlignedDependencySolution{" +
                "entities=" + entities +
                ", score=" + score +
                '}';
    }
}
