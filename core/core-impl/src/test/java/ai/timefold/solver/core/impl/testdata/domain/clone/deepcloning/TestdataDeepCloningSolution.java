package ai.timefold.solver.core.impl.testdata.domain.clone.deepcloning;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningSolution
public class TestdataDeepCloningSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataDeepCloningSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataDeepCloningSolution.class, TestdataDeepCloningEntity.class);
    }

    private List<TestdataValue> valueList;
    private List<TestdataDeepCloningEntity> entityList;
    private List<String> generalShadowVariableList;

    private SimpleScore score;

    public TestdataDeepCloningSolution() {
    }

    public TestdataDeepCloningSolution(String code) {
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

    @PlanningEntityCollectionProperty
    public List<TestdataDeepCloningEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataDeepCloningEntity> entityList) {
        this.entityList = entityList;
    }

    @DeepPlanningClone
    public List<String> getGeneralShadowVariableList() {
        return generalShadowVariableList;
    }

    public void setGeneralShadowVariableList(List<String> generalShadowVariableList) {
        this.generalShadowVariableList = generalShadowVariableList;
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
