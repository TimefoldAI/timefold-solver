package ai.timefold.solver.core.impl.testdata.domain.difficultyComparator;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity(difficultyWeightFactoryClass = TestdataDifficultyWeightFactory.class)
public class TestdataDifficultyWeightEntity extends TestdataObject {

    public static final String VALUE_FIELD = "value";
    private boolean comparisonCalled = false;

    public static EntityDescriptor<TestdataDifficultyWeightSolution> buildEntityDescriptor() {
        return TestdataDifficultyWeightSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataDifficultyWeightEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataDifficultyWeightSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataDifficultyWeightValue value;

    public TestdataDifficultyWeightEntity() {
    }

    public TestdataDifficultyWeightEntity(String code) {
        super(code);
    }

    public TestdataDifficultyWeightEntity(String code, TestdataDifficultyWeightValue value) {
        this(code);
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataDifficultyWeightValue getValue() {
        return value;
    }

    public void setValue(TestdataDifficultyWeightValue value) {
        this.value = value;
    }

    public boolean isComparisonCalled() {
        return comparisonCalled;
    }

    public void setComparisonCalled(boolean comparisonCalled) {
        this.comparisonCalled = comparisonCalled;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************
    public void updateValue() {
        this.value = new TestdataDifficultyWeightValue(value.getCode() + "/" + value.getCode());
    }
}
