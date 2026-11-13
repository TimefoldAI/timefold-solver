package ai.timefold.solver.core.testdomain.valuerange.entityproviding.pinned;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataPinnedEntityProvidingEntity extends TestdataObject {

    private List<TestdataValue> valueRange;
    private TestdataValue value;
    private boolean pinned;

    public TestdataPinnedEntityProvidingEntity() {
        // Required for cloning
    }

    public TestdataPinnedEntityProvidingEntity(String code, List<TestdataValue> valueRange, TestdataValue value,
            boolean pinned) {
        super(code);
        this.valueRange = valueRange;
        this.value = value;
        this.pinned = pinned;
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

    @PlanningPin
    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

}
