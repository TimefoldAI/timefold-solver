package ai.timefold.solver.core.testdomain.equals.list;

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
public class TestdataEqualsByCodeListSolution extends TestdataEqualsByCodeListObject {

    public static SolutionDescriptor<TestdataEqualsByCodeListSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataEqualsByCodeListSolution.class,
                TestdataEqualsByCodeListEntity.class,
                TestdataEqualsByCodeListValue.class);
    }

    public static TestdataEqualsByCodeListSolution generateSolution(int valueCount, int entityCount) {
        return generateSolution("Generated Solution 0", valueCount, entityCount);
    }

    public static TestdataEqualsByCodeListSolution generateSolution(String solutionName, int valueCount, int entityCount) {
        List<TestdataEqualsByCodeListEntity> entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataEqualsByCodeListEntity("Generated Entity " + i))
                .collect(Collectors.toList());
        List<TestdataEqualsByCodeListValue> valueList = IntStream.range(0, valueCount)
                .mapToObj(i -> new TestdataEqualsByCodeListValue("Generated Value " + i))
                .collect(Collectors.toList());
        TestdataEqualsByCodeListSolution solution = new TestdataEqualsByCodeListSolution(solutionName);
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataEqualsByCodeListValue> valueList;
    private List<TestdataEqualsByCodeListEntity> entityList;
    private SimpleScore score;

    public TestdataEqualsByCodeListSolution(String code) {
        super(code);
    }

    public TestdataEqualsByCodeListSolution initialize() {
        for (int i = 0; i < valueList.size(); i++) {
            entityList.get(i % entityList.size()).getValueList().add(valueList.get(i));
        }
        entityList.forEach(TestdataEqualsByCodeListEntity::setUpShadowVariables);
        return this;
    }

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataEqualsByCodeListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataEqualsByCodeListValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataEqualsByCodeListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataEqualsByCodeListEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    public void removeEntity(TestdataEqualsByCodeListEntity entity) {
        this.entityList = entityList.stream()
                .filter(e -> e != entity)
                .toList();
    }
}
