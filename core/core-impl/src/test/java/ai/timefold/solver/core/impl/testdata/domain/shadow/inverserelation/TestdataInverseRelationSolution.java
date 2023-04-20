package ai.timefold.solver.core.impl.testdata.domain.shadow.inverserelation;

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
public class TestdataInverseRelationSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataInverseRelationSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataInverseRelationSolution.class,
                TestdataInverseRelationEntity.class, TestdataInverseRelationValue.class);
    }

    private List<TestdataInverseRelationValue> valueList;
    private List<TestdataInverseRelationEntity> entityList;

    private SimpleScore score;

    public TestdataInverseRelationSolution() {
    }

    public TestdataInverseRelationSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataInverseRelationValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataInverseRelationValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataInverseRelationEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataInverseRelationEntity> entityList) {
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
