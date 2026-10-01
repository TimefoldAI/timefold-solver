package ai.timefold.solver.service.definition.api.validation;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.runtime.annotations.RecordableConstructor;

public record IssueCode(
        @JsonValue String value) {

    @RecordableConstructor
    public IssueCode(String value) {
        String nonNullValue = Objects.requireNonNull(value, "Issue code must not be null");
        this.value = nonNullValue.trim();
    }

    public static IssueCode of(String value) {
        return new IssueCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
