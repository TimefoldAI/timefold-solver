package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.childnot;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataChildNotAnnotatedInterfaceSolution {

    public static TestdataChildNotAnnotatedInterfaceSolution generateSolution(int valueListSize, int entityListSize,
            boolean initialize) {
        var solution = new TestdataChildNotAnnotatedInterfaceSolution();
        var valueList = new ArrayList<String>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add("Generated Value " + i);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataChildNotAnnotatedInterfaceChildEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataChildNotAnnotatedInterfaceChildEntity(i);
            if (initialize) {
                var value = valueList.get(i % valueListSize);
                entity.setValue(value);
            }
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    private List<String> valueList;
    @PlanningEntityCollectionProperty
    private List<TestdataChildNotAnnotatedInterfaceChildEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataChildNotAnnotatedInterfaceChildEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataChildNotAnnotatedInterfaceChildEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
