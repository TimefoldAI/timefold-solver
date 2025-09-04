package ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataAllowsUnassignedMultiVarEntityProvidingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataAllowsUnassignedMultiVarEntityProvidingSolution> buildEntityDescriptor() {
        return TestdataAllowsUnassignedMultiVarEntityProvidingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataAllowsUnassignedMultiVarEntityProvidingEntity.class);
    }

    @ValueRangeProvider(id = "valueRange")
    private List<TestdataValue> valueRange;
    @PlanningVariable(valueRangeProviderRefs = "valueRange", allowsUnassigned = true)
    private TestdataValue value;
    @ValueRangeProvider(id = "secondValueRange")
    private List<TestdataValue> secondValueRange;
    @PlanningVariable(valueRangeProviderRefs = "secondValueRange", allowsUnassigned = true)
    private TestdataValue secondValue;
    @PlanningVariable(valueRangeProviderRefs = "solutionValueRange", allowsUnassigned = true)
    private TestdataValue solutionValue;

    public TestdataAllowsUnassignedMultiVarEntityProvidingEntity() {
        // Required for cloning
    }

    public TestdataAllowsUnassignedMultiVarEntityProvidingEntity(String code, List<TestdataValue> valueRange,
            List<TestdataValue> secondValueRange) {
        this(code, valueRange, null, secondValueRange, null, null);
    }

    public TestdataAllowsUnassignedMultiVarEntityProvidingEntity(String code, List<TestdataValue> valueRange,
            TestdataValue value,
            List<TestdataValue> secondValueRange, TestdataValue secondValue, TestdataValue solutionValue) {
        super(code);
        this.valueRange = valueRange;
        this.value = value;
        this.secondValueRange = secondValueRange;
        this.secondValue = secondValue;
        this.solutionValue = solutionValue;
    }

    public List<TestdataValue> getValueRange() {
        return valueRange;
    }

    public void setValueRange(List<TestdataValue> valueRange) {
        this.valueRange = valueRange;
    }

    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    public List<TestdataValue> getSecondValueRange() {
        return secondValueRange;
    }

    public void setSecondValueRange(List<TestdataValue> secondValueRange) {
        this.secondValueRange = secondValueRange;
    }

    public TestdataValue getSecondValue() {
        return secondValue;
    }

    public void setSecondValue(TestdataValue secondValue) {
        this.secondValue = secondValue;
    }

    public TestdataValue getSolutionValue() {
        return solutionValue;
    }

    public void setSolutionValue(TestdataValue solutionValue) {
        this.solutionValue = solutionValue;
    }
}
