package ai.timefold.solver.core.impl.testdata.domain.list.pinned.boxed;

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
public class TestdataBoxedPinnedWithIndexListSolution {

    public static SolutionDescriptor<TestdataBoxedPinnedWithIndexListSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataBoxedPinnedWithIndexListSolution.class,
                TestdataBoxedPinnedWithIndexListEntity.class,
                TestdataBoxedPinnedWithIndexListValue.class);
    }

    public static TestdataBoxedPinnedWithIndexListSolution generateInitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount).initialize();
    }

    public static TestdataBoxedPinnedWithIndexListSolution generateUninitializedSolution(int valueCount, int entityCount) {
        return generateSolution(valueCount, entityCount);
    }

    private static TestdataBoxedPinnedWithIndexListSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataBoxedPinnedWithIndexListEntity> entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataBoxedPinnedWithIndexListEntity("Generated Entity " + i))
                .collect(Collectors.toList());
        List<TestdataBoxedPinnedWithIndexListValue> valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataBoxedPinnedWithIndexListValue("Generated Value " + i))
                .collect(Collectors.toList());
        TestdataBoxedPinnedWithIndexListSolution solution = new TestdataBoxedPinnedWithIndexListSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataBoxedPinnedWithIndexListValue> valueList;
    private List<TestdataBoxedPinnedWithIndexListEntity> entityList;
    private SimpleScore score;

    private TestdataBoxedPinnedWithIndexListSolution initialize() {
        for (int i = 0; i < valueList.size(); i++) {
            entityList.get(i % entityList.size()).getValueList().add(valueList.get(i));
        }
        entityList.forEach(TestdataBoxedPinnedWithIndexListEntity::setUpShadowVariables);
        return this;
    }

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataBoxedPinnedWithIndexListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataBoxedPinnedWithIndexListValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataBoxedPinnedWithIndexListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataBoxedPinnedWithIndexListEntity> entityList) {
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
