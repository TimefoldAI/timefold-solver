package ai.timefold.solver.benchmark.util;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

import java.util.List;

/**
 * Minimal stub – just enough for DefaultSolverFactory to build a descriptor.
 */
@PlanningSolution
public class TestSolution {

    @ValueRangeProvider(id = "valueRange")
    private List<TestValue> valueList;

    @PlanningEntityCollectionProperty
    private List<TestEntity> entityList;

    @PlanningScore
    private SimpleScore score;

    /* Getters/setters needed by reflection */
    public List<TestValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestValue> list) {
        this.valueList = list;
    }

    public List<TestEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestEntity> l) {
        this.entityList = l;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore s) {
        this.score = s;
    }
}

/**
 * Simple planning value type – no code necessary.
 */
record TestValue(int id) {
}

@PlanningEntity
class TestEntity {

    /* The single planning variable – can be null in tests. */
    @ai.timefold.solver.core.api.domain.variable.PlanningVariable(
            valueRangeProviderRefs = "valueRange")
    private TestValue value;

    public TestValue getValue() {
        return value;
    }

    public void setValue(TestValue val) {
        this.value = val;
    }
}

