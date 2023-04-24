package ai.timefold.solver.jaxb.impl.testdata.domain;

import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlRootElement;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

@PlanningEntity
@XmlRootElement
public class JaxbTestdataEntity extends JaxbTestdataObject {

    public static EntityDescriptor buildEntityDescriptor() {
        SolutionDescriptor solutionDescriptor = JaxbTestdataSolution.buildSolutionDescriptor();
        return solutionDescriptor.findEntityDescriptorOrFail(JaxbTestdataEntity.class);
    }

    public static GenuineVariableDescriptor buildVariableDescriptorForValue() {
        SolutionDescriptor solutionDescriptor = JaxbTestdataSolution.buildSolutionDescriptor();
        EntityDescriptor entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(JaxbTestdataEntity.class);
        return entityDescriptor.getGenuineVariableDescriptor("value");
    }

    private JaxbTestdataValue value;

    public JaxbTestdataEntity() {
    }

    public JaxbTestdataEntity(String code) {
        super(code);
    }

    public JaxbTestdataEntity(String code, JaxbTestdataValue value) {
        this(code);
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    @XmlIDREF
    public JaxbTestdataValue getValue() {
        return value;
    }

    public void setValue(JaxbTestdataValue value) {
        this.value = value;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
