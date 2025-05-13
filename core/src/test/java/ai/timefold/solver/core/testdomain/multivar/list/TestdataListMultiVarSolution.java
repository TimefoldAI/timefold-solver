package ai.timefold.solver.core.testdomain.multivar.list;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataListMultiVarSolution {

    public static TestdataListMultiVarSolution generateUninitializedSolution(int entityListSize, int valueListSize,
            int otherValueListSize) {
        var solution = new TestdataListMultiVarSolution();
        var valueList = new ArrayList<TestdataListMultiVarValue>(valueListSize);
        var otherValueList = new ArrayList<TestdataListMultiVarOtherValue>(otherValueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataListMultiVarValue("Generated Value " + i));
        }
        for (int i = 0; i < otherValueListSize; i++) {
            otherValueList.add(new TestdataListMultiVarOtherValue("Generated Other Value " + i));
        }
        solution.setValueList(valueList);
        solution.setOtherValueList(otherValueList);
        var entityList = new ArrayList<TestdataListMultiVarEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataListMultiVarEntity(String.valueOf(i));
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    private List<TestdataListMultiVarValue> valueList;
    @ValueRangeProvider(id = "otherValueRange")
    @ProblemFactCollectionProperty
    private List<TestdataListMultiVarOtherValue> otherValueList;
    @PlanningEntityCollectionProperty
    private List<TestdataListMultiVarEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<TestdataListMultiVarValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListMultiVarValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataListMultiVarOtherValue> getOtherValueList() {
        return otherValueList;
    }

    public void setOtherValueList(List<TestdataListMultiVarOtherValue> otherValueList) {
        this.otherValueList = otherValueList;
    }

    public List<TestdataListMultiVarEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListMultiVarEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
