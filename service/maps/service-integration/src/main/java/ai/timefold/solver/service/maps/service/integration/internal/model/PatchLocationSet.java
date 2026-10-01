package ai.timefold.solver.service.maps.service.integration.internal.model;

import java.util.List;

import ai.timefold.solver.service.maps.api.model.Location;

public record PatchLocationSet(List<Location> previous, List<Location> update, String options) {
}
