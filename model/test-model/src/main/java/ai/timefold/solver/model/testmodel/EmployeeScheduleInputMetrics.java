package ai.timefold.solver.model.testmodel;

import ai.timefold.solver.model.definition.api.domain.DataFormat;
import ai.timefold.solver.model.definition.api.metrics.ModelInputMetrics;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFormat;

public record EmployeeScheduleInputMetrics(
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT) @Schema(name = INPUT_METRIC_SHIFTS, title = "Shifts",
                format = DataFormat.Values.NUMBER, description = "The number of shifts submitted in the input dataset.",
                type = SchemaType.NUMBER, example = "100", readOnly = true, extensions = {
                        @Extension(name = "x-tf-priority", value = "1"),
                        @Extension(name = "x-tf-example", value = "100") }) int shiftCount,
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT) @Schema(name = INPUT_METRIC_EMPLOYEES, title = "Emplyees",
                format = DataFormat.Values.NUMBER, description = "The number of employees submitted in the input dataset.",
                type = SchemaType.NUMBER, example = "10", readOnly = true, extensions = {
                        @Extension(name = "x-tf-priority", value = "2"),
                        @Extension(name = "x-tf-example", value = "10") }) int employeeCount)
        implements
            ModelInputMetrics {

    public static final String INPUT_METRIC_EMPLOYEES = "employeeCount";
    public static final String INPUT_METRIC_SHIFTS = "shiftCount";
}
