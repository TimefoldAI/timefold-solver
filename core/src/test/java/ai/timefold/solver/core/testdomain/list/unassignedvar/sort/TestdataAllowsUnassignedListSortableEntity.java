package ai.timefold.solver.core.testdomain.list.unassignedvar.sort;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.TestSortableObjectComparator;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity
public class TestdataAllowsUnassignedListSortableEntity extends TestdataObject {

    @PlanningListVariable(allowsUnassignedValues = true, valueRangeProviderRefs = "valueRange",
            comparatorClass = TestSortableObjectComparator.class)
    private List<TestdataSortableValue> valueList;

    public TestdataAllowsUnassignedListSortableEntity() {
    }

    public TestdataAllowsUnassignedListSortableEntity(String code) {
        super(code);
        this.valueList = new ArrayList<>();
    }

    public List<TestdataSortableValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataSortableValue> valueList) {
        this.valueList = valueList;
    }

}
