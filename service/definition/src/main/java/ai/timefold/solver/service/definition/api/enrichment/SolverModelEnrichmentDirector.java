package ai.timefold.solver.service.definition.api.enrichment;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.service.definition.api.SolverModel;

/**
 * Provides a model-specific way of enriching a solver model, typically for models using multiple enrichers depending on each
 * other.
 * <p>
 * While the implementation typically injects a list of available enrichers {@link SolverModelEnricherBase}, it may use none,
 * any or all of them. The implementation can (and should) avoid traversing the same parts of the model by different enrichers.
 */
public non-sealed interface SolverModelEnrichmentDirector<SolverModel_ extends SolverModel<? extends Score<?>>>
        extends SolverModelEnrichmentDirectorBase {

    /**
     * Returns true if this enrichment director is able to enrich the given model.
     * <p>
     * The default implementation checks the model is not null.
     *
     * @param solverModel the solver model to check for compatibility with this director
     * @return true, if the given model can be enriched by this director, false otherwise
     */
    default boolean accepts(SolverModel_ solverModel) {
        return solverModel != null;
    }

    /**
     * Enriches given solver model content based on additional requirements specific for that particular model, such as
     * filling in calculated distances for locations.
     *
     * @param solverModel the solver model to be enriched, never null
     * @return enriched solverModel - never null
     */
    SolverModel_ enrich(SolverModel_ solverModel);
}
