package ai.timefold.solver.core.testdomain.equals.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;

@PlanningEntity
public class TestdataEqualsByCodeListEntity extends TestdataEqualsByCodeListObject {

    public static EntityDescriptor<TestdataEqualsByCodeListSolution> buildEntityDescriptor() {
        return TestdataEqualsByCodeListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataEqualsByCodeListEntity.class);
    }

    public static ListVariableDescriptor<TestdataEqualsByCodeListSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataEqualsByCodeListSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    public static TestdataEqualsByCodeListEntity createWithValues(String code, TestdataEqualsByCodeListValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataEqualsByCodeListEntity(code, values).setUpShadowVariables();
    }

    TestdataEqualsByCodeListEntity setUpShadowVariables() {
        valueList.forEach(testdataEqualsByCodeListValue -> {
            testdataEqualsByCodeListValue.setEntity(this);
            testdataEqualsByCodeListValue.setIndex(valueList.indexOf(testdataEqualsByCodeListValue));
        });
        return this;
    }

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataEqualsByCodeListValue> valueList;

    public TestdataEqualsByCodeListEntity(String code, List<TestdataEqualsByCodeListValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataEqualsByCodeListEntity(String code, TestdataEqualsByCodeListValue... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    public List<TestdataEqualsByCodeListValue> getValueList() {
        return valueList;
    }

    public void addValue(TestdataEqualsByCodeListValue value) {
        addValueAt(valueList.size(), value);
    }

    public void addValueAt(int pos, TestdataEqualsByCodeListValue value) {
        List<TestdataEqualsByCodeListValue> newValueList = new ArrayList<>(valueList);
        newValueList.add(pos, value);
        this.valueList = newValueList;
    }

    public void removeValue(TestdataEqualsByCodeListValue value) {
        this.valueList = valueList.stream()
                .filter(v -> !Objects.equals(v, value))
                .toList();
    }

}
