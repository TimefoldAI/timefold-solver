package ai.timefold.solver.core.impl.testdata.domain.record;

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

@PlanningSolution
public class TestdataRecordSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataRecordSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataRecordSolution.class, TestdataRecordEntity.class);
    }

    public static TestdataRecordSolution generateSolution() {
        return generateSolution(5, 7);
    }

    public static TestdataRecordSolution generateSolution(int valueListSize, int entityListSize) {
        TestdataRecordSolution solution = new TestdataRecordSolution("Generated Solution 0");
        List<TestdataRecordValue> valueList = new ArrayList<>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            TestdataRecordValue value = new TestdataRecordValue("Generated Value " + i);
            valueList.add(value);
        }
        solution.setValueList(valueList);
        List<TestdataRecordEntity> entityList = new ArrayList<>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            TestdataRecordValue value = valueList.get(i % valueListSize);
            TestdataRecordEntity entity = new TestdataRecordEntity("Generated Entity " + i, value);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataRecordValue> valueList;
    private List<TestdataRecordEntity> entityList;

    private SimpleScore score;

    public TestdataRecordSolution() {
    }

    public TestdataRecordSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataRecordValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataRecordValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataRecordEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataRecordEntity> entityList) {
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
