package ai.timefold.solver.core.testdomain.valuerange.entityproviding.parameter.invalid;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataInvalidCountEntityProvidingWithParameterSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataInvalidCountEntityProvidingWithParameterSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataInvalidCountEntityProvidingWithParameterSolution.class,
                TestdataInvalidCountEntityProvidingWithParameterEntity.class);
    }

    private List<TestdataInvalidCountEntityProvidingWithParameterEntity> entityList;

    private SimpleScore score;

    public TestdataInvalidCountEntityProvidingWithParameterSolution() {
        // Required for cloning
    }

    public TestdataInvalidCountEntityProvidingWithParameterSolution(String code) {
        super(code);
    }

    @PlanningEntityCollectionProperty
    public List<TestdataInvalidCountEntityProvidingWithParameterEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataInvalidCountEntityProvidingWithParameterEntity> entityList) {
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
