package ai.timefold.solver.core.testdomain.list.sort.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningSolution
public class TestdataListFactorySortableSolution {

    public static SolutionDescriptor<TestdataListFactorySortableSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataListFactorySortableSolution.class,
                TestdataListFactorySortableEntity.class,
                TestdataSortableValue.class);
    }

    public static TestdataListFactorySortableSolution generateSolution(int valueCount, int entityCount, boolean shuffle) {
        var entityList = new ArrayList<>(IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataListFactorySortableEntity("Generated Entity " + i, i))
                .toList());
        var valueList = new ArrayList<>(IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataSortableValue("Generated Value " + i, i))
                .toList());
        if (shuffle) {
            var random = new Random(0);
            Collections.shuffle(entityList, random);
            Collections.shuffle(valueList, random);
        }
        TestdataListFactorySortableSolution solution = new TestdataListFactorySortableSolution();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataSortableValue> valueList;
    private List<TestdataListFactorySortableEntity> entityList;
    private HardSoftScore score;

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataSortableValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataSortableValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataListFactorySortableEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListFactorySortableEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    public void removeEntity(TestdataListFactorySortableEntity entity) {
        this.entityList = entityList.stream()
                .filter(e -> e != entity)
                .toList();
    }
}
