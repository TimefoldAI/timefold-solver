package ai.timefold.solver.core.impl.testdata.domain.shadow.multiplelistener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

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

    public static TestdataListMultipleShadowVariableEntity createWithValues(String code,
                                                                            TestdataListMultipleShadowVariableValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataListMultipleShadowVariableEntity(code, values).setUpShadowVariables();
    }

    TestdataListMultipleShadowVariableEntity setUpShadowVariables() {
        for (int i = 0; i < valueList.size(); i++) {
            TestdataListMultipleShadowVariableValue value = valueList.get(i);
            value.setEntity(this);
            value.setIndex(i);
            if (i > 0) {
                value.setPrevious(valueList.get(i - 1));
            }
            if (i < valueList.size() - 1) {
                value.setNext(valueList.get(i + 1));
            }
        }
        return this;
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
