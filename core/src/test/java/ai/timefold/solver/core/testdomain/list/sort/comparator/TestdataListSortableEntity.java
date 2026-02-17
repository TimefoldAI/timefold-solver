package ai.timefold.solver.core.testdomain.list.sort.comparator;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.TestSortableObject;
import ai.timefold.solver.core.testdomain.common.TestSortableObjectComparator;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity(comparatorClass = TestSortableObjectComparator.class)
public class TestdataListSortableEntity extends TestdataObject implements TestSortableObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange", comparatorClass = TestSortableObjectComparator.class)
    private List<TestdataSortableValue> valueList;
    private int difficulty;

    public TestdataListSortableEntity() {
    }

    public TestdataListSortableEntity(String code, int difficulty) {
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

    @Override
    public int getComparatorValue() {
        return difficulty;
    }
}
