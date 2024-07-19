package ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.suply;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataSingleCascadingWithSupplySolution {

    public static SolutionDescriptor<TestdataSingleCascadingWithSupplySolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataSingleCascadingWithSupplySolution.class,
                TestdataSingleCascadingWithSupplyEntity.class,
                TestdataSingleCascadingWithSupplyValue.class);
    }

    public static TestdataSingleCascadingWithSupplySolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataSingleCascadingWithSupplySolution generateSolution(int valueCount, int entityCount) {
        List<TestdataSingleCascadingWithSupplyEntity> entityList = IntStream.range(1, entityCount + 1)
                .mapToObj(i -> new TestdataSingleCascadingWithSupplyEntity("Generated Entity " + i))
                .toList();
        List<TestdataSingleCascadingWithSupplyValue> valueList = IntStream.range(1, valueCount + 1)
                .mapToObj(TestdataSingleCascadingWithSupplyValue::new)
                .toList();
        TestdataSingleCascadingWithSupplySolution solution = new TestdataSingleCascadingWithSupplySolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataSingleCascadingWithSupplyValue> valueList;
    private List<TestdataSingleCascadingWithSupplyEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataSingleCascadingWithSupplyValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataSingleCascadingWithSupplyValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataSingleCascadingWithSupplyEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataSingleCascadingWithSupplyEntity> entityList) {
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
