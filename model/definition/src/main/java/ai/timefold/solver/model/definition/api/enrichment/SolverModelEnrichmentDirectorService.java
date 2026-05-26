package ai.timefold.solver.model.definition.api.enrichment;

import ai.timefold.solver.model.definition.api.SolverModel;

public interface SolverModelEnrichmentDirectorService {

    /**
     * Returns true if there is any solver model enrichment director accepting the given solver model.
     * <p>
     * Some models may choose not to provide a specific enrichment director.
     *
     * @param solverModel the solver model, never null
     * @return true, if there is a solver model director capable of enriching the given solver model, false otherwise
     * @param <T> the specific class of the solver model
     */
    <T extends SolverModel<?>> boolean accepts(T solverModel);

    /**
     * Applies available {@link SolverModelEnricher} implementations to a {@link SolverModel} instance.
     *
     * @param solverModel Input solver model.
     * @return Enriched solver model.
     */
    <T extends SolverModel<?>> T enrich(T solverModel);
}
