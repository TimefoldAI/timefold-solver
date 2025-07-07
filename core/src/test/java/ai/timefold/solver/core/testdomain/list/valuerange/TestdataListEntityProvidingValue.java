package ai.timefold.solver.core.testdomain.list.valuerange;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;

@PlanningEntity
public class TestdataListEntityProvidingValue extends TestdataObject {

    public static EntityDescriptor<TestdataListSolution> buildEntityDescriptor() {
        return TestdataListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListEntityProvidingValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataListEntityProvidingEntity entity;
    @IndexShadowVariable(sourceVariableName = "valueList")
    private Integer index;

    public TestdataListEntityProvidingValue() {
    }

    public TestdataListEntityProvidingValue(String code) {
        super(code);
    }

    public TestdataListEntityProvidingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataListEntityProvidingEntity entity) {
        this.entity = entity;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }
}
