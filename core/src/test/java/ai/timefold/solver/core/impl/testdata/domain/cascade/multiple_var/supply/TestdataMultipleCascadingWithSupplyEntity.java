package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.supply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.TestdataCascadingBaseEntity;

@PlanningEntity
public class TestdataMultipleCascadingWithSupplyEntity extends TestdataObject
        implements TestdataCascadingBaseEntity<TestdataMultipleCascadingWithSupplyValue> {

    public static EntityDescriptor<TestdataMultipleCascadingWithSupplySolution> buildEntityDescriptor() {
        return TestdataMultipleCascadingWithSupplySolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataMultipleCascadingWithSupplyEntity.class);
    }

    public static ListVariableDescriptor<TestdataMultipleCascadingWithSupplySolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataMultipleCascadingWithSupplySolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataMultipleCascadingWithSupplyEntity createWithValues(String code,
            TestdataMultipleCascadingWithSupplyValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataMultipleCascadingWithSupplyEntity(code, new ArrayList<>(Arrays.asList(values)))
                .setUpShadowVariables();
    }

    TestdataMultipleCascadingWithSupplyEntity setUpShadowVariables() {
        if (valueList != null && !valueList.isEmpty()) {
            int i = 0;
            var previous = valueList.get(i);
            var current = valueList.get(i);
            while (current != null) {
                current.setEntity(this);
                current.setPrevious(previous);
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
    private List<TestdataMultipleCascadingWithSupplyValue> valueList;

    public TestdataMultipleCascadingWithSupplyEntity() {
    }

    public TestdataMultipleCascadingWithSupplyEntity(String code) {
        super(code);
        this.valueList = new LinkedList<>();
    }

    public TestdataMultipleCascadingWithSupplyEntity(String code, List<TestdataMultipleCascadingWithSupplyValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setValueList(List valueList) {
        this.valueList = valueList;
    }

    @Override
    public List<TestdataMultipleCascadingWithSupplyValue> getValueList() {
        return valueList;
    }

    @Override
    public String toString() {
        return "TestdataMultipleCascadeEntity{" +
                "code='" + code + '\'' +
                '}';
    }
}
