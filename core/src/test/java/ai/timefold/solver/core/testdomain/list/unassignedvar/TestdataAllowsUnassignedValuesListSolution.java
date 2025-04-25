package ai.timefold.solver.core.testdomain.list.unassignedvar;

import java.util.List;
import java.util.stream.IntStream;

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

    public static TestdataAllowsUnassignedValuesListSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataAllowsUnassignedValuesListSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataAllowsUnassignedValuesListEntity> entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataAllowsUnassignedValuesListEntity("Generated Entity " + i))
                .toList();
        List<TestdataAllowsUnassignedValuesListValue> valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataAllowsUnassignedValuesListValue("Generated Value " + i))
                .toList();
        TestdataAllowsUnassignedValuesListSolution solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
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
