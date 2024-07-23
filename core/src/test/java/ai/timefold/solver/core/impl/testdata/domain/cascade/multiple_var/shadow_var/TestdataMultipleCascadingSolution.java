package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.shadow_var;

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
public class TestdataMultipleCascadingSolution
        implements TestdataCascadingBaseSolution<TestdataMultipleCascadingEntity, TestdataMultipleCascadingValue> {

    public static SolutionDescriptor<TestdataMultipleCascadingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataMultipleCascadingSolution.class,
                TestdataMultipleCascadingEntity.class,
                TestdataMultipleCascadingValue.class);
    }

    public static TestdataMultipleCascadingSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataMultipleCascadingSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataMultipleCascadingEntity> entityList = IntStream.range(1, entityCount + 1)
                .mapToObj(i -> new TestdataMultipleCascadingEntity("Generated Entity " + i))
                .toList();
        List<TestdataMultipleCascadingValue> valueList = IntStream.range(1, valueCount + 1)
                .mapToObj(TestdataMultipleCascadingValue::new)
                .toList();
        TestdataMultipleCascadingSolution solution = new TestdataMultipleCascadingSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataMultipleCascadingValue> valueList;
    private List<TestdataMultipleCascadingEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    @Override
    public List<TestdataMultipleCascadingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataMultipleCascadingValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    @Override
    public List<TestdataMultipleCascadingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataMultipleCascadingEntity> entityList) {
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
