package ai.timefold.solver.jackson.impl.testdata.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

@PlanningEntity
public class JacksonTestdataEntity extends JacksonTestdataObject {

    public static EntityDescriptor buildEntityDescriptor() {
        SolutionDescriptor solutionDescriptor = JacksonTestdataSolution.buildSolutionDescriptor();
        return solutionDescriptor.findEntityDescriptorOrFail(JacksonTestdataEntity.class);
    }

    public static GenuineVariableDescriptor buildVariableDescriptorForValue() {
        SolutionDescriptor solutionDescriptor = JacksonTestdataSolution.buildSolutionDescriptor();
        EntityDescriptor entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(JacksonTestdataEntity.class);
        return entityDescriptor.getGenuineVariableDescriptor("value");
    }

    private JacksonTestdataValue value;

    public JacksonTestdataEntity() {
    }

    public JacksonTestdataEntity(String code) {
        super(code);
    }

    public JacksonTestdataEntity(String code, JacksonTestdataValue value) {
        this(code);
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public JacksonTestdataValue getValue() {
        return value;
    }

    public void setValue(JacksonTestdataValue value) {
        this.value = value;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
