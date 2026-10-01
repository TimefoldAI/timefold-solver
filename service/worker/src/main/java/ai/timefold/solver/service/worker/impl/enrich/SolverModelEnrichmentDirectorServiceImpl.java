package ai.timefold.solver.service.worker.impl.enrich;

import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.enrichment.SolverModelEnrichmentDirector;
import ai.timefold.solver.service.definition.api.enrichment.SolverModelEnrichmentDirectorBase;
import ai.timefold.solver.service.definition.api.enrichment.SolverModelEnrichmentDirectorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.All;

@ApplicationScoped
public class SolverModelEnrichmentDirectorServiceImpl implements SolverModelEnrichmentDirectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolverModelEnrichmentDirectorServiceImpl.class);

    private List<SolverModelEnrichmentDirectorBase> enrichmentDirectors;

    @Inject
    public SolverModelEnrichmentDirectorServiceImpl(@All List<SolverModelEnrichmentDirectorBase> enrichmentDirectors) {
        this.enrichmentDirectors = enrichmentDirectors;
    }

    @Override
    public <T extends SolverModel<?>> boolean accepts(T solverModel) {
        return enrichmentDirectors.stream()
                .anyMatch(director -> ((SolverModelEnrichmentDirector<T>) director).accepts(solverModel));
    }

    @Override
    public <T extends SolverModel<?>> T enrich(T solverModel) {
        SolverModelEnrichmentDirector<T> acceptingDirector = findAcceptingDirector(solverModel);
        LOGGER.debug("Enriching solver model {} using enrichment director {}.", solverModel.getClass(),
                acceptingDirector.getClass());
        return acceptingDirector.enrich(solverModel);
    }

    private <T extends SolverModel<?>> SolverModelEnrichmentDirector<T> findAcceptingDirector(T solverModel) {
        var acceptingDirectors = enrichmentDirectors.stream()
                .filter(director -> ((SolverModelEnrichmentDirector<T>) director).accepts(solverModel))
                .map(solverModelEnrichmentDirectorBase -> (SolverModelEnrichmentDirector<T>) solverModelEnrichmentDirectorBase)
                .toList();

        if (acceptingDirectors.size() > 1) {
            throw new IllegalStateException(
                    "Multiple accepting model enrichment directors found for %s: %s".formatted(solverModel.getClass(),
                            Arrays.toString(acceptingDirectors.toArray())));
        } else if (acceptingDirectors.isEmpty()) {
            throw new IllegalStateException(
                    "No accepting model enrichment directors found for %s.".formatted(solverModel.getClass()));
        } else {
            var acceptingDirector = acceptingDirectors.getFirst();
            LOGGER.debug("Found enrichment director {} for solver model {}.", acceptingDirector.getClass(),
                    solverModel.getClass());
            return acceptingDirector;
        }
    }
}
