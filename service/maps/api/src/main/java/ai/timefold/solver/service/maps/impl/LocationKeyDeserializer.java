package ai.timefold.solver.service.maps.impl;

import ai.timefold.solver.service.maps.api.model.Location;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

class LocationKeyDeserializer extends KeyDeserializer {
    @Override
    public Location deserializeKey(String key, DeserializationContext ctxt) {
        try {
            String[] coordinates = key.split(",");
            String latitude = coordinates[0].split("=")[1];
            String longitude = coordinates[1].split("=")[1];
            return new Location(Double.parseDouble(latitude),
                    Double.parseDouble(longitude.substring(0, longitude.length() - 1)));
        } catch (Exception e) {
            throw new RuntimeException("Could not deserialize Location key", e);
        }

    }
}
