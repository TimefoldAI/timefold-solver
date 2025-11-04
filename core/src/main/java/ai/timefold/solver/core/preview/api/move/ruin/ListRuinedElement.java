package ai.timefold.solver.core.preview.api.move.ruin;

public record ListRuinedElement<Entity_, Value_, Metadata_ extends RecreateMetadata>(
        Value_ ruinedValue) implements ListRuinedOrRecreatedElement<Entity_, Value_, Metadata_> {
}
