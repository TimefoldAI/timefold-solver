package ai.timefold.solver.quarkus.benchmark.it.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public class TestdataListValueShadowEntity {

    private String value;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    private TestdataStringLengthShadowEntity entity;

    @ShadowVariable(supplierName = "updateLength")
    private Integer length;

    public TestdataListValueShadowEntity() {
    }

    public TestdataListValueShadowEntity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public TestdataStringLengthShadowEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataStringLengthShadowEntity entity) {
        this.entity = entity;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    @ShadowSources("entity")
    public Integer updateLength() {
        var oldLength = length != null ? length : 0;
        var newLength = entity != null ?
                entity.getValues().stream()
                        .map(TestdataListValueShadowEntity::getValue)
                        .mapToInt(TestdataListValueShadowEntity::getLength)
                        .sum() :
                0;
        if (oldLength != newLength) {
            return newLength;
        } else {
            return length;
        }
    }

    private static int getLength(String value) {
        if (value != null) {
            return value.length();
        } else {
            return 0;
        }
    }

}
