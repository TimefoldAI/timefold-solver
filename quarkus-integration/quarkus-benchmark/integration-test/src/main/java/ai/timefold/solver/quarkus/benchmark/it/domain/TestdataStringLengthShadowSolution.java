package ai.timefold.solver.quarkus.benchmark.it.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

@PlanningSolution
public class TestdataStringLengthShadowSolution {

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<TestdataListValueShadowEntity> valueList;

    @PlanningEntityCollectionProperty
    private List<TestdataStringLengthShadowEntity> entityList;

    @PlanningScore
    private HardSoftScore score;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public List<TestdataListValueShadowEntity> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListValueShadowEntity> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataStringLengthShadowEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataStringLengthShadowEntity> entityList) {
        this.entityList = entityList;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
