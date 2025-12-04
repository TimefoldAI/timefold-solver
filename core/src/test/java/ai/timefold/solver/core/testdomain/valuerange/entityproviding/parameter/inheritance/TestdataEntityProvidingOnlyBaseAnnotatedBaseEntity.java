package ai.timefold.solver.core.testdomain.valuerange.entityproviding.parameter.inheritance;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import java.util.List;

@PlanningEntity
public class TestdataEntityProvidingOnlyBaseAnnotatedBaseEntity extends TestdataObject {

    public static final String VALUE_FIELD = "value";

    public static EntityDescriptor<TestdataSolution> buildEntityDescriptor() {
        return TestdataSolution.buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataEntityProvidingOnlyBaseAnnotatedBaseEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataValue value;

    public TestdataEntityProvidingOnlyBaseAnnotatedBaseEntity() {
    }

    public TestdataEntityProvidingOnlyBaseAnnotatedBaseEntity(String code) {
        super(code);
    }

    public TestdataEntityProvidingOnlyBaseAnnotatedBaseEntity(String code, TestdataValue value) {
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

    @ValueRangeProvider(id = "valueRange")
    public List<TestdataValue> getValueList(TestdataEntityProvidingOnlyBaseAnnotatedSolution solution) {
        return solution.getValueList();
    }
}
