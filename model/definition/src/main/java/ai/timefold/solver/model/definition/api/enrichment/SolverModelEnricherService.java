package ai.timefold.solver.model.definition.api.enrichment;

import ai.timefold.solver.model.definition.api.SolverModel;

public interface SolverModelEnricherService {

    /**
     * Applies available {@link SolverModelEnricher} implementations to a {@link SolverModel} instance.
     *
     * @param solverModel Input solver model.
     * @return Enriched solver model.
     */
    <T extends SolverModel<?>> T enrich(T solverModel);
}
