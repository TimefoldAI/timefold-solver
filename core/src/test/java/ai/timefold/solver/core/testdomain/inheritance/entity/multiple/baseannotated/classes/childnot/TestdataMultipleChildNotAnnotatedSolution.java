package ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.childnot;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataMultipleChildNotAnnotatedSolution {

    public static TestdataMultipleChildNotAnnotatedSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataMultipleChildNotAnnotatedSolution();
        var valueList = new ArrayList<String>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add("Generated Value " + i);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataMultipleChildNotAnnotatedChildEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var value = valueList.get(i % valueListSize);
            var entity = new TestdataMultipleChildNotAnnotatedChildEntity(i);
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
    private List<TestdataMultipleChildNotAnnotatedChildEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataMultipleChildNotAnnotatedChildEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataMultipleChildNotAnnotatedChildEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
