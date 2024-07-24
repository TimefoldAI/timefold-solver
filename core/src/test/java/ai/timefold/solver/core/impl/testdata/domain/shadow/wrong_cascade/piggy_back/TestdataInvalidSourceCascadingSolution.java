package ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.piggy_back;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataInvalidSourceCascadingSolution {

    public static SolutionDescriptor<TestdataInvalidSourceCascadingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataInvalidSourceCascadingSolution.class,
                TestdataInvalidSourceCascadingEntity.class,
                TestdataInvalidSourceCascadingValue.class,
                TestdataInvalidSourceCascadingValue2.class);
    }

    public static TestdataInvalidSourceCascadingSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataInvalidSourceCascadingSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataInvalidSourceCascadingEntity> entityList = IntStream.range(1, entityCount + 1)
                .mapToObj(i -> new TestdataInvalidSourceCascadingEntity("Generated Entity " + i))
                .toList();
        List<TestdataInvalidSourceCascadingValue> valueList = IntStream.range(1, valueCount + 1)
                .mapToObj(TestdataInvalidSourceCascadingValue::new)
                .toList();
        TestdataInvalidSourceCascadingSolution solution = new TestdataInvalidSourceCascadingSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataInvalidSourceCascadingValue> valueList;
    private List<TestdataInvalidSourceCascadingEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataInvalidSourceCascadingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataInvalidSourceCascadingValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataInvalidSourceCascadingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataInvalidSourceCascadingEntity> entityList) {
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
