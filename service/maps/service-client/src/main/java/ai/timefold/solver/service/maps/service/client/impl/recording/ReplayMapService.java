package ai.timefold.solver.service.maps.service.client.impl.recording;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.ToIntFunction;

import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByTimeframeWithMetadata;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.Timeframe;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.TimeframeBucketing;
import ai.timefold.solver.service.maps.service.client.impl.recording.RecordingMapService.TimeframeRecording;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Answers map service calls with the results {@link RecordingMapService} wrote during a single run, so that the run can
 * be reproduced without access to the maps service. Each call reads the file named after its kind and transport type;
 * other options (tenant, provider, location set, ...) are ignored, as they typically differ between the recorded
 * environment and a local run. A call that was not recorded fails, rather than silently falling back to another source
 * of travel times.
 * <p>
 * Waypoints are not recorded and are delegated to the given fallback.
 */
public final class ReplayMapService implements MapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplayMapService.class);

    private final Path directory;
    private final TimeframeBucketing timeframeBucketing;
    private final MapService waypointsFallback;
    private final ObjectMapper mapper;

    public ReplayMapService(Path directory, TimeframeBucketing timeframeBucketing, MapService waypointsFallback,
            ObjectMapper mapper) {
        // Accept the root of an unzipped execution artifact as well as the recording directory itself.
        var recordingDirectory = directory.resolve(RecordingMapService.DIRECTORY_NAME);
        this.directory = Files.isDirectory(recordingDirectory) ? recordingDirectory : directory;
        this.timeframeBucketing = timeframeBucketing;
        this.waypointsFallback = waypointsFallback;
        this.mapper = mapper;
    }

    @Override
    public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
        return read(RecordedKind.DISTANCES, options, new TypeReference<>() {
        });
    }

    @Override
    public TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
            String options) {
        TimeframeRecording recorded = read(RecordedKind.DISTANCES_BY_TIMEFRAME, options, new TypeReference<>() {
        });
        return new TravelTimesByTimeframeWithMetadata(recorded.travelTimes().toArray(DistanceMatrix[]::new),
                recorded.distances().toArray(DistanceMatrix[]::new), recorded.locationsNotInMap(),
                timeframeResolver(recorded));
    }

    /**
     * A single matrix (traffic disabled) covers every instant. Otherwise, the matrices are in the order of the recorded
     * timeframes, which must be the local bucketing's, so that instants resolve to the same matrices as in the recorded
     * run.
     */
    private ToIntFunction<OffsetDateTime> timeframeResolver(TimeframeRecording recorded) {
        if (recorded.travelTimes().size() == 1) {
            return time -> 0;
        }
        var localTimeframes = timeframeBucketing.allTimeframes().stream().map(Timeframe::name).toList();
        if (!localTimeframes.equals(recorded.timeframes())) {
            throw new IllegalStateException(
                    "The recording was made with timeframes %s, but the local timeframe bucketing uses %s."
                            .formatted(recorded.timeframes(), localTimeframes));
        }
        return timeframeBucketing::indexOf;
    }

    @Override
    public List<Location> getWaypoints(List<Location> locations, String options) {
        return waypointsFallback.getWaypoints(locations, options);
    }

    @Override
    public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
        return read(RecordedKind.LOCATIONS_OUT_OF_MAP, options, new TypeReference<>() {
        });
    }

    private <T> T read(RecordedKind kind, String options, TypeReference<T> type) {
        var file = directory.resolve(kind.fileName(options));
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("No recording (%s) in the directory (%s) for the options (%s)."
                    .formatted(file.getFileName(), directory, options));
        }
        LOGGER.info("Replaying ({}).", file);
        try {
            return mapper.readValue(file.toFile(), type);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the recording (%s).".formatted(file), e);
        }
    }
}
