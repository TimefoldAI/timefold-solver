package ai.timefold.solver.model.quarkus.deployment.testdata.defaultconfigprofile;

import java.time.Duration;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

public class TestdataModelConfigOverrides implements ModelConfigOverrides {

    // Make sure the field is not omitted by the model descriptor even if it's null by default.
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Duration maximumTimeBurden;

    public Duration getMaximumTimeBurden() {
        return maximumTimeBurden;
    }

    public void setMaximumTimeBurden(Duration maximumTimeBurden) {
        this.maximumTimeBurden = maximumTimeBurden;
    }
}
