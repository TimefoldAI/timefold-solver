package ai.timefold.solver.core.impl.testdata.domain.cascade.single_var;

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
public class TestdataSingleCascadingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataSingleCascadingSolution> buildEntityDescriptor() {
        return TestdataSingleCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataSingleCascadingEntity.class);
    }

    public static ListVariableDescriptor<TestdataSingleCascadingSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataSingleCascadingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataSingleCascadingEntity createWithValues(String code, TestdataSingleCascadingValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataSingleCascadingEntity(code, new ArrayList<>(Arrays.asList(values))).setUpShadowVariables();
    }

    TestdataSingleCascadingEntity setUpShadowVariables() {
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
            }
        }
        return this;
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataSingleCascadingValue> valueList;

    public TestdataSingleCascadingEntity(String code) {
        super(code);
        this.valueList = new LinkedList<>();
    }

    public TestdataSingleCascadingEntity(String code, List<TestdataSingleCascadingValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public void setValueList(List<TestdataSingleCascadingValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataSingleCascadingValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataSingleCascadingEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
