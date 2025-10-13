package ai.timefold.solver.core.testdomain.valuerange.sort.factory;

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
public class TestdataFactorySortableEntityProvidingSolution {

    public static SolutionDescriptor<TestdataFactorySortableEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataFactorySortableEntityProvidingSolution.class,
                TestdataFactorySortableEntityProvidingEntity.class,
                TestdataFactorySortableEntityProvidingValue.class);
    }

    public static TestdataFactorySortableEntityProvidingSolution generateSolution(int valueCount, int entityCount,
                                                                                  boolean shuffle) {
        var entityList = new ArrayList<>(IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataFactorySortableEntityProvidingEntity("Generated Entity " + i, i))
                .toList());
        var valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataFactorySortableEntityProvidingValue("Generated Value " + i, i))
                .toList();
        var solution = new TestdataFactorySortableEntityProvidingSolution();
        var random = new Random(0);
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

    private List<TestdataFactorySortableEntityProvidingEntity> entityList;
    private HardSoftScore score;

    @PlanningEntityCollectionProperty
    public List<TestdataFactorySortableEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataFactorySortableEntityProvidingEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    public void removeEntity(TestdataFactorySortableEntityProvidingEntity entity) {
        this.entityList = entityList.stream()
                .filter(e -> e != entity)
                .toList();
    }
}
