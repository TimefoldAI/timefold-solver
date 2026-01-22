package ai.timefold.solver.core.testdomain.cascade.single;

import java.util.LinkedList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

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
