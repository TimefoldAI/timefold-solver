package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.piggyback;

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
public class TestdataPiggybackCascadingSolution
        implements TestdataCascadingBaseSolution<TestdataPiggybackCascadingEntity, TestdataPiggybackCascadingValue> {

    public static SolutionDescriptor<TestdataPiggybackCascadingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataPiggybackCascadingSolution.class,
                TestdataPiggybackCascadingEntity.class,
                TestdataPiggybackCascadingValue.class);
    }

    public static TestdataPiggybackCascadingSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataPiggybackCascadingSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataPiggybackCascadingEntity> entityList = IntStream.range(1, entityCount + 1)
                .mapToObj(i -> new TestdataPiggybackCascadingEntity("Generated Entity " + i))
                .toList();
        List<TestdataPiggybackCascadingValue> valueList = IntStream.range(1, valueCount + 1)
                .mapToObj(TestdataPiggybackCascadingValue::new)
                .toList();
        TestdataPiggybackCascadingSolution solution = new TestdataPiggybackCascadingSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataPiggybackCascadingValue> valueList;
    private List<TestdataPiggybackCascadingEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    @Override
    public List<TestdataPiggybackCascadingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataPiggybackCascadingValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    @Override
    public List<TestdataPiggybackCascadingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataPiggybackCascadingEntity> entityList) {
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
