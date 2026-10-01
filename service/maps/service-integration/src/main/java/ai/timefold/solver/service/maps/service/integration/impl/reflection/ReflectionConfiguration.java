package ai.timefold.solver.service.maps.service.integration.impl.reflection;

import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistance;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = { TravelTimeAndDistance.class })
public class ReflectionConfiguration {

}
