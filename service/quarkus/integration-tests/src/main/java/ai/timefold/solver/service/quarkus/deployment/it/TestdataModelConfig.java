package ai.timefold.solver.service.quarkus.deployment.it;

import ai.timefold.solver.service.definition.spi.dto.ModelConfigOverrides;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class TestdataModelConfig implements ModelConfigOverrides {

}
