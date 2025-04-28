package ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.interfaces.childtoo;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataMultipleBothAnnotatedInterfaceSolution {

    public static TestdataMultipleBothAnnotatedInterfaceSolution generateSolution(int valueListSize, int entityListSize,
            boolean initialized) {
        var solution = new TestdataMultipleBothAnnotatedInterfaceSolution();
        var valueList = new ArrayList<String>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add("Generated Value " + i);
        }
        var valueList2 = new ArrayList<String>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList2.add("Generated Value2 " + i);
        }
        solution.setValueList(valueList);
        solution.setValueList2(valueList2);
        var entityList = new ArrayList<TestdataMultipleBothAnnotatedInterfaceChildEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataMultipleBothAnnotatedInterfaceChildEntity(i);
            if (initialized) {
                var value = valueList.get(i % valueListSize);
                var value2 = valueList2.get(i % valueListSize);
                entity.setValue(value);
                entity.setValue2(value2);
            }
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    private List<String> valueList;
    @ValueRangeProvider(id = "valueRange2")
    @ProblemFactCollectionProperty
    private List<String> valueList2;
    @PlanningEntityCollectionProperty
    private List<TestdataMultipleBothAnnotatedInterfaceChildEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public List<String> getValueList2() {
        return valueList2;
    }

    public void setValueList2(List<String> valueList2) {
        this.valueList2 = valueList2;
    }

    public List<TestdataMultipleBothAnnotatedInterfaceChildEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataMultipleBothAnnotatedInterfaceChildEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
