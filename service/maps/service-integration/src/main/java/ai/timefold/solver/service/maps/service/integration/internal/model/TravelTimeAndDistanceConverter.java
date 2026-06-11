package ai.timefold.solver.service.maps.service.integration.internal.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.service.maps.api.model.Location;

import com.fasterxml.jackson.databind.ObjectMapper;

// TODO: part of the contract for map providers; lives on the SDK part. Check if this is the right module.

public interface TravelTimeAndDistanceConverter {

    boolean canConvert(String provider);

    TravelTimeAndDistanceWithMetadata convert(List<Location> locations, InputStream data, List<Integer> locationsNotInMap);

    TravelTimeAndDistanceWithMetadata convert(List<Location> locations, InputStream data, List<Integer> inputStreamBytes,
            List<Integer> locationsNotInMap);

    TravelTimeAndDistanceWithMetadata update(TravelTimeAndDistance travelTimeAndDistance, List<Location> oldLocations,
            List<Location> newLocations, InputStream data, List<Integer> inputStreamBytes, List<Integer> oldLocationsNotInMap,
            List<Integer> newLocationsNotInMap);

    default <T> List<T> readChunksFromStream(ObjectMapper mapper, InputStream inputStream,
            List<Integer> inputStreamChunkBytes, Class<T> clazz) throws IOException {
        List<T> chunks = new ArrayList<>();
        for (int bytes : inputStreamChunkBytes) {
            byte[] chunkBytes = inputStream.readNBytes(bytes);
            T chunk = mapper.readValue(chunkBytes, clazz);
            chunks.add(chunk);
        }
        return chunks;
    }

    default List<Location> getLocationsInMap(List<Location> locations, List<Integer> locationsNotInMapIdx) {

        if (locationsNotInMapIdx == null || locationsNotInMapIdx.isEmpty()) {
            return locations;
        }

        return IntStream.range(0, locations.size())
                .filter(i -> !locationsNotInMapIdx.contains(i))
                .mapToObj(locations::get)
                .toList();
    }

}
