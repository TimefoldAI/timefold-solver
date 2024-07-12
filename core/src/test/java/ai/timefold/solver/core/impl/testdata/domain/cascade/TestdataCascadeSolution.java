package ai.timefold.solver.core.impl.testdata.domain.cascade;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataCascadeSolution {

    public static SolutionDescriptor<TestdataCascadeSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataCascadeSolution.class,
                TestdataCascadeEntity.class,
                TestdataCascadeValue.class);
    }

    public static TestdataCascadeSolution generateInitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount).initialize();
    }

    public static TestdataCascadeSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataCascadeSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataCascadeEntity> entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataCascadeEntity("Generated Entity " + i))
                .toList();
        List<TestdataCascadeValue> valueList = IntStream.range(0, valueCount)
                .mapToObj(TestdataCascadeValue::new)
                .toList();
        TestdataCascadeSolution solution = new TestdataCascadeSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataCascadeValue> valueList;
    private List<TestdataCascadeEntity> entityList;
    private SimpleScore score;

    private TestdataCascadeSolution initialize() {
        for (int i = 0; i < valueList.size(); i++) {
            entityList.get(i % entityList.size()).getValueList().add(valueList.get(i));
        }
        entityList.forEach(TestdataCascadeEntity::setUpShadowVariables);
        return this;
    }

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataCascadeValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataCascadeValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataCascadeEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataCascadeEntity> entityList) {
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
