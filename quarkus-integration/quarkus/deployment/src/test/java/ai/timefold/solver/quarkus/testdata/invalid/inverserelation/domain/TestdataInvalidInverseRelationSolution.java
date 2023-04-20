package ai.timefold.solver.quarkus.testdata.invalid.inverserelation.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataInvalidInverseRelationSolution {

    private List<TestdataInvalidInverseRelationValue> valueList;
    private List<TestdataInvalidInverseRelationEntity> entityList;

    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataInvalidInverseRelationValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataInvalidInverseRelationValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataInvalidInverseRelationEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataInvalidInverseRelationEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
