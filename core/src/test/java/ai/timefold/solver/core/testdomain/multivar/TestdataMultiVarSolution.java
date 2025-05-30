package ai.timefold.solver.core.testdomain.multivar;

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
public class TestdataMultiVarSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataMultiVarSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMultiVarSolution.class, TestdataMultiVarEntity.class);
    }

    public static TestdataMultiVarSolution generateUninitializedSolution(int entityListSize, int valueListSize) {
        var solution = new TestdataMultiVarSolution();
        var valueList = new ArrayList<TestdataValue>(valueListSize);
        var otherValueList = new ArrayList<TestdataOtherValue>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataValue("Generated Value " + i));
            otherValueList.add(new TestdataOtherValue("Generated Other Value " + i));
        }
        solution.setValueList(valueList);
        solution.setOtherValueList(otherValueList);
        var entityList = new ArrayList<TestdataMultiVarEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataMultiVarEntity("Entity " + i);
            entityList.add(entity);
        }
        solution.setMultiVarEntityList(entityList);
        return solution;
    }

    private List<TestdataValue> valueList;
    private List<TestdataOtherValue> otherValueList;
    private List<TestdataMultiVarEntity> multiVarEntityList;

    private SimpleScore score;

    public TestdataMultiVarSolution() {
    }

    public TestdataMultiVarSolution(String code) {
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

    @ValueRangeProvider(id = "otherValueRange")
    @ProblemFactCollectionProperty
    public List<TestdataOtherValue> getOtherValueList() {
        return otherValueList;
    }

    public void setOtherValueList(List<TestdataOtherValue> otherValueList) {
        this.otherValueList = otherValueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataMultiVarEntity> getMultiVarEntityList() {
        return multiVarEntityList;
    }

    public void setMultiVarEntityList(List<TestdataMultiVarEntity> multiVarEntityList) {
        this.multiVarEntityList = multiVarEntityList;
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
