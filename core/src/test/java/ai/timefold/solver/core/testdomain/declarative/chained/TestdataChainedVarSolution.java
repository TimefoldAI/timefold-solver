package ai.timefold.solver.core.testdomain.declarative.chained;

import java.util.EnumSet;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataChainedVarSolution {

    public static SolutionDescriptor<TestdataChainedVarSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(EnumSet.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataChainedVarSolution.class, TestdataChainedVarEntity.class, TestdataChainedVarValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataChainedVarEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataChainedVarValue> values;

    @PlanningScore
    HardSoftScore score;

    public TestdataChainedVarSolution() {
    }

    public TestdataChainedVarSolution(List<TestdataChainedVarEntity> entities, List<TestdataChainedVarValue> values) {
        this.values = values;
        this.entities = entities;
    }

    public List<TestdataChainedVarValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataChainedVarValue> values) {
        this.values = values;
    }

    public List<TestdataChainedVarEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<TestdataChainedVarEntity> entities) {
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
        return "TestdataBasicVarSolution{" +
                "entities=" + entities +
                ", values=" + values +
                ", score=" + score +
                '}';
    }
}
