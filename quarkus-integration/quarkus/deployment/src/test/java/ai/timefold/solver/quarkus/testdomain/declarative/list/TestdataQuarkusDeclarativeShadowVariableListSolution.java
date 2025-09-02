package ai.timefold.solver.quarkus.testdomain.declarative.list;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataQuarkusDeclarativeShadowVariableListSolution {

    private List<TestdataQuarkusDeclarativeShadowVariableListValue> valueList;
    private List<TestdataQuarkusDeclarativeShadowVariableListEntity> entityList;

    private SimpleScore score;

    @ValueRangeProvider
    @PlanningEntityCollectionProperty
    public List<TestdataQuarkusDeclarativeShadowVariableListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataQuarkusDeclarativeShadowVariableListValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataQuarkusDeclarativeShadowVariableListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataQuarkusDeclarativeShadowVariableListEntity> entityList) {
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
