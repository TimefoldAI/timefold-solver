package ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.interfaces.childnot;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataMultipleChildNotAnnotatedInterfaceSolution {

    public static TestdataMultipleChildNotAnnotatedInterfaceSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataMultipleChildNotAnnotatedInterfaceSolution();
        var valueList = new ArrayList<String>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add("Generated Value " + i);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataMultipleChildNotAnnotatedInterfaceChildEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataMultipleChildNotAnnotatedInterfaceChildEntity(i);
            var value = valueList.get(i % valueListSize);
            entity.setValue(value);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    private List<String> valueList;
    @PlanningEntityCollectionProperty
    private List<TestdataMultipleChildNotAnnotatedInterfaceChildEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataMultipleChildNotAnnotatedInterfaceChildEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataMultipleChildNotAnnotatedInterfaceChildEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
