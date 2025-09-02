package ai.timefold.solver.core.testdomain.shadow.concurrent;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataConcurrentSolution {
    public static SolutionDescriptor<TestdataConcurrentSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataConcurrentSolution.class,
                TestdataConcurrentEntity.class, TestdataConcurrentValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataConcurrentEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataConcurrentValue> values;

    @PlanningScore
    HardSoftScore score;

    public List<TestdataConcurrentEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataConcurrentEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataConcurrentValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataConcurrentValue> values) {
        this.values = values;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
