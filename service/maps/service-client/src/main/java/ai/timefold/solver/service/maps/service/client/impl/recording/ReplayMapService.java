package ai.timefold.solver.service.maps.service.client.impl.recording;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Answers map service calls from the files written by {@link RecordingMapService}, so that a run can be reproduced
 * without access to the maps service.
 * <p>
 * A request is answered by the first recording of the same kind and transport type that contains every requested
 * location; locations are matched by coordinates, so the request may use a subset of the recorded locations, in any
 * order. Other options (tenant, provider, location set, ...) are ignored, as they typically differ between the recorded
 * environment and a local run. A request that no recording can answer fails, rather than silently falling back to
 * another source of travel times.
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
        Path recordingDirectory = directory.resolve(RecordingMapService.DIRECTORY_NAME);
        this.directory = Files.isDirectory(recordingDirectory) ? recordingDirectory : directory;
        this.timeframeBucketing = timeframeBucketing;
        this.waypointsFallback = waypointsFallback;
        this.mapper = mapper;
    }

    @Override
    public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
        Recording recording = find(KIND_DISTANCES, locations, options);
        int[] indexes = recording.indexesOf(locations);
        List<Integer> locationsNotInMap = new ArrayList<>();
        for (int i = 0; i < indexes.length; i++) {
            if (recording.locationsNotInMap().contains(indexes[i])) {
                locationsNotInMap.add(i);
            }
        }
        return new TravelTimeAndDistanceWithMetadata(new TravelTimeAndDistance(
                recording.travelTimes()[0].toDistanceMatrix(locations, indexes),
                recording.distances()[0].toDistanceMatrix(locations, indexes)),
                locationsNotInMap, recording.resolvedMapLocation());
    }

    @Override
    public TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
            String options) {
        Recording recording = find(KIND_DISTANCES_BY_TIMEFRAME, locations, options);
        ToIntFunction<OffsetDateTime> resolver = timeframeResolver(recording);
        int[] indexes = recording.indexesOf(locations);
        int timeframeCount = recording.travelTimes().length;
        DistanceMatrix[] travelTimes = new DistanceMatrix[timeframeCount];
        DistanceMatrix[] distances = new DistanceMatrix[timeframeCount];
        for (int timeframe = 0; timeframe < timeframeCount; timeframe++) {
            travelTimes[timeframe] = recording.travelTimes()[timeframe].toDistanceMatrix(locations, indexes);
            distances[timeframe] = recording.distances()[timeframe].toDistanceMatrix(locations, indexes);
        }
        List<Location> locationsNotInMap = new ArrayList<>();
        for (int i = 0; i < indexes.length; i++) {
            if (recording.locationsNotInMap().contains(indexes[i])) {
                locationsNotInMap.add(locations.get(i));
            }
        }
        return new TravelTimesByTimeframeWithMetadata(travelTimes, distances, locationsNotInMap, resolver);
    }

    /**
     * A single matrix (traffic disabled) covers every instant. Otherwise, the matrices are in the order of the recorded
     * timeframes, which must be the local bucketing's, so that instants resolve to the same matrices as in the recorded
     * run.
     */
    private ToIntFunction<OffsetDateTime> timeframeResolver(Recording recording) {
        if (recording.travelTimes().length == 1) {
            return time -> 0;
        }
        List<String> localTimeframes = timeframeBucketing.allTimeframes().stream().map(Timeframe::name).toList();
        if (!localTimeframes.equals(recording.timeframes())) {
            throw new IllegalStateException(
                    "The recording (%s) was made with timeframes %s, but the local timeframe bucketing uses %s."
                            .formatted(recording.file(), recording.timeframes(), localTimeframes));
        }
        return timeframeBucketing::indexOf;
    }

    @Override
    public List<Location> getWaypoints(List<Location> locations, String options) {
        return waypointsFallback.getWaypoints(locations, options);
    }

    /**
     * Prefers recorded calls of this method; otherwise uses the locations the recorded matrices could not route.
     */
    @Override
    public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < locations.size(); i++) {
            if (isOutOfMap(locations.get(i))) {
                result.add(i);
            }
        }
        return result;
    }

    private boolean isOutOfMap(Location location) {
        Stream<Recording> outOfMapFirst = Stream.concat(
                recordings().stream().filter(recording -> recording.kind().equals(KIND_LOCATIONS_OUT_OF_MAP)),
                recordings().stream().filter(recording -> !recording.kind().equals(KIND_LOCATIONS_OUT_OF_MAP)));
        return outOfMapFirst
                .filter(recording -> recording.locationIndex().containsKey(key(location)))
                .findFirst()
                .map(recording -> recording.locationsNotInMap().contains(recording.locationIndex().get(key(location))))
                .orElseThrow(() -> new IllegalStateException(
                        "The location (%s) is not in any recording in the directory (%s).".formatted(location,
                                directory)));
    }

    private Recording find(String kind, List<Location> locations, String options) {
        String transportType = transportType(options);
        for (Recording recording : recordings()) {
            if (recording.kind().equals(kind) && Objects.equals(recording.transportType(), transportType)
                    && recording.containsAll(locations)) {
                LOGGER.info("Replaying {} for {} locations from ({}).", kind, locations.size(), recording.file());
                return recording;
            }
        }
        throw new IllegalStateException(("No recording in the directory (%s) has %s for transport type (%s) "
                + "containing all %d requested locations. Available recordings: %s.")
                .formatted(directory, kind, transportType, locations.size(), recordings().stream()
                        .map(recording -> "%s (%s, transport type %s, %d locations)".formatted(
                                recording.file().getFileName(), recording.kind(), recording.transportType(),
                                recording.locations().size()))
                        .toList()));
    }

    private static String transportType(String options) {
        return MapServiceOptions.parse(options).get(MapServiceOptions.TRANSPORT_TYPE);
    }

    private synchronized List<Recording> recordings() {
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
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).sorted().toList()) {
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

    /**
     * Streams the file, so that the matrices are read straight into arrays without an intermediate tree.
     */
    Recording read(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        String kind = Stream.of(KIND_DISTANCES_BY_TIMEFRAME, KIND_LOCATIONS_OUT_OF_MAP, KIND_DISTANCES)
                .filter(candidate -> fileName.endsWith("-" + candidate + ".json"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown distance matrix recording (%s).".formatted(file)));
        String options = null;
        String resolvedMapLocation = null;
        List<Location> locations = List.of();
        Set<Integer> locationsNotInMap = Set.of();
        List<String> timeframes = null;
        Matrix[] travelTimes = new Matrix[0];
        Matrix[] distances = new Matrix[0];
        try (JsonParser parser = mapper.getFactory().createParser(file.toFile())) {
            expect(parser.nextToken(), JsonToken.START_OBJECT, file);
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String field = parser.currentName();
                JsonToken value = parser.nextToken();
                switch (field) {
                    case "options" -> options = parser.getValueAsString();
                    case "resolvedMapLocation" -> resolvedMapLocation = parser.getValueAsString();
                    case "locations" -> locations = readLocations(parser);
                    case "locationsNotInMap", "locationsOutOfMap" -> locationsNotInMap = readIndexes(parser);
                    case "timeframes" -> timeframes = value == JsonToken.VALUE_NULL ? null
                            : Arrays.asList(mapper.readValue(parser, String[].class));
                    case "travelTime" -> travelTimes = new Matrix[] { readMatrix(parser) };
                    case "distance" -> distances = new Matrix[] { readMatrix(parser) };
                    case "travelTimes" -> travelTimes = readMatrices(parser);
                    case "distances" -> distances = readMatrices(parser);
                    default -> parser.skipChildren();
                }
            }
        }
        Map<LocationKey, Integer> locationIndex = new HashMap<>();
        for (int i = 0; i < locations.size(); i++) {
            locationIndex.putIfAbsent(key(locations.get(i)), i);
        }
        return new Recording(file, kind, transportType(options), resolvedMapLocation, locations, locationIndex,
                locationsNotInMap, timeframes, travelTimes, distances);
    }

    private static List<Location> readLocations(JsonParser parser) throws IOException {
        List<Location> locations = new ArrayList<>();
        while (parser.nextToken() == JsonToken.START_OBJECT) {
            double latitude = Double.NaN;
            double longitude = Double.NaN;
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String field = parser.currentName();
                parser.nextToken();
                switch (field) {
                    case "latitude" -> latitude = parser.getDoubleValue();
                    case "longitude" -> longitude = parser.getDoubleValue();
                    default -> parser.skipChildren();
                }
            }
            locations.add(new Location(latitude, longitude));
        }
        return locations;
    }

    private static Set<Integer> readIndexes(JsonParser parser) throws IOException {
        Set<Integer> indexes = new HashSet<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            indexes.add(parser.getIntValue());
        }
        return indexes;
    }

    private static Matrix[] readMatrices(JsonParser parser) throws IOException {
        List<Matrix> matrices = new ArrayList<>();
        while (parser.nextToken() == JsonToken.START_ARRAY) {
            matrices.add(readMatrix(parser));
        }
        return matrices.toArray(Matrix[]::new);
    }

    /** Expects the parser on the matrix's {@code START_ARRAY}. */
    private static Matrix readMatrix(JsonParser parser) throws IOException {
        List<long[]> rows = new ArrayList<>();
        while (parser.nextToken() == JsonToken.START_ARRAY) {
            // Rows are square, so the first row's length sizes the others.
            long[] row = new long[rows.isEmpty() ? 16 : Math.max(rows.get(0).length, 1)];
            int size = 0;
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (size == row.length) {
                    row = Arrays.copyOf(row, row.length * 2);
                }
                row[size++] = parser.getLongValue();
            }
            rows.add(size == row.length ? row : Arrays.copyOf(row, size));
        }
        return new Matrix(rows.toArray(long[][]::new));
    }

    private static void expect(JsonToken actual, JsonToken expected, Path file) {
        if (actual != expected) {
            throw new IllegalStateException("Invalid distance matrix recording (%s): expected %s but was %s."
                    .formatted(file, expected, actual));
        }
    }

    private static LocationKey key(Location location) {
        return new LocationKey(location.getLatitude(), location.getLongitude());
    }

    /** Coordinates only, so lookups never touch the matrix indexes cached on {@link Location}. */
    private record LocationKey(double latitude, double longitude) {
    }

    record Matrix(long[][] values) {

        DistanceMatrix toDistanceMatrix(List<Location> locations, int[] indexes) {
            DistanceMatrix matrix = DistanceMatrix.getInstance(locations.size());
            // Fill with fresh location instances, like the converters do, so the model's locations stay untouched.
            List<Location> keys = locations.stream()
                    .map(location -> new Location(location.getLatitude(), location.getLongitude()))
                    .toList();
            for (int from = 0; from < keys.size(); from++) {
                long[] row = values[indexes[from]];
                for (int to = 0; to < keys.size(); to++) {
                    matrix.put(keys.get(from), keys.get(to), row[indexes[to]]);
                }
            }
            return matrix;
        }
    }

    record Recording(Path file, String kind, String transportType, String resolvedMapLocation,
            List<Location> locations, Map<LocationKey, Integer> locationIndex, Set<Integer> locationsNotInMap,
            List<String> timeframes, Matrix[] travelTimes, Matrix[] distances) {

        boolean containsAll(List<Location> requested) {
            return requested.stream().allMatch(location -> locationIndex.containsKey(key(location)));
        }

        int[] indexesOf(List<Location> requested) {
            return requested.stream().mapToInt(location -> locationIndex.get(key(location))).toArray();
        }
    }
}
