package ai.timefold.solver.core.testdomain.composite;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataCompositeSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataCompositeSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataCompositeSolution.class, TestdataCompositeEntity.class);
    }

    public static TestdataCompositeSolution generateSolution(int valueListSize, int entityListSize) {
        TestdataCompositeSolution solution = new TestdataCompositeSolution("Generated Solution 0");
        List<TestdataValue> valueList = new ArrayList<>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            TestdataValue value = new TestdataValue("Generated Value " + i);
            valueList.add(value);
        }
        List<TestdataValue> otherValueList = new ArrayList<>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            TestdataValue value = new TestdataValue("Generated Value " + (valueListSize + i - 1));
            otherValueList.add(value);
        }
        solution.setValueList(valueList);
        solution.setOtherValueList(otherValueList);
        List<TestdataCompositeEntity> entityList = new ArrayList<>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            TestdataCompositeEntity entity = new TestdataCompositeEntity("Generated Entity " + i);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataValue> valueList;
    private List<TestdataValue> otherValueList;
    private List<TestdataCompositeEntity> entityList;

    private SimpleScore score;

    public TestdataCompositeSolution() {
        // Required for cloning
    }

    public TestdataCompositeSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange1")
    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    @ValueRangeProvider(id = "valueRange2")
    @ProblemFactCollectionProperty
    public List<TestdataValue> getOtherValueList() {
        return otherValueList;
    }

    public void setOtherValueList(List<TestdataValue> otherValueList) {
        this.otherValueList = otherValueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataCompositeEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataCompositeEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
