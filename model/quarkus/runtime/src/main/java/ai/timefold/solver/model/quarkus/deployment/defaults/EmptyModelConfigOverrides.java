package ai.timefold.solver.model.quarkus.deployment.defaults;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
public class EmptyModelConfigOverrides implements ModelConfigOverrides {

}
