package ai.timefold.solver.model.maps.service.integration.internal.model;

import java.util.List;

import ai.timefold.solver.model.maps.api.model.Location;

public record LocationSet(List<Location> locations, String options) {
}
