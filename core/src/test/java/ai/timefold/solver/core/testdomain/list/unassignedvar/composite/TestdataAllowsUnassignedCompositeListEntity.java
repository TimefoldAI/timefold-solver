package ai.timefold.solver.core.testdomain.list.unassignedvar.composite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

@PlanningEntity
public class TestdataAllowsUnassignedCompositeListEntity extends TestdataObject {

    public static EntityDescriptor<TestdataAllowsUnassignedCompositeListSolution> buildEntityDescriptor() {
        return TestdataAllowsUnassignedCompositeListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataAllowsUnassignedCompositeListEntity.class);
    }

    public static ListVariableDescriptor<TestdataAllowsUnassignedCompositeListSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataAllowsUnassignedCompositeListSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    @PlanningListVariable(valueRangeProviderRefs = { "valueRange1", "valueRange2" }, allowsUnassignedValues = true)
    private List<TestdataListValue> valueList;

    public TestdataAllowsUnassignedCompositeListEntity() {
        // Required for cloning
    }

    public TestdataAllowsUnassignedCompositeListEntity(String code, List<TestdataListValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataAllowsUnassignedCompositeListEntity(String code, TestdataListValue... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    public List<TestdataListValue> getValueList() {
        return valueList;
    }
}
