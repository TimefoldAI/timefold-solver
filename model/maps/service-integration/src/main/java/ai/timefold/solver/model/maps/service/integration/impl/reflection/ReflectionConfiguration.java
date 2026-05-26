package ai.timefold.solver.model.maps.service.integration.impl.reflection;

import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistance;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = { TravelTimeAndDistance.class })
public class ReflectionConfiguration {

}
