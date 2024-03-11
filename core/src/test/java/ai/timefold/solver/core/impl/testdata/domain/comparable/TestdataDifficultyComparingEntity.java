package ai.timefold.solver.core.impl.testdata.domain.comparable;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity(difficultyComparatorClass = TestdataCodeComparator.class)
public class TestdataDifficultyComparingEntity extends TestdataObject {

    public static EntityDescriptor<TestdataDifficultyComparingSolution> buildEntityDescriptor() {
        return TestdataDifficultyComparingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataDifficultyComparingEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataDifficultyComparingSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataValue value;

    public TestdataDifficultyComparingEntity() {
    }

    public TestdataDifficultyComparingEntity(String code) {
        super(code);
    }

    public TestdataDifficultyComparingEntity(String code, TestdataValue value) {
        this(code);
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = { "valueRange" }, strengthComparatorClass = TestdataCodeComparator.class)
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************
}
