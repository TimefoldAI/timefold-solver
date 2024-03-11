package ai.timefold.solver.core.impl.testdata.domain.list.pinned.index;

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
public class TestdataPinnedWithIndexListSolution {

    public static SolutionDescriptor<TestdataPinnedWithIndexListSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataPinnedWithIndexListSolution.class,
                TestdataPinnedWithIndexListEntity.class,
                TestdataPinnedWithIndexListValue.class);
    }

    public static TestdataPinnedWithIndexListSolution generateInitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount).initialize();
    }

    public static TestdataPinnedWithIndexListSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataPinnedWithIndexListSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataPinnedWithIndexListEntity> entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataPinnedWithIndexListEntity("Generated Entity " + i))
                .collect(Collectors.toList());
        List<TestdataPinnedWithIndexListValue> valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataPinnedWithIndexListValue("Generated Value " + i))
                .collect(Collectors.toList());
        TestdataPinnedWithIndexListSolution solution = new TestdataPinnedWithIndexListSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataPinnedWithIndexListValue> valueList;
    private List<TestdataPinnedWithIndexListEntity> entityList;
    private SimpleScore score;

    private TestdataPinnedWithIndexListSolution initialize() {
        for (int i = 0; i < valueList.size(); i++) {
            entityList.get(i % entityList.size()).getValueList().add(valueList.get(i));
        }
        entityList.forEach(TestdataPinnedWithIndexListEntity::setUpShadowVariables);
        return this;
    }

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataPinnedWithIndexListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataPinnedWithIndexListValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataPinnedWithIndexListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataPinnedWithIndexListEntity> entityList) {
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
