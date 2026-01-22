package ai.timefold.solver.core.testdomain.list.unassignedvar.pinned;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataPinnedUnassignedValuesListEntity extends TestdataObject {

    public static EntityDescriptor<TestdataPinnedUnassignedValuesListSolution> buildEntityDescriptor() {
        return TestdataPinnedUnassignedValuesListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataPinnedUnassignedValuesListEntity.class);
    }

    public static ListVariableDescriptor<TestdataPinnedUnassignedValuesListSolution>
            buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataPinnedUnassignedValuesListSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    private List<TestdataPinnedUnassignedValuesListValue> valueList;
    @PlanningPinToIndex
    private int planningPinToIndex = 0;

    public TestdataPinnedUnassignedValuesListEntity() {
    }

    public TestdataPinnedUnassignedValuesListEntity(String code,
            List<TestdataPinnedUnassignedValuesListValue> valueList) {
        super(code);
        this.valueList = valueList;
    }

    public TestdataPinnedUnassignedValuesListEntity(String code,
            TestdataPinnedUnassignedValuesListValue... values) {
        this(code, new ArrayList<>(Arrays.asList(values)));
    }

    @PlanningListVariable(allowsUnassignedValues = true, valueRangeProviderRefs = "valueRange")
    public List<TestdataPinnedUnassignedValuesListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataPinnedUnassignedValuesListValue> valueList) {
        this.valueList = valueList;
    }

    public int getPlanningPinToIndex() {
        return planningPinToIndex;
    }

    public void setPlanningPinToIndex(int planningPinToIndex) {
        this.planningPinToIndex = planningPinToIndex;
    }

}
