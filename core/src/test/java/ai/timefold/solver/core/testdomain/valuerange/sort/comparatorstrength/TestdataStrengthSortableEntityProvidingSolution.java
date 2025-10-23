package ai.timefold.solver.core.testdomain.valuerange.sort.comparatorstrength;

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
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningSolution
public class TestdataStrengthSortableEntityProvidingSolution {

    public static SolutionDescriptor<TestdataStrengthSortableEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataStrengthSortableEntityProvidingSolution.class,
                TestdataStrengthSortableEntityProvidingEntity.class);
    }

    public static TestdataStrengthSortableEntityProvidingSolution generateSolution(int valueCount, int entityCount,
            boolean shuffle) {
        var entityList = new ArrayList<>(IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataStrengthSortableEntityProvidingEntity("Generated Entity " + i, i))
                .toList());
        var valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataSortableValue("Generated Value " + i, i))
                .toList();
        var random = new Random(0);
        var solution = new TestdataStrengthSortableEntityProvidingSolution();
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

    private List<TestdataStrengthSortableEntityProvidingEntity> entityList;
    private HardSoftScore score;

    @PlanningEntityCollectionProperty
    public List<TestdataStrengthSortableEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataStrengthSortableEntityProvidingEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    public void removeEntity(TestdataStrengthSortableEntityProvidingEntity entity) {
        this.entityList = entityList.stream()
                .filter(e -> e != entity)
                .toList();
    }
}
