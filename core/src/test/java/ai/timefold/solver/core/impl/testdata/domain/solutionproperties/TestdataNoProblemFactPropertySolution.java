package ai.timefold.solver.core.impl.testdata.domain.solutionproperties;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningSolution
public class TestdataNoProblemFactPropertySolution extends TestdataObject {

    public static SolutionDescriptor<TestdataNoProblemFactPropertySolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataNoProblemFactPropertySolution.class, TestdataEntity.class);
    }

    private List<TestdataValue> valueList;
    private List<TestdataEntity> entityList;

    private SimpleScore score;

    public TestdataNoProblemFactPropertySolution() {
    }

    public TestdataNoProblemFactPropertySolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataEntity> entityList) {
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
