package ai.timefold.solver.service.maps.service.client.impl.recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import ai.timefold.solver.service.definition.internal.platform.EnvironmentVars;
import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByTimeframeWithMetadata;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.Timeframe;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.TimeframeBucketing;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Writes the result of every map service call the solver depends on into a directory, as JSON, one file per call named
 * after the kind of call and its transport type (see {@link RecordedKind}), for {@link ReplayMapService} to read back.
 * <p>
 * Recording is best-effort: a failure to write is logged and never propagated to the caller.
 */
public final class RecordingMapService implements MapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordingMapService.class);

    static final String DIRECTORY_NAME = "distance-matrix";

    private final MapService delegate;
    private final TimeframeBucketing timeframeBucketing;
    private final Path directory;
    private final ObjectMapper mapper;

    RecordingMapService(MapService delegate, TimeframeBucketing timeframeBucketing, Path directory,
            ObjectMapper mapper) {
        this.delegate = delegate;
        this.timeframeBucketing = timeframeBucketing;
        this.directory = directory;
        this.mapper = mapper;
    }

    public RecordingMapService(MapService delegate, TimeframeBucketing timeframeBucketing, ObjectMapper mapper) {
        var dir = System.getenv(EnvironmentVars.ENV_TIMEFOLD_EXECUTION_PROFILE_DIR);
        var root = Path.of(dir != null && !dir.isBlank() ? dir : EnvironmentVars.DEFAULT_EXECUTION_PROFILE_DIR);
        this.delegate = delegate;
        this.timeframeBucketing = timeframeBucketing;
        this.directory = root.resolve(DIRECTORY_NAME);
        this.mapper = mapper;
    }

    @Override
    public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
        var result = delegate.getTravelTimeAndDistance(locations, options);
        write(RecordedKind.DISTANCES, options, result);
        return result;
    }

    @Override
    public TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
            String options) {
        var result = delegate.getTravelTimeAndDistanceByTimeframe(locations, options);
        write(RecordedKind.DISTANCES_BY_TIMEFRAME, options, new TimeframeRecording(
                List.of(result.travelTimesByTimeframe()), List.of(result.distancesByTimeframe()),
                result.locationsNotInMap(), timeframeNames(result.travelTimesByTimeframe().length)));
        return result;
    }

    @Override
    public List<Location> getWaypoints(List<Location> locations, String options) {
        return delegate.getWaypoints(locations, options);
    }

    @Override
    public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
        var result = delegate.getLocationsOutOfMap(locations, options);
        write(RecordedKind.LOCATIONS_OUT_OF_MAP, options, result);
        return result;
    }

    /**
     * With traffic disabled, the result holds a single matrix that does not correspond to any bucket.
     */
    private List<String> timeframeNames(int matrixCount) {
        List<Timeframe> timeframes = timeframeBucketing.allTimeframes();
        return matrixCount > 1 && timeframes.size() == matrixCount ? timeframes.stream().map(Timeframe::name).toList()
                : null;
    }

    /**
     * A run makes each call once per transport type; a repeated call would overwrite the file with the same content.
     */
    private void write(RecordedKind kind, String options, Object result) {
        Path file = directory.resolve(kind.fileName(options));
        try {
            Files.createDirectories(directory);
            // Write to a temporary file first, so a partially written file never ends up in the artifacts.
            Path tmp = Files.createTempFile(directory, file.getFileName().toString(), ".tmp");
            try {
                mapper.writeValue(tmp.toFile(), result);
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(tmp);
            }
            LOGGER.info("Recorded distance matrix ({}).", file);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not record distance matrix ({}).", file, e);
        }
    }

    /**
     * {@link TravelTimesByTimeframeWithMetadata} without its timeframe index resolver, which cannot be written; the
     * timeframe names let the replay check that its own bucketing resolves instants to the same matrices.
     */
    record TimeframeRecording(List<DistanceMatrix> travelTimes, List<DistanceMatrix> distances,
            List<Location> locationsNotInMap, List<String> timeframes) {
    }
}
