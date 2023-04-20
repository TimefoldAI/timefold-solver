package ai.timefold.solver.spring.boot.autoconfigure.gizmo.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataGizmoSpringSolution {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "valueRange")
    public List<String> valueList;
    @PlanningEntityCollectionProperty
    public List<TestdataGizmoSpringEntity> entityList;

    @PlanningScore
    public SimpleScore score;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataGizmoSpringEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataGizmoSpringEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
