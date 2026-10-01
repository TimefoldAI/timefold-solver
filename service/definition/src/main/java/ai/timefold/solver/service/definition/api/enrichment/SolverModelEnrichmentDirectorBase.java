package ai.timefold.solver.service.definition.api.enrichment;

/**
 * Root interface for all {@link SolverModelEnrichmentDirector} implementations.
 * Makes it easier to inject the implementations (that possibly use generics with wildcards) using DI frameworks.
 */
public sealed interface SolverModelEnrichmentDirectorBase permits SolverModelEnrichmentDirector {
}
