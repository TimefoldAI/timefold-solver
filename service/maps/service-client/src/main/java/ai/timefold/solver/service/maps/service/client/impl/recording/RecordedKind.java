package ai.timefold.solver.service.maps.service.client.impl.recording;

import ai.timefold.solver.service.maps.service.integration.internal.MapServiceOptions;

/**
 * The map service calls that are recorded. A run makes each of them at most once per transport type, so the kind and
 * the transport type name the file of the recording.
 */
enum RecordedKind {

    DISTANCES("distances"),
    DISTANCES_BY_TIMEFRAME("distances-by-timeframe"),
    LOCATIONS_OUT_OF_MAP("locations-out-of-map");

    /** Used in the file name when the options have no transport type, which is chosen by the map service then. */
    private static final String DEFAULT_TRANSPORT_TYPE = "default";

    private final String id;

    RecordedKind(String id) {
        this.id = id;
    }

    /**
     * @return for instance {@code distances-car.json}
     */
    String fileName(String options) {
        var transportType = MapServiceOptions.parse(options).get(MapServiceOptions.TRANSPORT_TYPE);
        return id + "-" + (transportType == null ? DEFAULT_TRANSPORT_TYPE : transportType) + ".json";
    }
}
