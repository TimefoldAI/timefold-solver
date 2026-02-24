package ai.timefold.solver.core.testdomain.valuerange.entityproviding.hashCode;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataEntityProvidingHashCodeSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataEntityProvidingHashCodeSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataEntityProvidingHashCodeSolution.class,
                TestdataEntityProvidingHashCodeEntity.class);
    }

    public static PlanningSolutionMetaModel<TestdataEntityProvidingHashCodeSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    private List<TestdataEntityProvidingHashCodeEntity> entityList;

    private SimpleScore score;

    public TestdataEntityProvidingHashCodeSolution() {
        // Required for cloning
    }

    public TestdataEntityProvidingHashCodeSolution(String code) {
        super(code);
    }

    @PlanningEntityCollectionProperty
    public List<TestdataEntityProvidingHashCodeEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataEntityProvidingHashCodeEntity> entityList) {
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
