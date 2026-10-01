package ai.timefold.solver.core.testdomain.list.pinned.unassignedvar;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataPinnedAllowsUnassignedValuesListSolution {

    public static SolutionDescriptor<TestdataPinnedAllowsUnassignedValuesListSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataPinnedAllowsUnassignedValuesListSolution.class,
                TestdataPinnedAllowsUnassignedValuesListEntity.class,
                TestdataPinnedAllowsUnassignedValuesListValue.class);
    }

    public static PlanningSolutionMetaModel<TestdataPinnedAllowsUnassignedValuesListSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    public static TestdataPinnedAllowsUnassignedValuesListSolution generateUninitializedSolution(int valueCount,
            int entityCount) {
        var entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataPinnedAllowsUnassignedValuesListEntity("Generated Entity " + i))
                .toList();
        var valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataPinnedAllowsUnassignedValuesListValue("Generated Value " + i))
                .toList();
        var solution = new TestdataPinnedAllowsUnassignedValuesListSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataPinnedAllowsUnassignedValuesListValue> valueList;
    private List<TestdataPinnedAllowsUnassignedValuesListEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataPinnedAllowsUnassignedValuesListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataPinnedAllowsUnassignedValuesListValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataPinnedAllowsUnassignedValuesListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataPinnedAllowsUnassignedValuesListEntity> entityList) {
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
