package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.piggyback_notifiable;

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
public class TestdataPiggybackNotifiableCascadingSolution
        implements TestdataCascadingBaseSolution<TestdataPiggybackNotifiableCascadingEntity, TestdataPiggybackNotifiableCascadingValue> {

    public static SolutionDescriptor<TestdataPiggybackNotifiableCascadingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataPiggybackNotifiableCascadingSolution.class,
                TestdataPiggybackNotifiableCascadingEntity.class,
                TestdataPiggybackNotifiableCascadingValue.class);
    }

    public static TestdataPiggybackNotifiableCascadingSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataPiggybackNotifiableCascadingSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataPiggybackNotifiableCascadingEntity> entityList = IntStream.range(1, entityCount + 1)
                .mapToObj(i -> new TestdataPiggybackNotifiableCascadingEntity("Generated Entity " + i))
                .toList();
        List<TestdataPiggybackNotifiableCascadingValue> valueList = IntStream.range(1, valueCount + 1)
                .mapToObj(TestdataPiggybackNotifiableCascadingValue::new)
                .toList();
        TestdataPiggybackNotifiableCascadingSolution solution = new TestdataPiggybackNotifiableCascadingSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataPiggybackNotifiableCascadingValue> valueList;
    private List<TestdataPiggybackNotifiableCascadingEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    @Override
    public List<TestdataPiggybackNotifiableCascadingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataPiggybackNotifiableCascadingValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    @Override
    public List<TestdataPiggybackNotifiableCascadingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataPiggybackNotifiableCascadingEntity> entityList) {
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
