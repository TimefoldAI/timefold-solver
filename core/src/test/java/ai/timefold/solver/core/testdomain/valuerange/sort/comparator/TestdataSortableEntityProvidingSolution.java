package ai.timefold.solver.core.testdomain.valuerange.sort.comparator;

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
public class TestdataSortableEntityProvidingSolution {

    public static SolutionDescriptor<TestdataSortableEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataSortableEntityProvidingSolution.class,
                TestdataSortableEntityProvidingEntity.class,
                TestdataSortableEntityProvidingValue.class);
    }

    public static TestdataSortableEntityProvidingSolution generateSolution(int valueCount, int entityCount,
                                                                           boolean shuffle) {
        var entityList = new ArrayList<>(IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataSortableEntityProvidingEntity("Generated Entity " + i, i))
                .toList());
        var valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataSortableEntityProvidingValue("Generated Value " + i, i))
                .toList();
        var random = new Random(0);
        var solution = new TestdataSortableEntityProvidingSolution();
        for (var entity : entityList) {
            var valueRange = new ArrayList<>(valueList);
            if (shuffle) {
                Collections.shuffle(valueRange, random);
            }
            entity.setValueRange(valueRange);
        }
        if (shuffle) {
            Collections.shuffle(entityList, random);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataSortableEntityProvidingEntity> entityList;
    private HardSoftScore score;

    @PlanningEntityCollectionProperty
    public List<TestdataSortableEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataSortableEntityProvidingEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    public void removeEntity(TestdataSortableEntityProvidingEntity entity) {
        this.entityList = entityList.stream()
                .filter(e -> e != entity)
                .toList();
    }
}
