package ai.timefold.solver.core.testdomain.valuerange.hashcode;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.clone.lookup.TestdataObjectEquals;

@PlanningEntity
public class TestdataValueRangeHashCodeEntity extends TestdataObject {

    public static EntityDescriptor<TestdataValueRangeHashCodeSolution> buildEntityDescriptor() {
        return TestdataValueRangeHashCodeSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataValueRangeHashCodeEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataValueRangeHashCodeSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    @PlanningVariable
    private TestdataObjectEquals value;

    public TestdataValueRangeHashCodeEntity() {
    }

    public TestdataValueRangeHashCodeEntity(String code) {
        super(code);
    }

    public TestdataObjectEquals getValue() {
        return value;
    }

    public void setValue(TestdataObjectEquals value) {
        this.value = value;
    }
}
