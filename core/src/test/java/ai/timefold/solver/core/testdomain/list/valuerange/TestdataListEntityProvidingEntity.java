package ai.timefold.solver.core.testdomain.list.valuerange;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

import java.util.ArrayList;
import java.util.List;

@PlanningEntity
public class TestdataListEntityProvidingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataListEntityProvidingSolution> buildEntityDescriptor() {
        return TestdataListEntityProvidingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListEntityProvidingEntity.class);
    }

    public static ListVariableDescriptor<TestdataListEntityProvidingSolution> buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataListEntityProvidingSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    @ValueRangeProvider(id = "valueRange")
    private final List<TestdataListEntityProvidingValue> valueRange;
    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataListEntityProvidingValue> valueList;

    public TestdataListEntityProvidingEntity() {
        // Required for cloning
        valueRange = new ArrayList<>();
        valueList = new ArrayList<>();
    }

    public TestdataListEntityProvidingEntity(String code, List<TestdataListEntityProvidingValue> valueRange) {
        super(code);
        this.valueRange = valueRange;
        valueList = new ArrayList<>();
    }

    public TestdataListEntityProvidingEntity(String code, List<TestdataListEntityProvidingValue> valueRange,
            List<TestdataListEntityProvidingValue> valueList) {
        super(code);
        this.valueRange = valueRange;
        this.valueList = valueList;
        for (var i = 0; i < valueList.size(); i++) {
            var value = valueList.get(i);
            value.setEntity(this);
            value.setIndex(i);
        }
    }

    public TestdataListEntityProvidingEntity setUpShadowVariables() {
        valueList.forEach(testdataListValue -> {
            testdataListValue.setEntity(this);
            testdataListValue.setIndex(valueList.indexOf(testdataListValue));
        });
        return this;
    }

    public List<TestdataListEntityProvidingValue> getValueRange() {
        return valueRange;
    }

    public List<TestdataListEntityProvidingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListEntityProvidingValue> valueList) {
        this.valueList = valueList;
    }
}
