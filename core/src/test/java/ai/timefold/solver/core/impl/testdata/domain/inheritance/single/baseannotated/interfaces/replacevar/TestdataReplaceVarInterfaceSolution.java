package ai.timefold.solver.core.impl.testdata.domain.inheritance.single.baseannotated.interfaces.replacevar;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataReplaceVarInterfaceSolution {

    public static TestdataReplaceVarInterfaceSolution generateSolution(int valueListSize, int entityListSize,
            boolean initialize) {
        var solution = new TestdataReplaceVarInterfaceSolution();
        var valueList = new ArrayList<String>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add("Generated Value " + i);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataReplaceVarInterfaceChildEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataReplaceVarInterfaceChildEntity(i);
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
    private List<TestdataReplaceVarInterfaceChildEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataReplaceVarInterfaceChildEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataReplaceVarInterfaceChildEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
