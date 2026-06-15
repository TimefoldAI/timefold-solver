package ai.timefold.solver.service.definition.internal;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Side-channel used by the maps enricher to publish the resolved map-service location to the
 * SolverWorker, which then writes it onto the dataset's {@code Metadata} so it propagates through
 * insight events.
 */
@ApplicationScoped
public class MapEnrichmentContext {

    private String resolvedMapLocation;

    public void setResolvedMapLocation(String location) {
        this.resolvedMapLocation = location;
    }

    public String getResolvedMapLocation() {
        return this.resolvedMapLocation;
    }
}
