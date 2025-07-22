package ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataListUnassignedEntityProvidingSolution {

    public static SolutionDescriptor<TestdataListUnassignedEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataListUnassignedEntityProvidingSolution.class,
                TestdataListUnassignedEntityProvidingEntity.class);
    }

    public static TestdataListUnassignedEntityProvidingSolution generateSolution() {
        var solution = new TestdataListUnassignedEntityProvidingSolution();
        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var value3 = new TestdataValue("v3");
        var entity1 = new TestdataListUnassignedEntityProvidingEntity("e1", List.of(value1, value2));
        var entity2 = new TestdataListUnassignedEntityProvidingEntity("e2", List.of(value1, value3));
        solution.setEntityList(List.of(entity1, entity2));
        return solution;
    }

    private List<TestdataListUnassignedEntityProvidingEntity> entityList;

    private SimpleScore score;

    @PlanningEntityCollectionProperty
    public List<TestdataListUnassignedEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListUnassignedEntityProvidingEntity> entityList) {
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
