package ai.timefold.solver.core.impl.testdata.domain.cascade.different_var;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataDifferentCascadingSolution {

    public static SolutionDescriptor<TestdataDifferentCascadingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataDifferentCascadingSolution.class,
                TestdataDifferentCascadingEntity.class,
                TestdataDifferentCascadingValue.class);
    }

    public static TestdataDifferentCascadingSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataDifferentCascadingSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataDifferentCascadingEntity> entityList = IntStream.range(1, entityCount + 1)
                .mapToObj(i -> new TestdataDifferentCascadingEntity("Generated Entity " + i))
                .toList();
        List<TestdataDifferentCascadingValue> valueList = IntStream.range(1, valueCount + 1)
                .mapToObj(TestdataDifferentCascadingValue::new)
                .toList();
        TestdataDifferentCascadingSolution solution = new TestdataDifferentCascadingSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataDifferentCascadingValue> valueList;
    private List<TestdataDifferentCascadingEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataDifferentCascadingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataDifferentCascadingValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataDifferentCascadingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataDifferentCascadingEntity> entityList) {
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
