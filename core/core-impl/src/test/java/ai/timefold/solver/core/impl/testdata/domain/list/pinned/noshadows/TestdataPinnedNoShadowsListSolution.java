package ai.timefold.solver.core.impl.testdata.domain.list.pinned.noshadows;

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
public class TestdataPinnedNoShadowsListSolution {

    public static SolutionDescriptor<TestdataPinnedNoShadowsListSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataPinnedNoShadowsListSolution.class,
                TestdataPinnedNoShadowsListEntity.class,
                TestdataPinnedNoShadowsListValue.class);
    }

    public static TestdataPinnedNoShadowsListSolution generateInitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount).initialize();
    }

    public static TestdataPinnedNoShadowsListSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataPinnedNoShadowsListSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataPinnedNoShadowsListEntity> entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataPinnedNoShadowsListEntity("Generated Entity " + i))
                .collect(Collectors.toList());
        List<TestdataPinnedNoShadowsListValue> valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataPinnedNoShadowsListValue("Generated Value " + i))
                .collect(Collectors.toList());
        TestdataPinnedNoShadowsListSolution solution = new TestdataPinnedNoShadowsListSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataPinnedNoShadowsListValue> valueList;
    private List<TestdataPinnedNoShadowsListEntity> entityList;
    private SimpleScore score;

    private TestdataPinnedNoShadowsListSolution initialize() {
        for (int i = 0; i < valueList.size(); i++) {
            entityList.get(i % entityList.size()).getValueList().add(valueList.get(i));
        }
        entityList.forEach(TestdataPinnedNoShadowsListEntity::setUpShadowVariables);
        return this;
    }

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataPinnedNoShadowsListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataPinnedNoShadowsListValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataPinnedNoShadowsListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataPinnedNoShadowsListEntity> entityList) {
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
