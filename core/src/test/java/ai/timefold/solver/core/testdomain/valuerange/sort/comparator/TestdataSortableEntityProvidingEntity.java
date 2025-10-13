package ai.timefold.solver.core.testdomain.valuerange.sort.comparator;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity(difficultyComparatorClass = SortableEntityComparator.class)
public class TestdataSortableEntityProvidingEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "valueRange", strengthComparatorClass = SortableValueComparator.class)
    private TestdataSortableEntityProvidingValue value;
    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    private List<TestdataSortableEntityProvidingValue> valueRange;

    private int difficulty;

    public TestdataSortableEntityProvidingEntity() {
    }

    public TestdataSortableEntityProvidingEntity(String code, int difficulty) {
        super(code);
        this.difficulty = difficulty;
    }

    public TestdataSortableEntityProvidingValue getValue() {
        return value;
    }

    public void setValue(TestdataSortableEntityProvidingValue value) {
        this.value = value;
    }

    public List<TestdataSortableEntityProvidingValue> getValueRange() {
        return valueRange;
    }

    public void setValueRange(List<TestdataSortableEntityProvidingValue> valueRange) {
        this.valueRange = valueRange;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
}
