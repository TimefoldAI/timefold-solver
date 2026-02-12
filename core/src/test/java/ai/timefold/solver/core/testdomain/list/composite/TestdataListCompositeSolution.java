package ai.timefold.solver.core.testdomain.list.composite;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

@PlanningSolution
public class TestdataListCompositeSolution {

    public static SolutionDescriptor<TestdataListCompositeSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataListCompositeSolution.class,
                TestdataListCompositeEntity.class);
    }

    public static TestdataListCompositeSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataListCompositeEntity> entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataListCompositeEntity("Generated Entity " + i))
                .toList();
        List<TestdataListValue> firstValueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataListValue("Generated Value " + i))
                .toList();
        List<TestdataListValue> secondValueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataListValue("Generated Value " + (valueCount + i)))
                .toList();
        TestdataListCompositeSolution solution = new TestdataListCompositeSolution();
        solution.setFirstValueList(firstValueList);
        solution.setSecondValueList(secondValueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataListValue> firstValueList;
    private List<TestdataListValue> secondValueList;
    private List<TestdataListCompositeEntity> entityList;
    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange1")
    @PlanningEntityCollectionProperty
    public List<TestdataListValue> getFirstValueList() {
        return firstValueList;
    }

    public void setFirstValueList(List<TestdataListValue> firstValueList) {
        this.firstValueList = firstValueList;
    }

    @ValueRangeProvider(id = "valueRange2")
    @PlanningEntityCollectionProperty
    public List<TestdataListValue> getSecondValueList() {
        return secondValueList;
    }

    public void setSecondValueList(List<TestdataListValue> secondValueList) {
        this.secondValueList = secondValueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataListCompositeEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListCompositeEntity> entityList) {
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
