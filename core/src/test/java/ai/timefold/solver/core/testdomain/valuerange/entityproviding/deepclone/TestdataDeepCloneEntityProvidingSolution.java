package ai.timefold.solver.core.testdomain.valuerange.entityproviding.deepclone;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataDeepCloneEntityProvidingSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataDeepCloneEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataDeepCloneEntityProvidingSolution.class,
                TestdataDeepCloneEntityProvidingEntity.class);
    }

    public static PlanningSolutionMetaModel<TestdataDeepCloneEntityProvidingSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    public static TestdataDeepCloneEntityProvidingSolution generateSolution() {
        var solution = new TestdataDeepCloneEntityProvidingSolution("s1");
        var value1 = new TestdataValue("1");
        var value2 = new TestdataValue("2");
        var entity1 = new TestdataDeepCloneEntityProvidingEntity("1", List.of(value1, value2));
        entity1.setValue(value1);
        var entity2 = new TestdataDeepCloneEntityProvidingEntity("2", List.of(value1, value2));
        entity2.setValue(value2);
        solution.setEntityList(List.of(entity1, entity2));
        return solution;
    }

    private List<TestdataDeepCloneEntityProvidingEntity> entityList;

    private SimpleScore score;

    public TestdataDeepCloneEntityProvidingSolution() {
        // Required for cloning
    }

    public TestdataDeepCloneEntityProvidingSolution(String code) {
        super(code);
    }

    @PlanningEntityCollectionProperty
    public List<TestdataDeepCloneEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataDeepCloneEntityProvidingEntity> entityList) {
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
