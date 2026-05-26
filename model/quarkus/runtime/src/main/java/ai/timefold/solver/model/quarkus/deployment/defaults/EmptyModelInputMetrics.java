package ai.timefold.solver.model.quarkus.deployment.defaults;

import ai.timefold.solver.model.definition.api.metrics.ModelInputMetrics;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
public class EmptyModelInputMetrics implements ModelInputMetrics {

}
