package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.supply;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.TestdataCascadingBaseSolution;

@PlanningSolution
public class TestdataMultipleCascadingWithSupplySolution
        implements
        TestdataCascadingBaseSolution<TestdataMultipleCascadingWithSupplyEntity, TestdataMultipleCascadingWithSupplyValue> {

    public static SolutionDescriptor<TestdataMultipleCascadingWithSupplySolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataMultipleCascadingWithSupplySolution.class,
                TestdataMultipleCascadingWithSupplyEntity.class,
                TestdataMultipleCascadingWithSupplyValue.class);
    }

    public static TestdataMultipleCascadingWithSupplySolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataMultipleCascadingWithSupplySolution generateSolution(int valueCount, int entityCount) {
        List<TestdataMultipleCascadingWithSupplyEntity> entityList = IntStream.range(1, entityCount + 1)
                .mapToObj(i -> new TestdataMultipleCascadingWithSupplyEntity("Generated Entity " + i))
                .toList();
        List<TestdataMultipleCascadingWithSupplyValue> valueList = IntStream.range(1, valueCount + 1)
                .mapToObj(TestdataMultipleCascadingWithSupplyValue::new)
                .toList();
        TestdataMultipleCascadingWithSupplySolution solution = new TestdataMultipleCascadingWithSupplySolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataMultipleCascadingWithSupplyValue> valueList;
    private List<TestdataMultipleCascadingWithSupplyEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    @Override
    public List<TestdataMultipleCascadingWithSupplyValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataMultipleCascadingWithSupplyValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    @Override
    public List<TestdataMultipleCascadingWithSupplyEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataMultipleCascadingWithSupplyEntity> entityList) {
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
