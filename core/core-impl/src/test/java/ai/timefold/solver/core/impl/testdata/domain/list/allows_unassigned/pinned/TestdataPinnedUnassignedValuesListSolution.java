package ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.pinned;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataPinnedUnassignedValuesListSolution {

    public static SolutionDescriptor<TestdataPinnedUnassignedValuesListSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataPinnedUnassignedValuesListSolution.class,
                TestdataPinnedUnassignedValuesListEntity.class,
                TestdataPinnedUnassignedValuesListValue.class);
    }

    private List<TestdataPinnedUnassignedValuesListValue> valueList;
    private List<TestdataPinnedUnassignedValuesListEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataPinnedUnassignedValuesListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataPinnedUnassignedValuesListValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataPinnedUnassignedValuesListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataPinnedUnassignedValuesListEntity> entityList) {
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
