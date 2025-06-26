package ai.timefold.solver.core.testdomain.declarative.extended;

import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataDeclarativeExtendedSolution extends TestdataObject {
    public static SolutionDescriptor<TestdataDeclarativeExtendedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(Set.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataDeclarativeExtendedSolution.class,
                TestdataDeclarativeExtendedEntity.class,
                TestdataDeclarativeExtendedBaseValue.class,
                TestdataDeclarativeExtendedSubclassValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataDeclarativeExtendedEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataDeclarativeExtendedBaseValue> values;

    @PlanningScore
    HardSoftScore score;

    public List<TestdataDeclarativeExtendedEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<TestdataDeclarativeExtendedEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataDeclarativeExtendedBaseValue> getValues() {
        return values;
    }

    public void setValues(
            List<TestdataDeclarativeExtendedBaseValue> values) {
        this.values = values;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
