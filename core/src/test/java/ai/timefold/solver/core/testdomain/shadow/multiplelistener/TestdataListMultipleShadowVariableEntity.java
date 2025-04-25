package ai.timefold.solver.core.testdomain.shadow.multiplelistener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListMultipleShadowVariableEntity extends TestdataObject {

    public static EntityDescriptor<TestdataListMultipleShadowVariableSolution> buildEntityDescriptor() {
        return TestdataListMultipleShadowVariableSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListMultipleShadowVariableEntity.class);
    }

    public static ListVariableDescriptor<TestdataListMultipleShadowVariableSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataListMultipleShadowVariableSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataListMultipleShadowVariableValue> valueList;

    public TestdataListMultipleShadowVariableEntity() {
    }

    public TestdataListMultipleShadowVariableEntity(String code, List<TestdataListMultipleShadowVariableValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataListMultipleShadowVariableEntity(String code, TestdataListMultipleShadowVariableValue... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    public void setValueList(List<TestdataListMultipleShadowVariableValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataListMultipleShadowVariableValue> getValueList() {
        return valueList;
    }
}
