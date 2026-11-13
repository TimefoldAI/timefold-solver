package ai.timefold.solver.core.testdomain.list.valuerange.pinned;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataListPinnedEntityProvidingSolution {

    public static SolutionDescriptor<TestdataListPinnedEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataListPinnedEntityProvidingSolution.class,
                TestdataListPinnedEntityProvidingEntity.class);
    }

    private List<TestdataListPinnedEntityProvidingEntity> entityList;

    private SimpleScore score;

    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueList() {
        return entityList.stream()
                .flatMap(entity -> entity.getValueRange().stream())
                .distinct()
                .toList();
    }

    @PlanningEntityCollectionProperty
    public List<TestdataListPinnedEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListPinnedEntityProvidingEntity> entityList) {
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
