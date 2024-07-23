package ai.timefold.solver.core.impl.testdata.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

@PlanningEntity
public class TestdataEntity extends TestdataObject {

    public static final String VALUE_FIELD = "value";

    public static EntityDescriptor<TestdataSolution> buildEntityDescriptor() {
        return TestdataSolution.buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataValue value;

    public TestdataEntity() {
    }

    public TestdataEntity(String code) {
        super(code);
    }

    public TestdataEntity(String code, TestdataValue value) {
        this(code);
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************
    public void updateValue() {
        this.value = new TestdataValue(value.code + "/" + value.code);
    }
}
