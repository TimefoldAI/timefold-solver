package ai.timefold.solver.core.testdomain.list.unassignedvar.composite;

import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

@PlanningSolution
public class TestdataAllowsUnassignedCompositeListSolution {

    public static SolutionDescriptor<TestdataAllowsUnassignedCompositeListSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataAllowsUnassignedCompositeListSolution.class,
                TestdataAllowsUnassignedCompositeListEntity.class);
    }

    public static TestdataAllowsUnassignedCompositeListSolution generateSolution(int valueCount, int entityCount) {
        List<TestdataAllowsUnassignedCompositeListEntity> entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataAllowsUnassignedCompositeListEntity("Generated Entity " + i))
                .toList();
        List<TestdataListValue> firstValueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataListValue("Generated Value " + i))
                .toList();
        List<TestdataListValue> secondValueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataListValue("Generated Value " + (valueCount + i)))
                .toList();
        TestdataAllowsUnassignedCompositeListSolution solution = new TestdataAllowsUnassignedCompositeListSolution();
        solution.setFirstValueList(firstValueList);
        solution.setSecondValueList(secondValueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataListValue> firstValueList;
    private List<TestdataListValue> secondValueList;
    private List<TestdataAllowsUnassignedCompositeListEntity> entityList;
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
    public List<TestdataAllowsUnassignedCompositeListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataAllowsUnassignedCompositeListEntity> entityList) {
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
