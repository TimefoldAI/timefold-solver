package ai.timefold.solver.service.maps.service.integration.internal.provider;

import java.io.InputStream;
import java.util.List;

public record TravelTimeAndDistanceMatrixResponse(InputStream response, List<Integer> locationsOutOfMapIndexes,
        String resolvedMapLocation) {

    public TravelTimeAndDistanceMatrixResponse(InputStream response, List<Integer> locationsOutOfMapIndexes) {
        this(response, locationsOutOfMapIndexes, null);
    }
}
