package ai.timefold.solver.service.quarkus.deployment.defaults;

import ai.timefold.solver.service.definition.api.ModelConstraintJustification;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
public class EmptyModelConstraintJustification implements ModelConstraintJustification {

}
