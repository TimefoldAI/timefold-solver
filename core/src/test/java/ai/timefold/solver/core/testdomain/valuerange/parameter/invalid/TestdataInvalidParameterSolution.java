package ai.timefold.solver.core.testdomain.valuerange.parameter.invalid;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataInvalidParameterSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataInvalidParameterSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataInvalidParameterSolution.class,
                TestdataInvalidParameterEntity.class);
    }

    private List<TestdataInvalidParameterEntity> entityList;
    private List<TestdataValue> valueList;

    private SimpleScore score;

    public TestdataInvalidParameterSolution() {
        // Required for cloning
    }

    public TestdataInvalidParameterSolution(String code) {
        super(code);
    }

    @PlanningEntityCollectionProperty
    public List<TestdataInvalidParameterEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataInvalidParameterEntity> entityList) {
        this.entityList = entityList;
    }

    @ValueRangeProvider(id = "valueRange")
    public List<TestdataValue> getValueList(TestdataInvalidParameterSolution solution) {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
