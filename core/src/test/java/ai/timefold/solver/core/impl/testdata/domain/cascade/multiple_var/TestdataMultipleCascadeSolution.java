package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataMultipleCascadeSolution {

    public static SolutionDescriptor<TestdataMultipleCascadeSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataMultipleCascadeSolution.class,
                TestdataMultipleCascadeEntity.class,
                TestdataMultipleCascadeValue.class);
    }

    public static TestdataMultipleCascadeSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataMultipleCascadeSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataMultipleCascadeEntity> entityList = IntStream.range(1, entityCount + 1)
                .mapToObj(i -> new TestdataMultipleCascadeEntity("Generated Entity " + i))
                .toList();
        List<TestdataMultipleCascadeValue> valueList = IntStream.range(1, valueCount + 1)
                .mapToObj(TestdataMultipleCascadeValue::new)
                .toList();
        TestdataMultipleCascadeSolution solution = new TestdataMultipleCascadeSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataMultipleCascadeValue> valueList;
    private List<TestdataMultipleCascadeEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataMultipleCascadeValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataMultipleCascadeValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataMultipleCascadeEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataMultipleCascadeEntity> entityList) {
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
