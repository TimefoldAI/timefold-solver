package ai.timefold.solver.model.maps.service.test.impl;

import static ai.timefold.solver.model.definition.internal.Headers.X_MAPS_LOCATIONS_CHUNK_BYTES;
import static ai.timefold.solver.model.definition.internal.Headers.X_MAPS_MATRIX_HASH_HEADER;
import static ai.timefold.solver.model.definition.internal.Headers.X_MAPS_PROVIDER_HEADER;
import static ai.timefold.solver.model.definition.internal.Headers.X_MAPS_RESPONSE_CHUNK_BYTES;

import java.io.ByteArrayInputStream;
import java.io.SequenceInputStream;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.haversine.impl.HaversineTravelTimeAndDistanceMatrixProvider;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

public class DistanceGetUpdateResponseTransformer implements ResponseDefinitionTransformerV2 {

    public static String TRANSFORMER_NAME = "distance-get-update-response-transformer";

    /**
     * Location-set name that opts a test into a real (non-410) update response. The test must seed the cache
     * with {@link #UPDATE_OLD_LOCATIONS} on a first enrich, then add {@link #UPDATE_NEW_LOCATIONS} on a second.
     * Used to drive the {@code processUpdateAndStoreInCache} path, which is otherwise unreachable via wiremock.
     */
    public static final String UPDATE_AWARE_LOCATION_SET_NAME = "with-updates";
    public static final List<Location> UPDATE_OLD_LOCATIONS = List.of(new Location(0, 0), new Location(1, 1));
    public static final List<Location> UPDATE_NEW_LOCATIONS = List.of(new Location(2, 2));

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HaversineTravelTimeAndDistanceMatrixProvider provider =
            new HaversineTravelTimeAndDistanceMatrixProvider(objectMapper);

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        String options = serveEvent.getRequest().queryParameter("options").firstValue();
        String matrixHash = serveEvent.getRequest().queryParameter("matrix-hash").firstValue();
        // "00" is the client's sentinel for "no cached hash yet"; it returns 410 so it falls back to POST.
        // For any other hash on the opt-in location set, deliver a real chunked update.
        if (!isUpdateAware(options) || "00".equals(matrixHash)) {
            return new ResponseDefinitionBuilder().withStatus(410).build();
        }
        try {
            return buildUpdateResponse();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isUpdateAware(String options) {
        if (options == null) {
            return false;
        }
        for (String entry : options.split(",")) {
            String[] kv = entry.split(":");
            if (kv.length == 2 && "locationSetName".equals(kv[0].trim())
                    && UPDATE_AWARE_LOCATION_SET_NAME.equals(kv[1].trim())) {
                return true;
            }
        }
        return false;
    }

    private ResponseDefinition buildUpdateResponse() throws Exception {
        // One matrix chunk (old × new) is enough to drive processUpdateAndStoreInCache. The test only
        // checks the resolvedMapLocation header fallback, not the joined matrix contents.
        TravelTimeAndDistance oldXNew = objectMapper.readValue(
                provider.calculateTravelTimeAndDistance(UPDATE_OLD_LOCATIONS, UPDATE_NEW_LOCATIONS,
                        Collections.emptyMap()).response(),
                TravelTimeAndDistance.class);

        byte[] newLocationsBytes = objectMapper.writeValueAsBytes(UPDATE_NEW_LOCATIONS);
        byte[] matrixBytes = objectMapper.writeValueAsBytes(oldXNew);

        return new ResponseDefinitionBuilder()
                .withHeader("Content-Type", "application/json")
                .withHeader(X_MAPS_PROVIDER_HEADER, provider.getProvider())
                // Intentionally NO X_MAPS_LOCATION_HEADER: drives the cache-fallback branch.
                .withHeader(X_MAPS_MATRIX_HASH_HEADER, "hash2")
                .withHeader(X_MAPS_RESPONSE_CHUNK_BYTES, String.valueOf(matrixBytes.length))
                .withHeader(X_MAPS_LOCATIONS_CHUNK_BYTES, newLocationsBytes.length + ",0")
                .withStatus(200)
                .withBody(new SequenceInputStream(new ByteArrayInputStream(newLocationsBytes),
                        new ByteArrayInputStream(matrixBytes)).readAllBytes())
                .build();
    }

    @Override
    public String getName() {
        return TRANSFORMER_NAME;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }
}
