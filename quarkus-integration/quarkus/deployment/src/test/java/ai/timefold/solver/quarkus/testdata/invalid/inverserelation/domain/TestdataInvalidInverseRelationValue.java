package ai.timefold.solver.quarkus.testdata.invalid.inverserelation.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;

public class TestdataInvalidInverseRelationValue {
    @InverseRelationShadowVariable(
            sourceVariableName = "value")
    private List<TestdataInvalidInverseRelationEntity> entityList;

    public List<TestdataInvalidInverseRelationEntity> getEntityList() {
        return entityList;
    }
}
