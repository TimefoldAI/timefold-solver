package ai.timefold.solver.service.testmodel;

import ai.timefold.solver.service.definition.api.domain.DataFormat;
import ai.timefold.solver.service.definition.api.metrics.ModelOutputMetrics;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFormat;

public record EmployeeScheduleOutputMetrics(
        @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT) @Schema(name = OUTPUT_METRIC_SHIFTS, title = "Assigned Shifts",
                format = DataFormat.Values.NUMBER, description = "The number of shifts assigned to employees.",
                type = SchemaType.NUMBER, example = "100", readOnly = true, extensions = {
                        @Extension(name = "x-tf-priority", value = "1"),
                        @Extension(name = "x-tf-example", value = "100") }) int assignedShifts)
        implements
            ModelOutputMetrics {

    public static final String OUTPUT_METRIC_SHIFTS = "assignedShifts";
}
