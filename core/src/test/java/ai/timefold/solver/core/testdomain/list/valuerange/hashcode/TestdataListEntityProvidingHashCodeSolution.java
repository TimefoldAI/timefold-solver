package ai.timefold.solver.core.testdomain.list.valuerange.hashcode;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataListEntityProvidingHashCodeSolution {

    public static SolutionDescriptor<TestdataListEntityProvidingHashCodeSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataListEntityProvidingHashCodeSolution.class,
                TestdataListEntityProvidingHashCodeEntity.class);
    }

    public static PlanningSolutionMetaModel<TestdataListEntityProvidingHashCodeSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    private List<TestdataListEntityProvidingHashCodeEntity> entityList;

    private SimpleScore score;

    @PlanningEntityCollectionProperty
    public List<TestdataListEntityProvidingHashCodeEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListEntityProvidingHashCodeEntity> entityList) {
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
