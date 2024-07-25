package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.multiple_var;

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
public class TestdataMultipleSourceCascadingSolution
        implements TestdataCascadingBaseSolution<TestdataMultipleSourceCascadingEntity, TestdataMultipleSourceCascadingValue> {

    public static SolutionDescriptor<TestdataMultipleSourceCascadingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataMultipleSourceCascadingSolution.class,
                TestdataMultipleSourceCascadingEntity.class,
                TestdataMultipleSourceCascadingValue.class);
    }

    public static TestdataMultipleSourceCascadingSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataMultipleSourceCascadingSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataMultipleSourceCascadingEntity> entityList = IntStream.range(1, entityCount + 1)
                .mapToObj(i -> new TestdataMultipleSourceCascadingEntity("Generated Entity " + i))
                .toList();
        List<TestdataMultipleSourceCascadingValue> valueList = IntStream.range(1, valueCount + 1)
                .mapToObj(TestdataMultipleSourceCascadingValue::new)
                .toList();
        TestdataMultipleSourceCascadingSolution solution = new TestdataMultipleSourceCascadingSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataMultipleSourceCascadingValue> valueList;
    private List<TestdataMultipleSourceCascadingEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    @Override
    public List<TestdataMultipleSourceCascadingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataMultipleSourceCascadingValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    @Override
    public List<TestdataMultipleSourceCascadingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataMultipleSourceCascadingEntity> entityList) {
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
