package ai.timefold.solver.spring.boot.autoconfigure.dummy.basic.noEntity;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringEntity;

@PlanningSolution
public class DummyTestdataSpringSolution {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "valueRange")
    private List<String> valueList;
    @PlanningEntityCollectionProperty
    private List<TestdataSpringEntity> entityList;

    @PlanningScore
    private SimpleScore score;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataSpringEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataSpringEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
