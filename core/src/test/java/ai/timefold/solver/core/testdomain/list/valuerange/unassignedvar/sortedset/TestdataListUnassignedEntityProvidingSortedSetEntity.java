package ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.sortedset;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataListUnassignedEntityProvidingSortedSetEntity extends TestdataObject {

    public static EntityDescriptor<TestdataListUnassignedEntityProvidingSortedSetSolution> buildEntityDescriptor() {
        return TestdataListUnassignedEntityProvidingSortedSetSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListUnassignedEntityProvidingSortedSetEntity.class);
    }

    public static ListVariableDescriptor<TestdataListUnassignedEntityProvidingSortedSetSolution>
            buildVariableDescriptorForValueList() {
        return (ListVariableDescriptor<TestdataListUnassignedEntityProvidingSortedSetSolution>) buildEntityDescriptor()
                .getGenuineVariableDescriptor("valueList");
    }

    @ValueRangeProvider(id = "valueRange")
    private final SortedSet<TestdataValue> valueRange;
    @PlanningListVariable(valueRangeProviderRefs = "valueRange", allowsUnassignedValues = true)
    private List<TestdataValue> valueList;

    public TestdataListUnassignedEntityProvidingSortedSetEntity() {
        valueRange = new TreeSet<>(Comparator.comparing(TestdataValue::getCode));
        valueList = new ArrayList<>();
    }

    public TestdataListUnassignedEntityProvidingSortedSetEntity(String code, List<TestdataValue> valueRange) {
        super(code);
        this.valueRange = new TreeSet<>(Comparator.comparing(TestdataValue::getCode));
        this.valueRange.addAll(valueRange);
        this.valueList = new ArrayList<>();
    }

    public SortedSet<TestdataValue> getValueRange() {
        return valueRange;
    }

    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

}
