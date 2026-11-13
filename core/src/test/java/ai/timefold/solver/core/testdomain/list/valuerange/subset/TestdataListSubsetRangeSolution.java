package ai.timefold.solver.core.testdomain.list.valuerange.subset;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.testdomain.TestdataValue;

/**
 * The value range is a strict subset of the values in the solution:
 * a value outside it is a problem fact, never assigned, and must never be assigned.
 */
@PlanningSolution
public class TestdataListSubsetRangeSolution {

    public static SolutionDescriptor<TestdataListSubsetRangeSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataListSubsetRangeSolution.class,
                TestdataListSubsetRangeEntity.class);
    }

    public static PlanningSolutionMetaModel<TestdataListSubsetRangeSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    private List<TestdataValue> valueList;
    private List<TestdataValue> valueRangeList;
    private List<TestdataListSubsetRangeEntity> entityList;
    private SimpleScore score;

    public TestdataListSubsetRangeSolution() {
    }

    public TestdataListSubsetRangeSolution(List<TestdataValue> valueList, List<TestdataValue> valueRangeList,
            List<TestdataListSubsetRangeEntity> entityList) {
        this.valueList = valueList;
        this.valueRangeList = valueRangeList;
        this.entityList = entityList;
    }

    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueList() {
        return valueList;
    }

    @ValueRangeProvider(id = "valueRange")
    public List<TestdataValue> getValueRangeList() {
        return valueRangeList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataListSubsetRangeEntity> getEntityList() {
        return entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
