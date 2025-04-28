package ai.timefold.solver.core.testdomain.equals;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataEqualsByCodeSolution extends TestdataEqualsByCodeObject {

    public static SolutionDescriptor<TestdataEqualsByCodeSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataEqualsByCodeSolution.class, TestdataEqualsByCodeEntity.class);
    }

    public static TestdataEqualsByCodeSolution generateSolution() {
        return generateSolution(5, 7);
    }

    public static TestdataEqualsByCodeSolution generateSolution(int valueListSize, int entityListSize) {
        return generateSolution("Generated Solution 0", valueListSize, entityListSize);
    }

    public static TestdataEqualsByCodeSolution generateSolution(String solutionId, int valueListSize, int entityListSize) {
        TestdataEqualsByCodeSolution solution = new TestdataEqualsByCodeSolution(solutionId);
        List<TestdataEqualsByCodeValue> valueList = new ArrayList<>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            TestdataEqualsByCodeValue value = new TestdataEqualsByCodeValue("Generated Value " + i);
            valueList.add(value);
        }
        solution.setValueList(valueList);
        List<TestdataEqualsByCodeEntity> entityList = new ArrayList<>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            TestdataEqualsByCodeValue value = valueList.get(i % valueListSize);
            TestdataEqualsByCodeEntity entity = new TestdataEqualsByCodeEntity("Generated Entity " + i, value);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataEqualsByCodeValue> valueList;
    private List<TestdataEqualsByCodeEntity> entityList;

    private SimpleScore score;

    public TestdataEqualsByCodeSolution() {
    }

    public TestdataEqualsByCodeSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataEqualsByCodeValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataEqualsByCodeValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataEqualsByCodeEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataEqualsByCodeEntity> entityList) {
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
