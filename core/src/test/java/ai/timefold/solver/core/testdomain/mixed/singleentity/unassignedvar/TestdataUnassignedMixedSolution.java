package ai.timefold.solver.core.testdomain.mixed.singleentity.unassignedvar;

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
public class TestdataUnassignedMixedSolution {

    public static SolutionDescriptor<TestdataUnassignedMixedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataUnassignedMixedSolution.class,
                TestdataUnassignedMixedEntity.class);
    }

    public static TestdataUnassignedMixedSolution generateUninitializedSolution(int entityListSize, int valueListSize,
            int otherValueListSize) {
        var solution = new TestdataUnassignedMixedSolution();
        var valueList = new ArrayList<TestdataUnassignedMixedValue>(valueListSize);
        var otherValueList = new ArrayList<TestdataUnassignedMixedOtherValue>(otherValueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataUnassignedMixedValue("Generated Value " + i));
        }
        for (int i = 0; i < otherValueListSize; i++) {
            otherValueList.add(new TestdataUnassignedMixedOtherValue("Generated Other Value " + i));
        }
        solution.setValueList(valueList);
        solution.setOtherValueList(otherValueList);
        var entityList = new ArrayList<TestdataUnassignedMixedEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataUnassignedMixedEntity("Entity " + i);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    private List<TestdataUnassignedMixedValue> valueList;
    @ValueRangeProvider(id = "otherValueRange")
    @ProblemFactCollectionProperty
    private List<TestdataUnassignedMixedOtherValue> otherValueList;
    @PlanningEntityCollectionProperty
    private List<TestdataUnassignedMixedEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<TestdataUnassignedMixedValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataUnassignedMixedValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataUnassignedMixedOtherValue> getOtherValueList() {
        return otherValueList;
    }

    public void setOtherValueList(List<TestdataUnassignedMixedOtherValue> otherValueList) {
        this.otherValueList = otherValueList;
    }

    public List<TestdataUnassignedMixedEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataUnassignedMixedEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
