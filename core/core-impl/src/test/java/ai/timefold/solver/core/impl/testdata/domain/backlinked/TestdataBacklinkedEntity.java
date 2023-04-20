package ai.timefold.solver.core.impl.testdata.domain.backlinked;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public class TestdataBacklinkedEntity extends TestdataObject {

    public static EntityDescriptor<TestdataBacklinkedSolution> buildEntityDescriptor() {
        return TestdataBacklinkedSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataBacklinkedEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataBacklinkedSolution> buildVariableDescriptorForValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("value");
    }

    private TestdataBacklinkedSolution solution;
    private TestdataValue value;

    public TestdataBacklinkedEntity() {
    }

    public TestdataBacklinkedEntity(TestdataBacklinkedSolution solution, String code) {
        this(solution, code, null);
    }

    public TestdataBacklinkedEntity(TestdataBacklinkedSolution solution, String code, TestdataValue value) {
        super(code);
        this.solution = solution;
        this.value = value;
    }

    public TestdataBacklinkedSolution getSolution() {
        return solution;
    }

    public void setSolution(TestdataBacklinkedSolution solution) {
        this.solution = solution;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
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
