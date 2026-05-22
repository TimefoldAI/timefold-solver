package ai.timefold.solver.model.maps.service.integration.internal.provider;

import java.io.InputStream;
import java.util.List;

public record TravelTimeAndDistanceMatrixResponse(InputStream response, List<Integer> locationsOutOfMapIndexes) {
}
