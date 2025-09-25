package ai.timefold.solver.core.testdomain.list.sort.compartor;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataListSortableSolution {

    public static SolutionDescriptor<TestdataListSortableSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataListSortableSolution.class,
                TestdataListSortableEntity.class,
                TestdataListSortableValue.class);
    }

    public static TestdataListSortableSolution generateSolution(int valueCount, int entityCount) {
        var entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataListSortableEntity("Generated Entity " + i, i))
                .toList();
        var valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataListSortableValue("Generated Value " + i, i))
                .toList();
        TestdataListSortableSolution solution = new TestdataListSortableSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataListSortableValue> valueList;
    private List<TestdataListSortableEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataListSortableValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListSortableValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataListSortableEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListSortableEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    public void removeEntity(TestdataListSortableEntity entity) {
        this.entityList = entityList.stream()
                .filter(e -> e != entity)
                .toList();
    }
}
