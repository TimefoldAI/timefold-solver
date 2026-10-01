package ai.timefold.solver.service.maps.service.integration.api;

/**
 * Root interface for all {@link WaypointsExtractor} implementations.
 * Makes it easier to inject the implementations (that possibly use generics with wildcards) using DI frameworks.
 */
public sealed interface WaypointsExtractorBase permits WaypointsExtractor {
}
