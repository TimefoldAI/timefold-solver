package ai.timefold.solver.core.preview.api.move.ruin;

public record BasicRuinedElement<Entity_, Value_, Metadata_ extends RecreateMetadata>(
        Entity_ ruinedEntity) implements BasicRuinedOrRecreatedElement<Entity_, Value_, Metadata_> {
}
