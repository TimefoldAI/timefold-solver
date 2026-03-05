package ai.timefold.solver.core.api.domain.specification;

/**
 * Builder for specifying the source variable of a shadow variable.
 */
public interface SourceRefBuilder {

    SourceRefBuilder sourceVariable(String variableName);
}
