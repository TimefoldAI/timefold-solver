package ai.timefold.solver.core.impl.testdata.domain.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataListEntity extends TestdataObject {

    public static EntityDescriptor<TestdataListSolution> buildEntityDescriptor() {
        return TestdataListSolution.buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataListEntity.class);
    }

    public static ListVariableDescriptor<TestdataListSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataListSolution>) buildEntityDescriptor().getGenuineVariableDescriptor("valueList");
    }

    public static TestdataListEntity createWithValues(String code, TestdataListValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataListEntity(code, values).setUpShadowVariables();
    }

    TestdataListEntity setUpShadowVariables() {
        valueList.forEach(testdataListValue -> {
            testdataListValue.setEntity(this);
            testdataListValue.setIndex(valueList.indexOf(testdataListValue));
        });
        return this;
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataListValue> valueList;

    public TestdataListEntity() {
    }

    public TestdataListEntity(String code, List<TestdataListValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataListEntity(String code, TestdataListValue... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    public List<TestdataListValue> getValueList() {
        return valueList;
    }

    public void addValue(TestdataListValue value) {
        addValueAt(valueList.size(), value);
    }

    public void addValueAt(int pos, TestdataListValue value) {
        List<TestdataListValue> newValueList = new ArrayList<>(valueList);
        newValueList.add(pos, value);
        this.valueList = newValueList;
    }

    public void removeValue(TestdataListValue value) {
        this.valueList = valueList.stream()
                .filter(v -> !Objects.equals(v, value))
                .toList();
    }
}
