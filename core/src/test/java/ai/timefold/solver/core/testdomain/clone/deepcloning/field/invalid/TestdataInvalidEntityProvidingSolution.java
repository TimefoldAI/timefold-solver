package ai.timefold.solver.core.testdomain.clone.deepcloning.field.invalid;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataInvalidEntityProvidingSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataInvalidEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataInvalidEntityProvidingSolution.class,
                TestdataInvalidEntityProvidingEntity.class);
    }

    public static PlanningSolutionMetaModel<TestdataInvalidEntityProvidingSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    public static TestdataInvalidEntityProvidingSolution generateSolution() {
        var solution = new TestdataInvalidEntityProvidingSolution("s1");
        var value1 = new TestdataValue("1");
        var value2 = new TestdataValue("2");
        var entity1 = new TestdataInvalidEntityProvidingEntity("1", List.of(value1, value2));
        entity1.setValue(value1);
        var entity2 = new TestdataInvalidEntityProvidingEntity("2", List.of(value1, value2));
        entity2.setValue(value2);
        solution.setEntityList(List.of(entity1, entity2));
        return solution;
    }

    private List<TestdataInvalidEntityProvidingEntity> entityList;

    private SimpleScore score;

    public TestdataInvalidEntityProvidingSolution() {
        // Required for cloning
    }

    public TestdataInvalidEntityProvidingSolution(String code) {
        super(code);
    }

    @PlanningEntityCollectionProperty
    public List<TestdataInvalidEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataInvalidEntityProvidingEntity> entityList) {
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
