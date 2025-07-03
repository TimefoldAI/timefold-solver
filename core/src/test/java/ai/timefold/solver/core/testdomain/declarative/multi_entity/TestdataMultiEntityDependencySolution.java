package ai.timefold.solver.core.testdomain.declarative.multi_entity;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataMultiEntityDependencySolution {
    public static SolutionDescriptor<TestdataMultiEntityDependencySolution> buildDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(Set.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataMultiEntityDependencySolution.class,
                TestdataMultiEntityDependencyEntity.class,
                TestdataMultiEntityDependencyValue.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataMultiEntityDependencyEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMultiEntityDependencyValue> values;

    @ValueRangeProvider
    List<TestdataMultiEntityDependencyDelay> delays;

    @PlanningScore
    HardSoftScore score;

    public TestdataMultiEntityDependencySolution() {
    }

    public TestdataMultiEntityDependencySolution(List<TestdataMultiEntityDependencyEntity> entities,
            List<TestdataMultiEntityDependencyValue> values) {
        this.values = values;
        this.entities = entities;
    }

    public static SolutionDescriptor<TestdataMultiEntityDependencySolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(EnumSet.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataMultiEntityDependencySolution.class, TestdataMultiEntityDependencyEntity.class,
                TestdataMultiEntityDependencyValue.class);
    }

    public List<TestdataMultiEntityDependencyValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataMultiEntityDependencyValue> values) {
        this.values = values;
    }

    public List<TestdataMultiEntityDependencyEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<TestdataMultiEntityDependencyEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataMultiEntityDependencyDelay> getDelays() {
        return delays;
    }

    public void setDelays(
            List<TestdataMultiEntityDependencyDelay> delays) {
        this.delays = delays;
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
