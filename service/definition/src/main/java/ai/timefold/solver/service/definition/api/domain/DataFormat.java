package ai.timefold.solver.service.definition.api.domain;

import java.util.Arrays;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DataFormat {

    Number(Values.NUMBER),
    Duration(Values.DURATION),
    Distance(Values.DISTANCE),
    Percentage(Values.PERCENTAGE);

    public static class Values {

        public static final String NUMBER = "number";
        public static final String DURATION = "duration";
        public static final String DISTANCE = "distance";
        public static final String PERCENTAGE = "percentage";
    }

    private final String label;

    DataFormat(String label) {
        this.label = label;
    }

    @JsonValue
    public String toString() {
        return this.label.toLowerCase();
    }

    public static DataFormat fromString(String format) {
        return Stream.of(DataFormat.values()).filter(value -> value.label.equalsIgnoreCase(format)).findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Output Metric data format is unknown, supported values are: " + Arrays.toString(DataFormat.values())));

    }
}
