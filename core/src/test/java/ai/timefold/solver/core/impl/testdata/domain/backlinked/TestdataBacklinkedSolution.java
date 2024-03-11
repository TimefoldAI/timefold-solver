package ai.timefold.solver.core.impl.testdata.domain.backlinked;

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
public class TestdataBacklinkedSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataBacklinkedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataBacklinkedSolution.class, TestdataBacklinkedEntity.class);
    }

    public static TestdataBacklinkedSolution generateSolution() {
        return generateSolution(5, 7);
    }

    public static TestdataBacklinkedSolution generateSolution(int valueListSize, int entityListSize) {
        TestdataBacklinkedSolution solution = new TestdataBacklinkedSolution("Generated Solution 0");
        List<TestdataValue> valueList = new ArrayList<>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            TestdataValue value = new TestdataValue("Generated Value " + i);
            valueList.add(value);
        }
        solution.setValueList(valueList);
        List<TestdataBacklinkedEntity> entityList = new ArrayList<>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            TestdataValue value = valueList.get(i % valueListSize);
            TestdataBacklinkedEntity entity = new TestdataBacklinkedEntity(solution, "Generated Entity " + i, value);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataValue> valueList;
    private List<TestdataBacklinkedEntity> entityList;

    private SimpleScore score;

    public TestdataBacklinkedSolution() {
    }

    public TestdataBacklinkedSolution(String code) {
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
    public List<TestdataBacklinkedEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataBacklinkedEntity> entityList) {
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
