package ai.timefold.solver.service.worker.impl.testdata;

import java.util.Objects;

import ai.timefold.solver.service.definition.api.ModelInput;

public class TestdataModelInput implements ModelInput {

    private String value;

    public TestdataModelInput() {
    }

    public TestdataModelInput(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TestdataModelInput other && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
