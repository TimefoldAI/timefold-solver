package ai.timefold.solver.core.testdomain.shadow.multiplelistener;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataListMultipleShadowVariableSolution {

    public static SolutionDescriptor<TestdataListMultipleShadowVariableSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataListMultipleShadowVariableSolution.class,
                TestdataListMultipleShadowVariableEntity.class,
                TestdataListMultipleShadowVariableValue.class);
    }

    public static TestdataListMultipleShadowVariableSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataListMultipleShadowVariableSolution();
        var valueList = new ArrayList<TestdataListMultipleShadowVariableValue>(valueListSize);
        for (var i = 0; i < valueListSize; i++) {
            var value = new TestdataListMultipleShadowVariableValue("Generated Value " + i);
            valueList.add(value);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataListMultipleShadowVariableEntity>(entityListSize);
        for (var i = 0; i < entityListSize; i++) {
            var value = valueList.get(i % valueListSize);
            var entity =
                    new TestdataListMultipleShadowVariableEntity("Generated Entity " + i, value);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "valueRange")
    private List<TestdataListMultipleShadowVariableValue> valueList;
    @PlanningEntityCollectionProperty
    private List<TestdataListMultipleShadowVariableEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<TestdataListMultipleShadowVariableValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListMultipleShadowVariableValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataListMultipleShadowVariableEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListMultipleShadowVariableEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
