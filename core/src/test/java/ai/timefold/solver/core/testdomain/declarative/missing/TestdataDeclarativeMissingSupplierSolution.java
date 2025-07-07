package ai.timefold.solver.core.testdomain.declarative.missing;

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
public class TestdataDeclarativeMissingSupplierSolution {

    public static SolutionDescriptor<TestdataDeclarativeMissingSupplierSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(EnumSet.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataDeclarativeMissingSupplierSolution.class, TestdataDeclarativeMissingSupplierEntity.class,
                TestdataDeclarativeMissingSupplierValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataDeclarativeMissingSupplierEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataDeclarativeMissingSupplierValue> values;

    @PlanningScore
    HardSoftScore score;

    public TestdataDeclarativeMissingSupplierSolution() {
    }

    public TestdataDeclarativeMissingSupplierSolution(List<TestdataDeclarativeMissingSupplierEntity> entities,
            List<TestdataDeclarativeMissingSupplierValue> values) {
        this.values = values;
        this.entities = entities;
    }

    public List<TestdataDeclarativeMissingSupplierValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataDeclarativeMissingSupplierValue> values) {
        this.values = values;
    }

    public List<TestdataDeclarativeMissingSupplierEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<TestdataDeclarativeMissingSupplierEntity> entities) {
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
        return "TestdataDeclarativeMissingSupplierSolution{" +
                "entities=" + entities +
                ", values=" + values +
                ", score=" + score +
                '}';
    }
}
