package ai.timefold.solver.spring.boot.it.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class IntegrationTestSolution {
    @PlanningEntityCollectionProperty
    private List<IntegrationTestEntity> entityList;

    private List<IntegrationTestValue> valueList;

    @PlanningScore
    private SimpleScore score;

    public IntegrationTestSolution() {
    }

    public IntegrationTestSolution(List<IntegrationTestEntity> entityList, List<IntegrationTestValue> valueList) {
        this.entityList = entityList;
        this.valueList = valueList;
    }

    public List<IntegrationTestEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<IntegrationTestEntity> entityList) {
        this.entityList = entityList;
    }

    public List<IntegrationTestValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<IntegrationTestValue> valueList) {
        this.valueList = valueList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
