package ai.timefold.solver.service.maps.service.client.impl.recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Writes every travel-time and distance matrix returned by the delegate into a directory, one JSON file per call,
 * prefixed with a process-wide sequence number:
 * <ul>
 * <li>{@code <seq>-distances.json}: {@code options}, {@code locations}, {@code locationsNotInMap} (indexes into
 * {@code locations}), {@code resolvedMapLocation}, and the {@code travelTime} and {@code distance} matrices.</li>
 * <li>{@code <seq>-distances-by-timeframe.json}: {@code options}, {@code locations}, {@code locationsNotInMap}
 * (indexes into {@code locations}),
 * {@code timeframes} (the bucket names, in the order of the matrices) and the {@code travelTimes} and
 * {@code distances} matrices, one per timeframe.</li>
 * <li>{@code <seq>-locations-out-of-map.json}: {@code options}, {@code locations} and the {@code locationsOutOfMap}
 * indexes.</li>
 * </ul>
 * Matrices are written as rows of {@code locations}, i.e. {@code matrix[from][to]}, and streamed to the file so that
 * no copy is held in memory. Recording is best-effort: a failure to write is logged and never propagated to the
 * caller.
 */
public final class RecordingMapService implements MapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordingMapService.class);

    static final String DIRECTORY_NAME = "distance-matrix";

    /** Shared across instances, because the map service is produced once per injection point. */
    private static final AtomicLong SEQUENCE = new AtomicLong();

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
        write("distances", generator -> {
            generator.writeStringField("options", options);
            writeLocations(generator, "locations", locations);
            generator.writeObjectField("locationsNotInMap", result.locationsNotInMapIdx());
            generator.writeStringField("resolvedMapLocation", result.resolvedMapLocation());
            var keys = keys(locations);
            writeMatrix(generator, "travelTime", result.travelTimeAndDistance().travelTime(), keys);
            writeMatrix(generator, "distance", result.travelTimeAndDistance().distance(), keys);
        });
        return result;
    }

    @Override
    public TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
            String options) {
        var result = delegate.getTravelTimeAndDistanceByTimeframe(locations, options);
        write("distances-by-timeframe", generator -> {
            generator.writeStringField("options", options);
            writeLocations(generator, "locations", locations);
            generator.writeObjectField("locationsNotInMap", indexesOf(result.locationsNotInMap(), locations));
            generator.writeObjectField("timeframes", timeframeNames(result.travelTimesByTimeframe().length));
            var keys = keys(locations);
            writeMatrices(generator, "travelTimes", result.travelTimesByTimeframe(), keys);
            writeMatrices(generator, "distances", result.distancesByTimeframe(), keys);
        });
        return result;
    }

    @Override
    public List<Location> getWaypoints(List<Location> locations, String options) {
        return delegate.getWaypoints(locations, options);
    }

    @Override
    public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
        var result = delegate.getLocationsOutOfMap(locations, options);
        write("locations-out-of-map", generator -> {
            generator.writeStringField("options", options);
            writeLocations(generator, "locations", locations);
            generator.writeObjectField("locationsOutOfMap", result);
        });
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

    private void write(String kind, JsonWriter content) {
        Path file = directory.resolve(String.format("%05d-%s.json", SEQUENCE.incrementAndGet(), kind));
        try {
            Files.createDirectories(directory);
            // Write to a temporary file first, so a partially written file never ends up in the artifacts.
            Path tmp = Files.createTempFile(directory, file.getFileName().toString(), ".tmp");
            try {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(tmp.toFile(),
                        JsonEncoding.UTF8)) {
                    generator.writeStartObject();
                    content.write(generator);
                    generator.writeEndObject();
                }
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(tmp);
            }
            LOGGER.info("Recorded distance matrix ({}).", file);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not record distance matrix ({}).", file, e);
        }
    }

    private void writeLocations(JsonGenerator generator, String field, List<Location> locations)
            throws IOException {
        generator.writeArrayFieldStart(field);
        for (Location location : locations) {
            generator.writeStartObject();
            generator.writeNumberField("latitude", location.getLatitude());
            generator.writeNumberField("longitude", location.getLongitude());
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    private void writeMatrices(JsonGenerator generator, String field, DistanceMatrix[] matrices,
            List<Location> keys) throws IOException {
        generator.writeArrayFieldStart(field);
        for (DistanceMatrix matrix : matrices) {
            writeMatrixRows(generator, matrix, keys);
        }
        generator.writeEndArray();
    }

    private void writeMatrix(JsonGenerator generator, String field, DistanceMatrix matrix, List<Location> keys)
            throws IOException {
        generator.writeFieldName(field);
        writeMatrixRows(generator, matrix, keys);
    }

    private void writeMatrixRows(JsonGenerator generator, DistanceMatrix matrix, List<Location> keys)
            throws IOException {
        generator.writeStartArray();
        for (Location from : keys) {
            generator.writeStartArray();
            for (Location to : keys) {
                generator.writeNumber(matrix.get(from, to));
            }
            generator.writeEndArray();
        }
        generator.writeEndArray();
    }

    private List<Integer> indexesOf(List<Location> subset, List<Location> locations) {
        Set<Location> subsetSet = new HashSet<>(subset);
        return IntStream.range(0, locations.size())
                .filter(index -> subsetSet.contains(locations.get(index)))
                .boxed()
                .toList();
    }

    /**
     * Matrices are looked up by coordinates. Fresh copies keep the lookup from caching matrix indexes on the model's
     * own locations.
     */
    private List<Location> keys(List<Location> locations) {
        return locations.stream().map(location -> new Location(location.getLatitude(), location.getLongitude()))
                .toList();
    }

    @FunctionalInterface
    private interface JsonWriter {

        void write(JsonGenerator generator) throws IOException;
    }
}
