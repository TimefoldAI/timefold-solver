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
public class TestdataSingleCascadeSolution {

    public static SolutionDescriptor<TestdataSingleCascadeSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataSingleCascadeSolution.class,
                TestdataSingleCascadeEntity.class,
                TestdataSingleCascadeValue.class);
    }

    public static TestdataSingleCascadeSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataSingleCascadeSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataSingleCascadeEntity> entityList = IntStream.range(1, entityCount + 1)
                .mapToObj(i -> new TestdataSingleCascadeEntity("Generated Entity " + i))
                .toList();
        List<TestdataSingleCascadeValue> valueList = IntStream.range(1, valueCount + 1)
                .mapToObj(TestdataSingleCascadeValue::new)
                .toList();
        TestdataSingleCascadeSolution solution = new TestdataSingleCascadeSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataSingleCascadeValue> valueList;
    private List<TestdataSingleCascadeEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataSingleCascadeValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataSingleCascadeValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataSingleCascadeEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataSingleCascadeEntity> entityList) {
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
