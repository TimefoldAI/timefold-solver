package ai.timefold.solver.core.impl.testdata.domain.reflect.generic;

import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningSolution
public class TestdataGenericSolution<T> extends TestdataObject {

    public static SolutionDescriptor<TestdataGenericSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataGenericSolution.class, TestdataGenericEntity.class);
    }

    private List<TestdataGenericValue<T>> valueList;
    private List<? extends TestdataGenericValue<T>> subTypeValueList;
    private List<TestdataGenericValue<Map<T, TestdataGenericValue<T>>>> complexGenericValueList;
    private List<TestdataGenericEntity<T>> entityList;

    private SimpleScore score;

    public TestdataGenericSolution() {
    }

    public TestdataGenericSolution(String code) {
        super(code);
    }

    public TestdataGenericSolution(String code, List<TestdataGenericValue<T>> valueList,
            List<TestdataGenericValue<Map<T, TestdataGenericValue<T>>>> complexGenericValueList,
            List<TestdataGenericEntity<T>> entityList) {
        super(code);
        this.valueList = valueList;
        this.complexGenericValueList = complexGenericValueList;
        this.entityList = entityList;
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataGenericValue<T>> getValueList() {
        return valueList;
    }

    @ValueRangeProvider(id = "complexGenericValueRange")
    @ProblemFactCollectionProperty
    public List<TestdataGenericValue<Map<T, TestdataGenericValue<T>>>> getComplexGenericValueList() {
        return complexGenericValueList;
    }

    @ValueRangeProvider(id = "subTypeValueRange")
    @ProblemFactCollectionProperty
    public List<? extends TestdataGenericValue<T>> getSubTypeValueList() {
        return subTypeValueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataGenericEntity<T>> getEntityList() {
        return entityList;
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
