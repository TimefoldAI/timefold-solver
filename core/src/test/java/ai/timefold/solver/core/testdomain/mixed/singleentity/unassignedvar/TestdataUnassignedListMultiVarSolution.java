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
public class TestdataUnassignedListMultiVarSolution {

    public static SolutionDescriptor<TestdataUnassignedListMultiVarSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataUnassignedListMultiVarSolution.class,
                TestdataUnassignedListMultiVarEntity.class);
    }

    public static TestdataUnassignedListMultiVarSolution generateUninitializedSolution(int entityListSize, int valueListSize,
            int otherValueListSize) {
        var solution = new TestdataUnassignedListMultiVarSolution();
        var valueList = new ArrayList<TestdataUnassignedListMultiVarValue>(valueListSize);
        var otherValueList = new ArrayList<TestdataUnassignedListMultiVarOtherValue>(otherValueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataUnassignedListMultiVarValue("Generated Value " + i));
        }
        for (int i = 0; i < otherValueListSize; i++) {
            otherValueList.add(new TestdataUnassignedListMultiVarOtherValue("Generated Other Value " + i));
        }
        solution.setValueList(valueList);
        solution.setOtherValueList(otherValueList);
        var entityList = new ArrayList<TestdataUnassignedListMultiVarEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataUnassignedListMultiVarEntity("Entity " + i);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    private List<TestdataUnassignedListMultiVarValue> valueList;
    @ValueRangeProvider(id = "otherValueRange")
    @ProblemFactCollectionProperty
    private List<TestdataUnassignedListMultiVarOtherValue> otherValueList;
    @PlanningEntityCollectionProperty
    private List<TestdataUnassignedListMultiVarEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<TestdataUnassignedListMultiVarValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataUnassignedListMultiVarValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataUnassignedListMultiVarOtherValue> getOtherValueList() {
        return otherValueList;
    }

    public void setOtherValueList(List<TestdataUnassignedListMultiVarOtherValue> otherValueList) {
        this.otherValueList = otherValueList;
    }

    public List<TestdataUnassignedListMultiVarEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataUnassignedListMultiVarEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
