package ai.timefold.solver.service.definition.api;

/**
 * Root interface for all {@link ModelConvertor} implementations.
 * Makes it easier to inject the implementations (that possibly use generics with wildcards) using DI frameworks.
 */
public sealed interface ModelConvertorBase permits ModelConvertor {
}
