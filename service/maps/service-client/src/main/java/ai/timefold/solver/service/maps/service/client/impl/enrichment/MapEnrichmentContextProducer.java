package ai.timefold.solver.service.maps.service.client.impl.enrichment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import ai.timefold.solver.service.definition.impl.enrichment.MapEnrichmentContext;

@ApplicationScoped
public class MapEnrichmentContextProducer {

    @Produces
    @ApplicationScoped
    public MapEnrichmentContext produceMapEnrichmentContext() {
        return new MapEnrichmentContext();
    }
}
