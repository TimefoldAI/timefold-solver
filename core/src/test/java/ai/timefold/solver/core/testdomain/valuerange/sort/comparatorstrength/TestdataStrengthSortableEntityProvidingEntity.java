package ai.timefold.solver.core.testdomain.valuerange.sort.comparatorstrength;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.TestSortableComparator;
import ai.timefold.solver.core.testdomain.common.TestSortableObject;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity(difficultyComparatorClass = TestSortableComparator.class)
public class TestdataStrengthSortableEntityProvidingEntity extends TestdataObject implements TestSortableObject {

    @PlanningVariable(valueRangeProviderRefs = "valueRange", strengthComparatorClass = TestSortableComparator.class)
    private TestdataSortableValue value;
    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    private List<TestdataSortableValue> valueRange;

    private int difficulty;

    public TestdataStrengthSortableEntityProvidingEntity() {
    }

    public TestdataStrengthSortableEntityProvidingEntity(String code, int difficulty) {
        super(code);
        this.difficulty = difficulty;
    }

    public TestdataSortableValue getValue() {
        return value;
    }

    public void setValue(TestdataSortableValue value) {
        this.value = value;
    }

    public List<TestdataSortableValue> getValueRange() {
        return valueRange;
    }

    public void setValueRange(List<TestdataSortableValue> valueRange) {
        this.valueRange = valueRange;
    }

    @Override
    public int getComparatorValue() {
        return difficulty;
    }
}
