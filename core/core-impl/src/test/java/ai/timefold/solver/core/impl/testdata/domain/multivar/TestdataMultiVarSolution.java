package ai.timefold.solver.core.impl.testdata.domain.multivar;

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
public class TestdataMultiVarSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataMultiVarSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMultiVarSolution.class, TestdataMultiVarEntity.class);
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
