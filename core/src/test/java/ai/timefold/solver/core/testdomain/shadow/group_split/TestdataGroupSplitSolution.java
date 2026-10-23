package ai.timefold.solver.core.testdomain.shadow.group_split;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataGroupSplitSolution {
    public static SolutionDescriptor<TestdataGroupSplitSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataGroupSplitSolution.class, TestdataGroupSplitEntity.class, TestdataGroupSplitValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataGroupSplitEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataGroupSplitValue> values;

    @PlanningScore
    SimpleScore score;

    public TestdataGroupSplitSolution() {
    }

    public TestdataGroupSplitSolution(List<TestdataGroupSplitEntity> entities, List<TestdataGroupSplitValue> values) {
        this.entities = entities;
        this.values = values;
    }

    public List<TestdataGroupSplitEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataGroupSplitEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataGroupSplitValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataGroupSplitValue> values) {
        this.values = values;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "TestdataGroupSplitSolution{" +
                "entities=" + entities +
                ", score=" + score +
                '}';
    }
}
