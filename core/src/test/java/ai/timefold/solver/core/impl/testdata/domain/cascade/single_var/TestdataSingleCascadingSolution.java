package ai.timefold.solver.core.impl.testdata.domain.cascade.single_var;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataSingleCascadingSolution {

    public static SolutionDescriptor<TestdataSingleCascadingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataSingleCascadingSolution.class,
                TestdataSingleCascadingEntity.class,
                TestdataSingleCascadingValue.class);
    }

    public static TestdataSingleCascadingSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataSingleCascadingSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataSingleCascadingEntity> entityList = IntStream.range(1, entityCount + 1)
                .mapToObj(i -> new TestdataSingleCascadingEntity("Generated Entity " + i))
                .toList();
        List<TestdataSingleCascadingValue> valueList = IntStream.range(1, valueCount + 1)
                .mapToObj(TestdataSingleCascadingValue::new)
                .toList();
        TestdataSingleCascadingSolution solution = new TestdataSingleCascadingSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataSingleCascadingValue> valueList;
    private List<TestdataSingleCascadingEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataSingleCascadingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataSingleCascadingValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataSingleCascadingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataSingleCascadingEntity> entityList) {
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
