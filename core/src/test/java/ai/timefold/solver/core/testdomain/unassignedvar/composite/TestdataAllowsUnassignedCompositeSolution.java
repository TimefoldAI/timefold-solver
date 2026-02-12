package ai.timefold.solver.core.testdomain.unassignedvar.composite;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataAllowsUnassignedCompositeSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataAllowsUnassignedCompositeSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataAllowsUnassignedCompositeSolution.class,
                TestdataAllowsUnassignedCompositeEntity.class);
    }

    public static TestdataAllowsUnassignedCompositeSolution generateSolution(int valueListSize, int entityListSize) {
        TestdataAllowsUnassignedCompositeSolution solution =
                new TestdataAllowsUnassignedCompositeSolution("Generated Solution 0");
        List<TestdataValue> valueList = new ArrayList<>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            TestdataValue value = new TestdataValue("Generated Value " + i);
            valueList.add(value);
        }
        List<TestdataValue> otherValueList = new ArrayList<>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            TestdataValue value = new TestdataValue("Generated Value " + (valueListSize + i));
            otherValueList.add(value);
        }
        solution.setValueList(valueList);
        solution.setOtherValueList(otherValueList);
        List<TestdataAllowsUnassignedCompositeEntity> entityList = new ArrayList<>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            TestdataAllowsUnassignedCompositeEntity entity =
                    new TestdataAllowsUnassignedCompositeEntity("Generated Entity " + i);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataValue> valueList;
    private List<TestdataValue> otherValueList;
    private List<TestdataAllowsUnassignedCompositeEntity> entityList;

    private SimpleScore score;

    public TestdataAllowsUnassignedCompositeSolution() {
        // Required for cloning
    }

    public TestdataAllowsUnassignedCompositeSolution(String code) {
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
    public List<TestdataAllowsUnassignedCompositeEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataAllowsUnassignedCompositeEntity> entityList) {
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
