package ai.timefold.solver.core.testdomain.list.valuerange.compartor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataListSortableEntityProvidingSolution {

    public static SolutionDescriptor<TestdataListSortableEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataListSortableEntityProvidingSolution.class,
                TestdataListSortableEntityProvidingEntity.class,
                TestdataListSortableEntityProvidingValue.class);
    }

    public static TestdataListSortableEntityProvidingSolution generateSolution(int valueCount, int entityCount) {
        var entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataListSortableEntityProvidingEntity("Generated Entity " + i, i))
                .toList();
        var valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataListSortableEntityProvidingValue("Generated Value " + i, i))
                .toList();
        var solution = new TestdataListSortableEntityProvidingSolution();
        var random = new Random(0);
        for (var entity : entityList) {
            var valueRange = new ArrayList<>(valueList);
            Collections.shuffle(valueRange, random);
            entity.setValueRange(valueRange);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataListSortableEntityProvidingEntity> entityList;
    private SimpleScore score;

    @PlanningEntityCollectionProperty
    public List<TestdataListSortableEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListSortableEntityProvidingEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    public void removeEntity(TestdataListSortableEntityProvidingEntity entity) {
        this.entityList = entityList.stream()
                .filter(e -> e != entity)
                .toList();
    }
}
