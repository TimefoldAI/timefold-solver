package ai.timefold.solver.service.maps.service.integration.internal.model;

import java.util.List;

import ai.timefold.solver.service.maps.api.model.Location;

public record LocationSet(List<Location> locations, String options) {
}
