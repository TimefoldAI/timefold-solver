package ai.timefold.solver.core.testdomain.valuerange.incomplete;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataIncompleteValueRangeSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataIncompleteValueRangeSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataIncompleteValueRangeSolution.class,
                TestdataIncompleteValueRangeEntity.class);
    }

    public static PlanningSolutionMetaModel<TestdataIncompleteValueRangeSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    public static TestdataIncompleteValueRangeSolution generateSolution() {
        return generateSolution(5, 7);
    }

    public static TestdataIncompleteValueRangeSolution generateSolution(int valueListSize, int entityListSize) {
        return generateSolution(valueListSize, entityListSize, true);
    }

    public static TestdataIncompleteValueRangeSolution generateUninitializedSolution(int valueListSize, int entityListSize) {
        return generateSolution(valueListSize, entityListSize, false);
    }

    private static TestdataIncompleteValueRangeSolution generateSolution(int valueListSize, int entityListSize,
            boolean initialized) {
        var solution = new TestdataIncompleteValueRangeSolution("Generated Solution 0");
        var valueList = new ArrayList<TestdataValue>(valueListSize);
        for (var i = 0; i < valueListSize; i++) {
            var value = new TestdataValue("Generated Value " + i);
            valueList.add(value);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataIncompleteValueRangeEntity>(entityListSize);
        for (var i = 0; i < entityListSize; i++) {
            var value = initialized ? valueList.get(i % valueListSize) : null;
            var entity = new TestdataIncompleteValueRangeEntity("Generated Entity " + i, value);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataValue> valueList;
    private List<TestdataValue> valueListNotInValueRange;
    private List<TestdataIncompleteValueRangeEntity> entityList;

    private SimpleScore score;

    public TestdataIncompleteValueRangeSolution() {
    }

    public TestdataIncompleteValueRangeSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueListNotInValueRange() {
        return valueListNotInValueRange == null ? new ArrayList<>() : valueListNotInValueRange;
    }

    public void setValueListNotInValueRange(List<TestdataValue> valueListNotInValueRange) {
        this.valueListNotInValueRange = valueListNotInValueRange;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataIncompleteValueRangeEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataIncompleteValueRangeEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}