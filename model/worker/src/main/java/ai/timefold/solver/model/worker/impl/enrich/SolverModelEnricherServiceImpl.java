package ai.timefold.solver.model.worker.impl.enrich;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.model.definition.api.SolverModel;
import ai.timefold.solver.model.definition.api.enrichment.SolverModelEnricher;
import ai.timefold.solver.model.definition.api.enrichment.SolverModelEnricherBase;
import ai.timefold.solver.model.definition.api.enrichment.SolverModelEnricherService;

import io.quarkus.arc.All;

@ApplicationScoped
public class SolverModelEnricherServiceImpl implements SolverModelEnricherService {

    private List<SolverModelEnricherBase> enrichers;

    @Inject
    public SolverModelEnricherServiceImpl(@All List<SolverModelEnricherBase> enrichers) {
        this.enrichers = enrichers;
    }

    public <T extends SolverModel<?>> T enrich(T solverModel) {
        List<SolverModelEnricher<T>> enrichers = this.enrichers.stream()
                .map(solverModelEnricherBase -> (SolverModelEnricher<T>) solverModelEnricherBase)
                .toList();
        for (SolverModelEnricher<T> enricher : enrichers) {
            if (enricher.accept(solverModel)) {
                solverModel = enricher.enrich(solverModel);
            }
        }
        return solverModel;
    }
}
