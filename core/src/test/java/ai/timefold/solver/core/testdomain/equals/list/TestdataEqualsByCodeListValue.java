package ai.timefold.solver.core.testdomain.equals.list;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

@PlanningEntity
public class TestdataEqualsByCodeListValue extends TestdataEqualsByCodeListObject {

    public static EntityDescriptor<TestdataEqualsByCodeListSolution> buildEntityDescriptor() {
        return TestdataEqualsByCodeListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataEqualsByCodeListValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataEqualsByCodeListEntity entity;
    @IndexShadowVariable(sourceVariableName = "valueList")
    private Integer index;

    public TestdataEqualsByCodeListValue(String code) {
        super(code);
    }

    public TestdataEqualsByCodeListEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataEqualsByCodeListEntity entity) {
        this.entity = entity;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }
}
