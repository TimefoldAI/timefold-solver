package ai.timefold.solver.core.impl.testdata.domain.list;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataListSolution {

    public static SolutionDescriptor<TestdataListSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataListSolution.class,
                TestdataListEntity.class,
                TestdataListValue.class);
    }

    public static TestdataListSolution generateInitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount).initialize();
    }

    public static TestdataListSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataListSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataListEntity> entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataListEntity("Generated Entity " + i))
                .toList();
        List<TestdataListValue> valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataListValue("Generated Value " + i))
                .toList();
        TestdataListSolution solution = new TestdataListSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataListValue> valueList;
    private List<TestdataListEntity> entityList;
    private SimpleScore score;

    private TestdataListSolution initialize() {
        for (int i = 0; i < valueList.size(); i++) {
            entityList.get(i % entityList.size()).getValueList().add(valueList.get(i));
        }
        entityList.forEach(TestdataListEntity::setUpShadowVariables);
        return this;
    }

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    public void removeEntity(TestdataListEntity entity) {
        this.entityList = entityList.stream()
                .filter(e -> e != entity)
                .toList();
    }
}
