package ai.timefold.solver.model.definition.api.enrichment;

/**
 * Root interface for all {@link SolverModelEnricher} implementations.
 * Makes it easier to inject the implementations (that possibly use generics with wildcards) using DI frameworks.
 */
public sealed interface SolverModelEnricherBase permits SolverModelEnricher {
}
