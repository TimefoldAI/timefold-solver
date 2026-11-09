package ai.timefold.solver.service.definition.spi.enrichment;

import ai.timefold.solver.service.definition.spi.model.SolverModel;

public interface SolverModelEnricherService {

    /**
     * Applies available {@link SolverModelEnricher} implementations to a {@link SolverModel} instance.
     *
     * @param solverModel Input solver model.
     * @return Enriched solver model.
     */
    <T extends SolverModel<?>> T enrich(T solverModel);
}
