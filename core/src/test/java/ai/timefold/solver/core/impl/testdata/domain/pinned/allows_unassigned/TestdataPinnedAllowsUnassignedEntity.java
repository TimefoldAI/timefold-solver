package ai.timefold.solver.core.impl.testdata.domain.pinned.allows_unassigned;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity(pinningFilter = TestdataAllowsUnassignedPinningFilter.class)
public class TestdataPinnedAllowsUnassignedEntity extends TestdataObject {

    public static EntityDescriptor<TestdataPinnedAllowsUnassignedSolution> buildEntityDescriptor() {
        return TestdataPinnedAllowsUnassignedSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataPinnedAllowsUnassignedEntity.class);
    }

    private TestdataValue value;
    private boolean locked;
    private boolean pinned;

    public TestdataPinnedAllowsUnassignedEntity() {
    }

    public TestdataPinnedAllowsUnassignedEntity(String code) {
        super(code);
    }

    public TestdataPinnedAllowsUnassignedEntity(String code, boolean locked, boolean pinned) {
        this(code);
        this.locked = locked;
        this.pinned = pinned;
    }

    public TestdataPinnedAllowsUnassignedEntity(String code, TestdataValue value) {
        this(code);
        this.value = value;
    }

    public TestdataPinnedAllowsUnassignedEntity(String code, TestdataValue value, boolean locked, boolean pinned) {
        this(code, value);
        this.locked = locked;
        this.pinned = pinned;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange", allowsUnassigned = true)
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @PlanningPin
    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
