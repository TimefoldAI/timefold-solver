package ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.souce;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataSingleCascadingSouceSolution {

    public static SolutionDescriptor<TestdataSingleCascadingSouceSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataSingleCascadingSouceSolution.class,
                TestdataSingleCascadingSourceEntity.class,
                TestdataSingleCascadingSourceValue.class);
    }

    public static TestdataSingleCascadingSouceSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataSingleCascadingSouceSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataSingleCascadingSourceEntity> entityList = IntStream.range(1, entityCount + 1)
                .mapToObj(i -> new TestdataSingleCascadingSourceEntity("Generated Entity " + i))
                .toList();
        List<TestdataSingleCascadingSourceValue> valueList = IntStream.range(1, valueCount + 1)
                .mapToObj(TestdataSingleCascadingSourceValue::new)
                .toList();
        TestdataSingleCascadingSouceSolution solution = new TestdataSingleCascadingSouceSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataSingleCascadingSourceValue> valueList;
    private List<TestdataSingleCascadingSourceEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataSingleCascadingSourceValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataSingleCascadingSourceValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataSingleCascadingSourceEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataSingleCascadingSourceEntity> entityList) {
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
