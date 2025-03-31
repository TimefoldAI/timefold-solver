package ai.timefold.solver.test.api.score.stream.testdata.shadow.list;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataListShadowConstraintVerifierSolution {

    public static SolutionDescriptor<TestdataListShadowConstraintVerifierSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataListShadowConstraintVerifierSolution.class,
                TestdataListShadowConstraintVerifierEntity.class,
                TestdataListShadowConstraintVerifierValue.class);
    }

    public static TestdataListShadowConstraintVerifierSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataListShadowConstraintVerifierSolution();
        var valueList = new ArrayList<TestdataListShadowConstraintVerifierValue>(valueListSize);
        for (var i = 0; i < valueListSize; i++) {
            var value = new TestdataListShadowConstraintVerifierValue("Generated Value " + i);
            valueList.add(value);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataListShadowConstraintVerifierEntity>(entityListSize);
        for (var i = 0; i < entityListSize; i++) {
            var value = valueList.get(i % valueListSize);
            var entity =
                    new TestdataListShadowConstraintVerifierEntity("Generated Entity " + i, value);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "valueRange")
    private List<TestdataListShadowConstraintVerifierValue> valueList;
    @PlanningEntityCollectionProperty
    private List<TestdataListShadowConstraintVerifierEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<TestdataListShadowConstraintVerifierValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListShadowConstraintVerifierValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataListShadowConstraintVerifierEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListShadowConstraintVerifierEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
