package ai.timefold.solver.quarkus.benchmark.it.domain;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public class TestdataListValueShadowEntity {

    private String value;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    private TestdataStringLengthShadowEntity entity;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    private TestdataListValueShadowEntity previousValue;

    @ShadowVariable(supplierName = "updateLength")
    private int length;

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

    public TestdataListValueShadowEntity getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(TestdataListValueShadowEntity previousValue) {
        this.previousValue = previousValue;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @ShadowSources("previousValue.length")
    public int updateLength() {
        var out = getLength(value);
        if (previousValue != null) {
            out += previousValue.getLength();
        }
        return out;
    }

    private static int getLength(String value) {
        return Objects.requireNonNullElse(value, "").length();
    }

}
