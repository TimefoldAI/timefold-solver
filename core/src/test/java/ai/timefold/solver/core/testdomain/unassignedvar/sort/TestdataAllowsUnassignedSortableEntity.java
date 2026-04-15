package ai.timefold.solver.core.testdomain.unassignedvar.sort;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.TestSortableComparator;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity
public class TestdataAllowsUnassignedSortableEntity extends TestdataObject {

    @PlanningVariable(allowsUnassigned = true, valueRangeProviderRefs = "valueRange",
            comparatorClass = TestSortableComparator.class)
    private TestdataSortableValue value;

    public TestdataAllowsUnassignedSortableEntity() {
    }

    public TestdataAllowsUnassignedSortableEntity(String code) {
        super(code);
    }

    public TestdataSortableValue getValue() {
        return value;
    }

    public void setValue(TestdataSortableValue value) {
        this.value = value;
    }
}
