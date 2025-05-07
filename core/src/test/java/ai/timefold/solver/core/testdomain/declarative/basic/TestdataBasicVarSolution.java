package ai.timefold.solver.core.testdomain.declarative.basic;

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
public class TestdataBasicVarSolution {

    public static SolutionDescriptor<TestdataBasicVarSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(EnumSet.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataBasicVarSolution.class, TestdataBasicVarEntity.class, TestdataBasicVarValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataBasicVarEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataBasicVarValue> values;

    @PlanningScore
    HardSoftScore score;

    public TestdataBasicVarSolution() {
    }

    public TestdataBasicVarSolution(List<TestdataBasicVarEntity> entities, List<TestdataBasicVarValue> values) {
        this.values = values;
        this.entities = entities;
    }

    public List<TestdataBasicVarValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataBasicVarValue> values) {
        this.values = values;
    }

    public List<TestdataBasicVarEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<TestdataBasicVarEntity> entities) {
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
