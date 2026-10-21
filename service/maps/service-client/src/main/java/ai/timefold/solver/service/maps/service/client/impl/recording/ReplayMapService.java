package ai.timefold.solver.service.maps.service.client.impl.recording;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByTimeframeWithMetadata;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.Timeframe;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.TimeframeBucketing;
import ai.timefold.solver.service.maps.service.integration.internal.MapServiceOptions;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistance;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Answers map service calls from the files written by {@link RecordingMapService} during a single run, so that the run
 * can be reproduced without access to the maps service.
 * <p>
 * The directory holds the recording of one run, which is replayed with the same input: a call is answered by the
 * recording of the same kind and transport type, which must have been made for exactly the same locations, in the same
 * order. Other options (tenant, provider, location set, max distance from road, ...) are ignored, as they typically
 * differ between the recorded environment and a local run. A call that no recording answers fails, rather than silently
 * falling back to another source of travel times.
 * <p>
 * Waypoints are not recorded and are delegated to the given fallback.
 */
public final class ReplayMapService implements MapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplayMapService.class);

    static final String KIND_DISTANCES = "distances";
    static final String KIND_DISTANCES_BY_TIMEFRAME = "distances-by-timeframe";
    static final String KIND_LOCATIONS_OUT_OF_MAP = "locations-out-of-map";

    private final Path directory;
    private final TimeframeBucketing timeframeBucketing;
    private final MapService waypointsFallback;
    private final ObjectMapper mapper;

    private List<Recording> recordings;

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
        var recording = find(KIND_DISTANCES, locations, options);
        var keys = keys(locations);
        return new TravelTimeAndDistanceWithMetadata(new TravelTimeAndDistance(
                toDistanceMatrix(recording.content().travelTime(), keys),
                toDistanceMatrix(recording.content().distance(), keys)),
                recording.content().locationsNotInMap(), recording.content().resolvedMapLocation());
    }

    @Override
    public TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
            String options) {
        var recording = find(KIND_DISTANCES_BY_TIMEFRAME, locations, options);
        var resolver = timeframeResolver(recording);
        var keys = keys(locations);
        var timeframeCount = recording.content().travelTimes().size();
        var travelTimes = new DistanceMatrix[timeframeCount];
        var distances = new DistanceMatrix[timeframeCount];
        for (var timeframe = 0; timeframe < timeframeCount; timeframe++) {
            travelTimes[timeframe] = toDistanceMatrix(recording.content().travelTimes().get(timeframe), keys);
            distances[timeframe] = toDistanceMatrix(recording.content().distances().get(timeframe), keys);
        }
        var locationsNotInMap = recording.content().locationsNotInMap().stream().map(locations::get).toList();
        return new TravelTimesByTimeframeWithMetadata(travelTimes, distances, locationsNotInMap, resolver);
    }

    /**
     * A single matrix (traffic disabled) covers every instant. Otherwise, the matrices are in the order of the recorded
     * timeframes, which must be the local bucketing's, so that instants resolve to the same matrices as in the recorded
     * run.
     */
    private ToIntFunction<OffsetDateTime> timeframeResolver(Recording recording) {
        if (recording.content().travelTimes().size() == 1) {
            return time -> 0;
        }
        var localTimeframes = timeframeBucketing.allTimeframes().stream().map(Timeframe::name).toList();
        if (!localTimeframes.equals(recording.content().timeframes())) {
            throw new IllegalStateException(
                    "The recording (%s) was made with timeframes %s, but the local timeframe bucketing uses %s."
                            .formatted(recording.file(), recording.content().timeframes(), localTimeframes));
        }
        return timeframeBucketing::indexOf;
    }

    @Override
    public List<Location> getWaypoints(List<Location> locations, String options) {
        return waypointsFallback.getWaypoints(locations, options);
    }

    @Override
    public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
        return find(KIND_LOCATIONS_OUT_OF_MAP, locations, options).content().locationsOutOfMap();
    }

    private Recording find(String kind, List<Location> locations, String options) {
        var transportType = transportType(options);
        var requested = coordinates(locations);
        for (var recording : recordings()) {
            if (recording.kind().equals(kind) && Objects.equals(recording.transportType(), transportType)
                    && recording.content().locations().equals(requested)) {
                LOGGER.info("Replaying {} for {} locations from ({}).", kind, locations.size(), recording.file());
                return recording;
            }
        }
        throw new IllegalStateException(("No recording in the directory (%s) has %s for transport type (%s) "
                + "for the %d requested locations; replay the input of the recorded run. Available recordings: %s.")
                .formatted(directory, kind, transportType, locations.size(), recordings().stream()
                        .map(recording -> "%s (%s, transport type %s, %d locations)".formatted(
                                recording.file().getFileName(), recording.kind(), recording.transportType(),
                                recording.content().locations().size()))
                        .toList()));
    }

    private String transportType(String options) {
        return MapServiceOptions.parse(options).get(MapServiceOptions.TRANSPORT_TYPE);
    }

    private List<Recording> recordings() {
        if (recordings == null) {
            recordings = load();
        }
        return recordings;
    }

    private List<Recording> load() {
        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException("The distance matrix replay directory (%s) does not exist."
                    .formatted(directory));
        }
        try (Stream<Path> files = Files.list(directory)) {
            List<Recording> loaded = new ArrayList<>();
            for (var file : files.filter(path -> path.getFileName().toString().endsWith(".json")).sorted().toList()) {
                loaded.add(read(file));
            }
            if (loaded.isEmpty()) {
                throw new IllegalStateException("The distance matrix replay directory (%s) has no recordings."
                        .formatted(directory));
            }
            LOGGER.info("Loaded {} distance matrix recording(s) from ({}).", loaded.size(), directory);
            return loaded;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the distance matrix recordings in (%s).".formatted(directory),
                    e);
        }
    }

    private Recording read(Path file) throws IOException {
        var fileName = file.getFileName().toString();
        var kind = Stream.of(KIND_DISTANCES_BY_TIMEFRAME, KIND_LOCATIONS_OUT_OF_MAP, KIND_DISTANCES)
                .filter(candidate -> fileName.endsWith("-" + candidate + ".json"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown distance matrix recording (%s).".formatted(file)));
        var content = mapper.readValue(file.toFile(), RecordedCall.class);
        if (!kind.equals(KIND_LOCATIONS_OUT_OF_MAP) && !content.hasMatrices()) {
            throw new IllegalStateException(("Invalid distance matrix recording (%s): expected the same, non-zero number "
                    + "of travel time and distance matrices.").formatted(file));
        }
        return new Recording(file, kind, transportType(content.options()), content);
    }

    private List<Coordinates> coordinates(List<Location> locations) {
        return locations.stream().map(location -> new Coordinates(location.getLatitude(), location.getLongitude()))
                .toList();
    }

    private List<Location> keys(List<Location> locations) {
        return locations.stream().map(location -> new Location(location.getLatitude(), location.getLongitude()))
                .toList();
    }

    private DistanceMatrix toDistanceMatrix(List<long[]> rows, List<Location> keys) {
        var matrix = DistanceMatrix.getInstance(keys.size());
        for (var from = 0; from < keys.size(); from++) {
            var row = rows.get(from);
            for (var to = 0; to < keys.size(); to++) {
                matrix.put(keys.get(from), keys.get(to), row[to]);
            }
        }
        return matrix;
    }

    record Coordinates(double latitude, double longitude) {
    }

    /**
     * The content of a recorded file, see {@link RecordingMapService}; only the fields of its kind are set. Matrices are
     * lists of rows, {@code matrix[from][to]}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RecordedCall(String options, List<Coordinates> locations, List<Integer> locationsNotInMap,
            List<Integer> locationsOutOfMap, String resolvedMapLocation, List<String> timeframes,
            List<long[]> travelTime, List<long[]> distance, List<List<long[]>> travelTimes,
            List<List<long[]>> distances) {

        RecordedCall {
            locations = locations == null ? List.of() : locations;
            locationsNotInMap = locationsNotInMap == null ? List.of() : locationsNotInMap;
            locationsOutOfMap = locationsOutOfMap == null ? List.of() : locationsOutOfMap;
            travelTimes = travelTimes == null ? List.of() : travelTimes;
            distances = distances == null ? List.of() : distances;
        }

        boolean hasMatrices() {
            if (travelTime != null || distance != null) {
                return travelTime != null && distance != null;
            }
            return !travelTimes.isEmpty() && travelTimes.size() == distances.size();
        }
    }

    record Recording(Path file, String kind, String transportType, RecordedCall content) {
    }
}
