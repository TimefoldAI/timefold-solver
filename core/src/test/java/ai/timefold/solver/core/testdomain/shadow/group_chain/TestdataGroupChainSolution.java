package ai.timefold.solver.core.testdomain.shadow.group_chain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataGroupChainSolution {
    public static SolutionDescriptor<TestdataGroupChainSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataGroupChainSolution.class, TestdataGroupChainEntity.class, TestdataGroupChainValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataGroupChainEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataGroupChainValue> values;

    @PlanningScore
    SimpleScore score;

    public TestdataGroupChainSolution() {
    }

    public TestdataGroupChainSolution(List<TestdataGroupChainEntity> entities, List<TestdataGroupChainValue> values) {
        this.entities = entities;
        this.values = values;
    }

    public List<TestdataGroupChainEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataGroupChainEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataGroupChainValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataGroupChainValue> values) {
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
        return "TestdataGroupChainSolution{" +
                "entities=" + entities +
                ", score=" + score +
                '}';
    }
}
