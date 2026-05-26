package ai.timefold.solver.model.maps.service.integration.internal.model;

import java.util.List;
import java.util.UUID;

import ai.timefold.solver.model.maps.api.model.Location;

public record TenantLocationSet(UUID tenantId, String name, List<Location> locations, String provider, String region,
        String transportType) {
}
