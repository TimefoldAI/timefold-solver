package ai.timefold.solver.core.preview.api.move.ruin;

public record ListRecreatedElement<Entity_, Value_, Metadata_ extends RecreateMetadata>(
        Value_ recreatedValue,
        Metadata_ possibleLocationMetadata) implements ListRuinedOrRecreatedElement<Entity_, Value_, Metadata_> {
}
