package ai.timefold.solver.core.testdomain.list.valuerange.sort.comparator;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.TestSortableComparator;
import ai.timefold.solver.core.testdomain.common.TestSortableObject;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity(comparatorClass = TestSortableComparator.class)
public class TestdataListSortableEntityProvidingEntity extends TestdataObject implements TestSortableObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange", comparatorClass = TestSortableComparator.class)
    private List<TestdataSortableValue> valueList;
    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    private List<TestdataSortableValue> valueRange;

    private int difficulty;

    public TestdataListSortableEntityProvidingEntity() {
    }

    public TestdataListSortableEntityProvidingEntity(String code, int difficulty) {
        super(code);
        this.difficulty = difficulty;
        this.valueList = new ArrayList<>();
    }

    public List<TestdataSortableValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataSortableValue> valueList) {
        this.valueList = valueList;
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
