
package ai.timefold.solver.core.impl.testdata.domain.basic_list;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningSolution
public class TestdataBasicListSolution extends TestdataObject {

    private List<TestdataValue> valueList;
    private List<TestdataValue> secondValueList;
    private List<TestdataBasicListEntity> entityList;

    private SimpleScore score;

    public TestdataBasicListSolution() {
    }

    public TestdataBasicListSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    @ValueRangeProvider(id = "secondValueRange")
    @ProblemFactCollectionProperty
    public List<TestdataValue> getSecondValueList() {
        return secondValueList;
    }

    public void setSecondValueList(List<TestdataValue> secondValueList) {
        this.secondValueList = secondValueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataBasicListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataBasicListEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
