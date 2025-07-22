package ai.timefold.solver.core.testdomain.list.valuerange.composite;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

@PlanningSolution
public class TestdataListCompositeEntityProvidingSolution {

    public static SolutionDescriptor<TestdataListCompositeEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataListCompositeEntityProvidingSolution.class,
                TestdataListCompositeEntityProvidingEntity.class);
    }

    public static TestdataListCompositeEntityProvidingSolution generateSolution() {
        var solution = new TestdataListCompositeEntityProvidingSolution();
        var value1 = new TestdataListEntityProvidingValue("v1");
        var value2 = new TestdataListEntityProvidingValue("v2");
        var value3 = new TestdataListEntityProvidingValue("v3");
        var value4 = new TestdataListEntityProvidingValue("v4");
        var value5 = new TestdataListEntityProvidingValue("v5");
        var entity1 = new TestdataListCompositeEntityProvidingEntity("e1", List.of(value1, value2), List.of(value1, value3));
        var entity2 = new TestdataListCompositeEntityProvidingEntity("e2", List.of(value1, value4), List.of(value1, value5));
        solution.setEntityList(List.of(entity1, entity2));
        return solution;
    }

    private List<TestdataListCompositeEntityProvidingEntity> entityList;
    private SimpleScore score;

    @PlanningEntityCollectionProperty
    public List<TestdataListCompositeEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListCompositeEntityProvidingEntity> entityList) {
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
