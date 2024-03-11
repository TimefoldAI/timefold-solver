package ai.timefold.solver.core.impl.testdata.domain.list.pinned;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataPinnedListSolution {

    public static SolutionDescriptor<TestdataPinnedListSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataPinnedListSolution.class,
                TestdataPinnedListEntity.class,
                TestdataPinnedListValue.class);
    }

    public static TestdataPinnedListSolution generateInitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount).initialize();
    }

    public static TestdataPinnedListSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataPinnedListSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataPinnedListEntity> entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataPinnedListEntity("Generated Entity " + i))
                .collect(Collectors.toList());
        List<TestdataPinnedListValue> valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataPinnedListValue("Generated Value " + i))
                .collect(Collectors.toList());
        TestdataPinnedListSolution solution = new TestdataPinnedListSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataPinnedListValue> valueList;
    private List<TestdataPinnedListEntity> entityList;
    private SimpleScore score;

    private TestdataPinnedListSolution initialize() {
        for (int i = 0; i < valueList.size(); i++) {
            entityList.get(i % entityList.size()).getValueList().add(valueList.get(i));
        }
        entityList.forEach(TestdataPinnedListEntity::setUpShadowVariables);
        return this;
    }

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataPinnedListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataPinnedListValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataPinnedListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataPinnedListEntity> entityList) {
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
