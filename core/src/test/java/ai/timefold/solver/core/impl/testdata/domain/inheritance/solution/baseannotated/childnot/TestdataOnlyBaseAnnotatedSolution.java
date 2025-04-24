package ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childnot;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningSolution
public class TestdataOnlyBaseAnnotatedSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataOnlyBaseAnnotatedSolution> buildBaseSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataOnlyBaseAnnotatedSolution.class, TestdataEntity.class);
    }

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "valueRange")
    private List<TestdataValue> valueList;
    @PlanningEntityCollectionProperty
    private List<TestdataEntity> entityList;
    @PlanningScore
    private SimpleScore score;
    private ConstraintWeightOverrides<SimpleScore> constraintWeightOverrides;

    public TestdataOnlyBaseAnnotatedSolution() {
    }

    public TestdataOnlyBaseAnnotatedSolution(String code) {
        super(code);
    }

    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    public List<? extends TestdataEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<? extends TestdataEntity> entityList) {
        this.entityList = (List<TestdataEntity>) entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    public ConstraintWeightOverrides<SimpleScore> getConstraintWeightOverrides() {
        return constraintWeightOverrides;
    }

    public void setConstraintWeightOverrides(ConstraintWeightOverrides<SimpleScore> constraintWeightOverrides) {
        this.constraintWeightOverrides = constraintWeightOverrides;
    }
}
