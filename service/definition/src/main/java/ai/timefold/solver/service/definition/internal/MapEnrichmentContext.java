package ai.timefold.solver.service.definition.internal;

import jakarta.enterprise.context.ApplicationScoped;

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
