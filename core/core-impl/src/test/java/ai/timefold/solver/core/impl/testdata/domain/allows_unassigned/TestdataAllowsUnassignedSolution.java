package ai.timefold.solver.core.impl.testdata.domain.allows_unassigned;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningSolution
public class TestdataAllowsUnassignedSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataAllowsUnassignedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataAllowsUnassignedSolution.class,
                TestdataAllowsUnassignedEntity.class);
    }

    public static TestdataAllowsUnassignedSolution generateSolution() {
        return generateSolution(2, 2);
    }

    public static TestdataAllowsUnassignedSolution generateSolution(int valueListSize, int entityListSize) {
        TestdataAllowsUnassignedSolution solution = new TestdataAllowsUnassignedSolution("Generated Solution 0");
        List<TestdataValue> valueList = new ArrayList<>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            TestdataValue value = new TestdataValue("Generated Value " + i);
            valueList.add(value);
        }
        solution.setValueList(valueList);
        List<TestdataAllowsUnassignedEntity> entityList = new ArrayList<>(entityListSize);
        entityList.add(new TestdataAllowsUnassignedEntity("Generated Entity 0", null));
        for (int i = 1; i < entityListSize; i++) {
            TestdataValue value = valueList.get(i % valueListSize);
            TestdataAllowsUnassignedEntity entity = new TestdataAllowsUnassignedEntity("Generated Entity " + i, value);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataValue> valueList;
    private List<TestdataAllowsUnassignedEntity> entityList;

    private SimpleScore score;

    public TestdataAllowsUnassignedSolution() {
    }

    public TestdataAllowsUnassignedSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataAllowsUnassignedEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataAllowsUnassignedEntity> entityList) {
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
