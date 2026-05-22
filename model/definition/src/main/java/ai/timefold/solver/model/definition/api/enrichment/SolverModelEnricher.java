package ai.timefold.solver.model.definition.api.enrichment;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.model.definition.api.SolverModel;

public non-sealed interface SolverModelEnricher<SolverModel_ extends SolverModel<? extends Score<?>>>
        extends SolverModelEnricherBase {

    /**
     * Allows to enrich the solver model content based on additional requirements
     *
     * @param solverModel solver model to be enriched - never null
     * @return enriched solverModel - never null
     */
    SolverModel_ enrich(SolverModel_ solverModel);

    default boolean accept(Object solverModel) {
        return true;
    }

}
