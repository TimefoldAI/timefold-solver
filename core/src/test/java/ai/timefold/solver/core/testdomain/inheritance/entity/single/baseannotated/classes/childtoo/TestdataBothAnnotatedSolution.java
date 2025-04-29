package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.childtoo;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataBothAnnotatedSolution {

    public static TestdataBothAnnotatedSolution generateSolution(int valueListSize, int entityListSize, boolean initialized) {
        var solution = new TestdataBothAnnotatedSolution();
        var valueList = new ArrayList<TestdataValue>(valueListSize);
        var subValueList = new ArrayList<TestdataValue>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataValue("Generated value " + i));
            subValueList.add(new TestdataValue("Generated Subvalue " + i));
        }
        solution.setValueList(valueList);
        solution.setSubValueList(subValueList);
        var entityList = new ArrayList<TestdataBothAnnotatedChildEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataBothAnnotatedChildEntity(String.valueOf(i));
            if (initialized) {
                var value = valueList.get(i % valueList.size());
                var subValue = subValueList.get(i % subValueList.size());
                entity.setValue(value);
                entity.setSubValue(subValue);
            }
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    private List<TestdataValue> valueList;
    @ValueRangeProvider(id = "subValueRange")
    @ProblemFactCollectionProperty
    private List<TestdataValue> subValueList;
    @PlanningEntityCollectionProperty
    private List<TestdataBothAnnotatedChildEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public TestdataBothAnnotatedSolution() {
        // Empty constructor
    }

    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataValue> getSubValueList() {
        return subValueList;
    }

    public void setSubValueList(List<TestdataValue> subValueList) {
        this.subValueList = subValueList;
    }

    public List<TestdataBothAnnotatedChildEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataBothAnnotatedChildEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
