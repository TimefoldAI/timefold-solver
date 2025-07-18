package ai.timefold.solver.core.testdomain.list.valuerange;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataListEntityProvidingSolution {

    public static SolutionDescriptor<TestdataListEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataListEntityProvidingSolution.class,
                TestdataListEntityProvidingEntity.class);
    }

    private List<TestdataListEntityProvidingEntity> entityList;
    private List<TestdataListEntityProvidingValue> temporaryValueList;

    private SimpleScore score;

    @PlanningEntityCollectionProperty
    public List<TestdataListEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListEntityProvidingEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    public List<TestdataListEntityProvidingValue> getTemporaryValueList() {
        return temporaryValueList;
    }

    public void setTemporaryValueList(List<TestdataListEntityProvidingValue> temporaryValueList) {
        this.temporaryValueList = temporaryValueList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
