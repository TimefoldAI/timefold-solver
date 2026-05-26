package ai.timefold.solver.model.definition.api.data;

public record DemoDataConfigEntry(String key, String value) {

    public enum ConfigEntryKey {
        /***
         * Specifies the map data region used in the dataset, e.g. "osrm-us-georgia".
         */
        MAP_SERVICE_LOCATION,
        /***
         * Specifies the map service provider used in the dataset, e.g. "osrm".
         */
        MAP_SERVICE_PROVIDER
    }
}
