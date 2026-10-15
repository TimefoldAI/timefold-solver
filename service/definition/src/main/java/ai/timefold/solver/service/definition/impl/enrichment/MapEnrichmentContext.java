package ai.timefold.solver.service.definition.impl.enrichment;

/**
 * Stores the resolved map region during the enrichment phase to be sent over to the Platform later.
 */
public final class MapEnrichmentContext {

    private String resolvedMapLocation;

    public void setResolvedMapLocation(String location) {
        resolvedMapLocation = location;
    }

    public String getResolvedMapLocation() {
        return this.resolvedMapLocation;
    }
}
