package ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataAllowsUnassignedValuesListSolution {

    public static SolutionDescriptor<TestdataAllowsUnassignedValuesListSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataAllowsUnassignedValuesListSolution.class,
                TestdataAllowsUnassignedValuesListEntity.class,
                TestdataAllowsUnassignedValuesListValue.class);
    }

    private List<TestdataAllowsUnassignedValuesListValue> valueList;
    private List<TestdataAllowsUnassignedValuesListEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataAllowsUnassignedValuesListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataAllowsUnassignedValuesListValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataAllowsUnassignedValuesListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataAllowsUnassignedValuesListEntity> entityList) {
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
