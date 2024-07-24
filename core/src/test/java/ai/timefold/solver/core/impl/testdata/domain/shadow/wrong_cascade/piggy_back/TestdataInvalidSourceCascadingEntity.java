package ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.piggy_back;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataInvalidSourceCascadingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataInvalidSourceCascadingSolution> buildEntityDescriptor() {
        return TestdataInvalidSourceCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataInvalidSourceCascadingEntity.class);
    }

    public static ListVariableDescriptor<TestdataInvalidSourceCascadingSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataInvalidSourceCascadingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataInvalidSourceCascadingEntity createWithValues(String code, TestdataInvalidSourceCascadingValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataInvalidSourceCascadingEntity(code, new ArrayList<>(Arrays.asList(values))).setUpShadowVariables();
    }

    TestdataInvalidSourceCascadingEntity setUpShadowVariables() {
        if (valueList != null && !valueList.isEmpty()) {
            int i = 0;
            var previous = valueList.get(i);
            var current = valueList.get(i);
            while (current != null) {
                current.setEntity(this);
                current.setPrevious(previous);
                if (previous != null) {
                    previous.setNext(current);
                }
                previous = current;
                current = ++i < valueList.size() ? valueList.get(i) : null;
            }
            for (var v : valueList) {
                v.updateCascadeValue();
                v.updateCascadeValueWithReturnType();
            }
        }
        return this;
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataInvalidSourceCascadingValue> valueList;

    public TestdataInvalidSourceCascadingEntity(String code) {
        super(code);
        this.valueList = new LinkedList<>();
    }

    public TestdataInvalidSourceCascadingEntity(String code, List<TestdataInvalidSourceCascadingValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public void setValueList(List<TestdataInvalidSourceCascadingValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataInvalidSourceCascadingValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataCascadeEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
