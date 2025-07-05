package ai.timefold.solver.core.testdomain.declarative.multi_directional_parent;

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
public class TestdataMultiDirectionConcurrentSolution {
    public static SolutionDescriptor<TestdataMultiDirectionConcurrentSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(EnumSet.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataMultiDirectionConcurrentSolution.class,
                TestdataMultiDirectionConcurrentEntity.class, TestdataMultiDirectionConcurrentValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataMultiDirectionConcurrentEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMultiDirectionConcurrentValue> values;

    @PlanningScore
    HardSoftScore score;

    public List<TestdataMultiDirectionConcurrentEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataMultiDirectionConcurrentEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataMultiDirectionConcurrentValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataMultiDirectionConcurrentValue> values) {
        this.values = values;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
