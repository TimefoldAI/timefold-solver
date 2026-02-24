package ai.timefold.solver.core.testdomain.valuerange.hashcode;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.clone.lookup.TestdataObjectEquals;

@PlanningSolution
public class TestdataValueRangeHashCodeSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataValueRangeHashCodeSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataValueRangeHashCodeSolution.class,
                TestdataValueRangeHashCodeEntity.class);
    }

    private List<TestdataValueRangeHashCodeEntity> entityList;
    private List<TestdataObjectEquals> valueList;

    private SimpleScore score;

    public TestdataValueRangeHashCodeSolution() {
    }

    public TestdataValueRangeHashCodeSolution(String code) {
        super(code);
    }

    @PlanningEntityCollectionProperty
    public List<TestdataValueRangeHashCodeEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataValueRangeHashCodeEntity> entityList) {
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

    @ValueRangeProvider
    public List<TestdataObjectEquals> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataObjectEquals> valueList) {
        this.valueList = valueList;
    }
}
