package ai.timefold.solver.service.definition.api.enrichment;

import ai.timefold.solver.service.definition.api.SolverModel;

public interface SolverModelEnricherService {

    /**
     * Applies available {@link SolverModelEnricher} implementations to a {@link SolverModel} instance.
     *
     * @param solverModel Input solver model.
     * @return Enriched solver model.
     */
    <T extends SolverModel<?>> T enrich(T solverModel);
}
