package ai.timefold.solver.core.testdomain.valuerange.entityproviding.multientity;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

/**
 * Entity whose basic variable uses an entity value range.
 */
@PlanningEntity
public class TestdataMultiEntityEntityProvidingEntity extends TestdataObject {

    private List<TestdataValue> valueRange;

    private TestdataValue value;

    public TestdataMultiEntityEntityProvidingEntity() {
        // Required for cloning
    }

    public TestdataMultiEntityEntityProvidingEntity(String code, List<TestdataValue> valueRange, TestdataValue value) {
        super(code);
        this.valueRange = valueRange;
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    @ValueRangeProvider(id = "valueRange")
    public List<TestdataValue> getValueRange() {
        return valueRange;
    }

    public void setValueRange(List<TestdataValue> valueRange) {
        this.valueRange = valueRange;
    }
}
