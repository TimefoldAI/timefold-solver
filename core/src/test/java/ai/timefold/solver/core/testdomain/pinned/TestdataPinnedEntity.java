package ai.timefold.solver.core.testdomain.pinned;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataPinnedEntity extends TestdataObject {

    public static EntityDescriptor<TestdataPinnedSolution> buildEntityDescriptor() {
        return TestdataPinnedSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataPinnedEntity.class);
    }

    private TestdataValue value;
    private boolean pinned;

    public TestdataPinnedEntity() {
    }

    public TestdataPinnedEntity(String code) {
        super(code);
    }

    public TestdataPinnedEntity(String code, TestdataValue value) {
        this(code);
        this.value = value;
    }

    public TestdataPinnedEntity(String code, TestdataValue value, boolean pinned) {
        this(code, value);
        this.pinned = pinned;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
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
