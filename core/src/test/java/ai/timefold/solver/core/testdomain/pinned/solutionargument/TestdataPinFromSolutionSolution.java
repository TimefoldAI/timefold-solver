package ai.timefold.solver.core.testdomain.pinned.solutionargument;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataPinFromSolutionSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataPinFromSolutionSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataPinFromSolutionSolution.class,
                TestdataPinFromSolutionEntity.class);
    }

    private List<TestdataValue> valueList;
    private List<TestdataPinFromSolutionEntity> entityList;
    private int planningWindowStart;
    private SimpleScore score;

    public TestdataPinFromSolutionSolution() {
    }

    public TestdataPinFromSolutionSolution(String code) {
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
    public List<TestdataPinFromSolutionEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataPinFromSolutionEntity> entityList) {
        this.entityList = entityList;
    }

    public int getPlanningWindowStart() {
        return planningWindowStart;
    }

    public void setPlanningWindowStart(int planningWindowStart) {
        this.planningWindowStart = planningWindowStart;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
