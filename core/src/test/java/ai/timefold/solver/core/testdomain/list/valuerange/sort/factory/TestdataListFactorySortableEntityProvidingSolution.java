package ai.timefold.solver.core.testdomain.list.valuerange.sort.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataListFactorySortableEntityProvidingSolution {

    public static SolutionDescriptor<TestdataListFactorySortableEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataListFactorySortableEntityProvidingSolution.class,
                TestdataListFactorySortableEntityProvidingEntity.class,
                TestdataListFactorySortableEntityProvidingValue.class);
    }

    public static TestdataListFactorySortableEntityProvidingSolution generateSolution(int valueCount, int entityCount) {
        var entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataListFactorySortableEntityProvidingEntity("Generated Entity " + i, i))
                .toList();
        var valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataListFactorySortableEntityProvidingValue("Generated Value " + i, i))
                .toList();
        var solution = new TestdataListFactorySortableEntityProvidingSolution();
        var random = new Random(0);
        for (var entity : entityList) {
            var valueRange = new ArrayList<>(valueList);
            Collections.shuffle(valueRange, random);
            entity.setValueRange(valueRange);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataListFactorySortableEntityProvidingEntity> entityList;
    private HardSoftScore score;

    @PlanningEntityCollectionProperty
    public List<TestdataListFactorySortableEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListFactorySortableEntityProvidingEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    public void removeEntity(TestdataListFactorySortableEntityProvidingEntity entity) {
        this.entityList = entityList.stream()
                .filter(e -> e != entity)
                .toList();
    }
}
